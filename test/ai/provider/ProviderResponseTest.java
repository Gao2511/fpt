package ai.provider;

import ai.dto.*;
import ai.consultation.ConsultationPrompt;
import ai.exception.AIException;
import com.google.gson.*;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Actual adapter HTTP parsing against a loopback fixture, never a paid/live provider. */
public class ProviderResponseTest {
    static int passed;
    static void check(boolean condition,String reason){if(!condition)throw new AssertionError(reason);}
    public static void main(String[]args)throws Exception{
        String[] response={""},request={""},authHeader={""},path={""};int[] status={200},delay={0};AtomicInteger calls=new AtomicInteger();
        HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/",exchange->{
            calls.incrementAndGet();
            authHeader[0]=exchange.getRequestHeaders().getFirst("x-goog-api-key");path[0]=exchange.getRequestURI().toString();
            if(status[0]==302)exchange.getResponseHeaders().add("Location","/redirect-must-not-be-followed");
            java.io.ByteArrayOutputStream body=new java.io.ByteArrayOutputStream();byte[] buffer=new byte[4096];int n;
            while((n=exchange.getRequestBody().read(buffer))!=-1)body.write(buffer,0,n);
            request[0]=new String(body.toByteArray(),StandardCharsets.UTF_8);
            if(delay[0]>0)try{Thread.sleep(delay[0]);}catch(InterruptedException e){Thread.currentThread().interrupt();}
            byte[] output=response[0].getBytes(StandardCharsets.UTF_8);exchange.sendResponseHeaders(status[0],output.length);
            exchange.getResponseBody().write(output);exchange.close();
        });
        server.start();String base="http://127.0.0.1:"+server.getAddress().getPort();
        System.setProperty("AI_LOCAL_BASE_URL",base); // Explicit server-owned allowlist for this fixture only.
        try {
            ChatOptions options=new ChatOptions(0.3,1200);options.setResponseSchema(ConsultationPrompt.schema());
            List<ChatMessage> messages=Arrays.asList(ChatMessage.system(ConsultationPrompt.SYSTEM),ChatMessage.user("Chào bạn!"));
            String envelope="{\"message\":\"Em chào anh/chị.\",\"intent\":\"consultation\",\"recommendedPackageId\":null,\"packageIds\":[],\"action\":\"none\"}";
            JsonObject root=new JsonObject(),candidate=new JsonObject(),content=new JsonObject();JsonArray parts=new JsonArray(),candidates=new JsonArray();
            JsonObject thought=new JsonObject();thought.addProperty("thought",true);thought.addProperty("text","State Tracking: No. Internal reasoning.");parts.add(thought);
            for(String part:Arrays.asList(envelope.substring(0,40),envelope.substring(40))){JsonObject p=new JsonObject();p.addProperty("text",part);parts.add(p);}
            content.add("parts",parts);candidate.add("content",content);candidate.addProperty("finishReason","STOP");candidates.add(candidate);root.add("candidates",candidates);response[0]=root.toString();
            ChatResponse gemini=new GeminiProvider().chat(messages,"fake-test-key","gemini-2.5-flash",base,options);
            check(envelope.equals(gemini.getContent()) && "STOP".equals(gemini.getFinishReason()),"Multipart/thought parsing failed");
            JsonObject sent=JsonParser.parseString(request[0]).getAsJsonObject();
            check(sent.getAsJsonObject("generationConfig").has("responseJsonSchema") && sent.has("system_instruction"),"Missing native Gemini schema/system separation");
            passed++;System.out.println("PASS Gemini HTTP multipart, thought exclusion and schema payload");

            for(String model:Arrays.asList("gemini-3.8-flash","gemini-3.5-flash-lite")){
                options.setThinkingLevel(model.equals("gemini-3.8-flash")?"medium":"low");
                new GeminiProvider().chat(messages,"fake-test-key",model,base,options);
                sent=JsonParser.parseString(request[0]).getAsJsonObject();JsonObject gen=sent.getAsJsonObject("generationConfig");
                check(gen.getAsJsonObject("thinkingConfig").get("thinkingLevel").getAsString().equals(options.getThinkingLevel())&&!gen.has("temperature")&&!gen.has("topP")&&gen.has("responseJsonSchema")&&gen.get("maxOutputTokens").getAsInt()==1200,"Phase4 REST configuration");
                check("fake-test-key".equals(authHeader[0])&&!path[0].contains("key=")&&path[0].endsWith(model+":generateContent"),"header auth / exact model endpoint");
            }
            passed++;System.out.println("PASS Phase4 exact Gemini models / thinking / header auth REST payload");
            int redirectBefore=calls.get();status[0]=302;boolean redirectRejected=false;
            try{new GeminiProvider().chat(messages,"fake-test-key","gemini-3.8-flash",base,options);}catch(AIException expected){redirectRejected=true;}
            check(redirectRejected&&calls.get()==redirectBefore+1,"Gemini redirected or retried");status[0]=200;
            passed++;System.out.println("PASS Gemini redirects rejected without an extra HTTP request");

            response[0]="{\"choices\":[{\"message\":{\"content\":"+new Gson().toJson(envelope)+"},\"finish_reason\":\"stop\"}]}";
            OpenAICompatibleProvider openai=new OpenAICompatibleProvider("openai","Test OpenAI",base,false);
            ChatResponse result=openai.chat(messages,"fake-test-key","gpt-4o-mini",base,options);
            check(envelope.equals(result.getContent()),"OpenAI response parse failed");
            sent=JsonParser.parseString(request[0]).getAsJsonObject();check("json_schema".equals(sent.getAsJsonObject("response_format").get("type").getAsString()),"Missing OpenAI native schema");
            passed++;System.out.println("PASS OpenAI HTTP content and strict schema payload");

            openai.chat(messages,"fake-test-key","gpt-4o-2024-05-13",base,options);
            sent=JsonParser.parseString(request[0]).getAsJsonObject();check(!sent.has("response_format"),"Unsupported snapshot forced to use native schema");
            response[0]="{\"content\":[{\"type\":\"thinking\",\"thinking\":\"INTERNAL_THOUGHT\"},{\"type\":\"text\",\"text\":"+new Gson().toJson(envelope)+"}],\"stop_reason\":\"end_turn\"}";
            ChatResponse claude=new AnthropicProvider().chat(messages,"fake-test-key","fixture-claude",base,options);
            check(envelope.equals(claude.getContent()) && "end_turn".equals(claude.getFinishReason()),"Claude thought/text separation failed");
            sent=JsonParser.parseString(request[0]).getAsJsonObject();check(sent.has("system") && !sent.has("response_format"),"Unsupported provider schema handling failed");
            passed++;System.out.println("PASS unsupported snapshots use envelope protocol; Claude excludes thinking blocks");

            response[0]="{\"choices\":[{\"message\":{\"content\":\"unfinished\"},\"finish_reason\":\"length\"}]}";
            check("length".equals(openai.chat(messages,"fake-test-key","gpt-4o-mini",base,options).getFinishReason()),"Truncation signal lost");
            response[0]="{}";result=openai.chat(messages,"fake-test-key","gpt-4o-mini",base,options);check(result.getContent().isEmpty() && result.getFinishReason()==null,"Missing fields assumed successful");
            passed++;System.out.println("PASS truncation and missing finish signals are preserved for validation");

            for(int code:Arrays.asList(429,503)){
                int before=calls.get();status[0]=code;response[0]="{\"error\":\"fixture failure\"}";boolean rejected=false;
                try{openai.chat(messages,"fake-test-key","gpt-4o-mini",base,options);}catch(AIException expected){rejected=expected.getErrorType()==(code==429?AIException.ErrorType.QUOTA_EXCEEDED:AIException.ErrorType.HIGH_DEMAND);}
                check(rejected && calls.get()==before+1,"Unbounded or incorrect retry classification");
            }
            passed++;System.out.println("PASS HTTP rate-limit/outage classification and consultation retry bound");
            int before=calls.get();options.setDeadlineMillis(System.currentTimeMillis()-1);boolean expired=false;
            try{openai.chat(messages,"fake-test-key","gpt-4o-mini",base,options);}catch(AIException e){expired=e.getErrorType()==AIException.ErrorType.TIMEOUT;}
            check(expired && calls.get()==before,"Expired request still contacted provider");
            passed++;System.out.println("PASS expired deadline prevents provider request");
            status[0]=200;response[0]="{}";delay[0]=500;options.setDeadlineMillis(System.currentTimeMillis()+100);
            long began=System.currentTimeMillis();boolean timedOut=false;
            try{openai.chat(messages,"fake-test-key","gpt-4o-mini",base,options);}catch(AIException e){timedOut=e.getErrorType()==AIException.ErrorType.TIMEOUT;}
            check(timedOut && System.currentTimeMillis()-began<1500,"Stalled response not bounded/classified as timeout");
            passed++;System.out.println("PASS stalled provider response times out within bounded deadline");
        } finally {System.clearProperty("AI_LOCAL_BASE_URL");server.stop(0);}
        noReplayOnEof(false);noReplayOnEof(true);
        System.out.println("ProviderResponseTest: "+passed+" passed");
    }
    /** Server drops the response after reading a POST: the JDK must not replay its buffered body. */
    private static void noReplayOnEof(boolean embedding)throws Exception{
        try(java.net.ServerSocket listener=new java.net.ServerSocket(0,4,java.net.InetAddress.getByName("127.0.0.1"))){
            listener.setSoTimeout(600);AtomicInteger posts=new AtomicInteger();java.util.concurrent.atomic.AtomicReference<Throwable> failure=new java.util.concurrent.atomic.AtomicReference<>();
            Thread dropper=new Thread(()->{try{while(posts.get()<3){try(java.net.Socket socket=listener.accept()){
                socket.setSoTimeout(2000);java.io.InputStream in=socket.getInputStream();java.io.ByteArrayOutputStream header=new java.io.ByteArrayOutputStream();int n;
                while((n=in.read())>=0){header.write(n);byte[] bytes=header.toByteArray();int len=bytes.length;if(len>=4&&bytes[len-4]==13&&bytes[len-3]==10&&bytes[len-2]==13&&bytes[len-1]==10)break;if(len>8192)throw new java.io.IOException("Fixture header bound");}
                String h=new String(header.toByteArray(),StandardCharsets.US_ASCII);java.util.regex.Matcher m=java.util.regex.Pattern.compile("(?im)^Content-Length: ([0-9]+)").matcher(h);if(!m.find())throw new AssertionError("Expected fixed-length POST");int size=Integer.parseInt(m.group(1));for(int i=0;i<size;i++)if(in.read()<0)throw new java.io.IOException("Fixture body truncated");posts.incrementAndGet();
            }}}catch(java.net.SocketTimeoutException finished){}catch(Throwable t){failure.set(t);}},"offline-eof-fixture");dropper.setDaemon(true);dropper.start();
            String base="http://127.0.0.1:"+listener.getLocalPort();boolean rejected=false;
            try{
                if(embedding)rag.GeminiEmbeddingService.httpTransport().request("POST",base+"/embedContent","fake-test-key",new JsonObject(),System.currentTimeMillis()+2000);
                else{ChatOptions o=new ChatOptions(.3,1200);o.setDeadlineMillis(System.currentTimeMillis()+2000);new GeminiProvider().chat(Arrays.asList(ChatMessage.user("Chào bạn")),"fake-test-key","gemini-3.8-flash",base,o);}
            }catch(java.io.IOException|AIException expected){rejected=true;}
            dropper.join(3000);check(rejected&&failure.get()==null&&!dropper.isAlive()&&posts.get()==1,"Implicit HTTP POST replay on EOF");
            passed++;System.out.println("PASS "+(embedding?"Embedding":"Chat")+" HTTP EOF cannot implicitly replay POST");
        }
    }
}
