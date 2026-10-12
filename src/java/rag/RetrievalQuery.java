package rag;

import ai.consultation.*;
import ai.dto.ChatMessage;
import java.util.*;

/** Bounded deterministic expansion; no age inference, model call, or history copied to embeddings. */
public final class RetrievalQuery {
    public final String query;
    public final boolean retrieve, clarify, business;
    private RetrievalQuery(String query,boolean retrieve,boolean clarify,boolean business){this.query=query;this.retrieve=retrieve;this.clarify=clarify;this.business=business;}
    private static String word(String text,String word,String replacement){return text.replaceAll("(?<![a-z0-9._/-])(?:"+word+")(?![a-z0-9._/-])",replacement);}
    public static String normalize(String text) {
        String q=word(ProductCatalog.normalize(text),"ko|hong|khum","khong");q=word(q,"tui","toi");
        q=word(q,"xai","su dung");q=word(q,"lag","ket noi cham");q=word(q,"yeu xiu","song yeu");q=word(q,"chien game","choi game");return word(q,"fix","xu ly");
    }
    static Set<String> topics(String text) {
        if(text==null)return Collections.emptySet();
        String q=normalize(text);Set<String> result=new LinkedHashSet<>();
        for(String domain:Arrays.asList("camera","mesh","wifi","game","truyen hinh"))if(q.matches("(?s).*\\b"+domain+"\\b.*"))result.add(domain);
        if(q.matches("(?s).*\\bphu song\\b.*"))result.add("wifi");
        if(q.matches("(?s).*\\b(mang|internet|ket noi|song yeu)\\b.*") && result.isEmpty())result.add("internet");
        if(q.matches("(?s).*\\b(hop dong|chuyen dia chi|chuyen nha|tam ngung|tam dung)\\b.*"))result.add("hop dong");
        return result;
    }
    public static Set<String> subjectTopics(String text){return Collections.unmodifiableSet(topics(text));}
    public static RetrievalQuery plan(String question,List<ChatMessage> history,CustomerRequirements memory,ProductCatalog catalog) {
        if(ConversationSignals.greeting(question)||ConversationSignals.social(question)||ConversationSignals.playful(question)||ConversationSignals.outside(question))return new RetrievalQuery(question,false,false,false);
        String q=normalize(question);Set<String> topic=topics(question);
        boolean support=q.matches("(?s).*\\b(ket noi cham|song yeu|huong dan|su co|mat mang|khong ket noi|bao mat|mat khau|thu tuc|chinh sach|bao tri|hop dong|chuyen dia chi|chuyen nha|tam ngung|tam dung|ping|do tre)\\b.*");
        boolean availability=q.matches("(?s).*\\bcombo\\b.*") ||
            q.matches("(?s).*(?:co camera\\s*(?:khong|ko|hong|khum)|internet kem truyen hinh).*" );
        // A request for structured prices/fees stays business-owned even when phrased as a policy.
        boolean structuredFacts=q.matches("(?s).*\\b(gia|cuoc|phi|uu dai|khuyen mai)\\b.*");
        boolean business=structuredFacts || (!support && (!catalog.mentioned(question).isEmpty() || memory.requirementsChanged || availability ||
            q.matches("(?s).*\\b(goi|ngan sach|ngon bo re|gia|cuoc|phi|uu dai|khuyen mai|toc do|them camera)\\b.*")));
        boolean follow=q.length()<=180 && q.matches("(?s).*(?:\\b(?:vay|the|them|nay|do|cai do|cach do)\\b|thi sao|sao shop).*" );
        Set<String> previous=new LinkedHashSet<>();int turns=0;
        // Extract only domain labels from two recent CUSTOMER turns, never names/addresses/assistant prose.
        for(int i=history.size()-1;i>=0 && turns<2;i--)if("user".equals(history.get(i).getRole())){
            turns++;String text=history.get(i).getContent();if(text!=null && text.length()<=2000)previous.addAll(topics(text));
        }
        if(topic.isEmpty() && follow && !business){
            if(previous.size()==1)topic.addAll(previous);
            else if(previous.size()>1 || (memory.selectedPackageId==null && !memory.televisionKnown && !memory.cameraKnown))return new RetrievalQuery(question,false,true,false);
        }
        business=business || (!support && follow && memory.selectedPackageId!=null);
        boolean service=!topic.isEmpty() || ConversationSignals.service(question,memory,catalog);
        StringBuilder expansion=new StringBuilder();
        for(String t:topic)if(!ProductCatalog.normalize(question).contains(t))expansion.append(' ').append(t);
        if(q.contains("ket noi cham")||q.contains("song yeu"))expansion.append(" su co ket noi cham song yeu");
        if(q.contains("choi game"))expansion.append(" choi game");
        if(follow && topic.contains("game") && q.contains("ping"))expansion.append(" ket noi day LAN");
        String rewritten=question.trim();if(rewritten.length()+expansion.length()+2<=2000)rewritten+="\n"+expansion.toString().trim();
        return new RetrievalQuery(rewritten.trim(),service||support,false,business);
    }
}
