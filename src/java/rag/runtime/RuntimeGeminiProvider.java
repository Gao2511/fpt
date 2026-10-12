package rag.runtime;
import ai.*;
import ai.dto.*;
import ai.exception.AIException;
import rag.*;
import java.util.*;
import java.nio.charset.StandardCharsets;

/** No unmetered list/ping/stream routes and no alternate host/key. ConsultationEngine validates output. */
public final class RuntimeGeminiProvider implements LLMProvider {
    private final GeminiRuntimeConfig cfg;private final ApiRequestLedger ledger;private final LLMProvider wire;private final String turnId;
    public RuntimeGeminiProvider(GeminiRuntimeConfig cfg,ApiRequestLedger ledger){this(cfg,ledger,new ai.provider.GeminiProvider());}
    public RuntimeGeminiProvider(GeminiRuntimeConfig cfg,ApiRequestLedger ledger,LLMProvider wire){this.cfg=cfg;this.ledger=ledger;this.wire=wire;turnId=UUID.randomUUID().toString();}
    public String getId(){return "gemini";}public String getDisplayName(){return "Gemini local authorized";}public String getDefaultBaseUrl(){return "https://generativelanguage.googleapis.com";}
    private AIException blocked(){return new AIException(AIException.ErrorType.INVALID_REQUEST,"Gemini local request is not authorized or exceeds its safety budget.","Gemini");}
    public List<ModelInfo> getPresetModels(){return Collections.emptyList();}
    public List<ModelInfo> listModels(String k,String b)throws AIException{throw blocked();}
    public ChatResponse testConnection(String k,String m,String b)throws AIException{throw blocked();}
    public void chatStream(List<ChatMessage> m,String k,String n,String b,ChatOptions o,StreamCallback c){c.onError(blocked());}
    public ChatResponse chat(List<ChatMessage> messages,String key,String model,String base,ChatOptions options)throws AIException{
        UUID ticket;int tokens;
        try{
            cfg.current();boolean primary="gemini-3.8-flash".equals(model),backup="gemini-3.5-flash-lite".equals(model)&&cfg.backupEnabled;
            if((!primary&&!backup)||!cfg.chatKey().equals(key)||(base!=null&&!base.isEmpty()&&!getDefaultBaseUrl().equals(base))||options==null||options.isExpired()||options.getMaxTokens()>cfg.maxChatTokens||options.getMaxTokens()<1||!(primary?"medium":"low").equals(options.getThinkingLevel())||options.getResponseSchema()==null)throw new RagException(RagException.Code.EXECUTION_BLOCKED);
            String last=messages.get(messages.size()-1).getContent(),marker="\nCâu hỏi của khách:\n";int offset=last.indexOf(marker);if(offset<0)throw new RagException(RagException.Code.EXECUTION_BLOCKED);cfg.question(last.substring(offset+marker.length()));
            tokens=options.getMaxTokens();for(ChatMessage m:messages)tokens+=2*(m.getContent()==null?0:m.getContent().getBytes(StandardCharsets.UTF_8).length)+32;
            tokens+=2*options.getResponseSchema().toString().getBytes(StandardCharsets.UTF_8).length;
            ticket=ledger.reserve("CHAT","chat:"+turnId+":"+model,model,tokens);
        }catch(Exception e){throw blocked();}
        long start=System.nanoTime();ChatResponse response;
        try{response=wire.chat(messages,cfg.chatKey(),model,getDefaultBaseUrl(),options);}
        catch(AIException e){
            boolean uncertain=e.getErrorType()==AIException.ErrorType.TIMEOUT||e.getErrorType()==AIException.ErrorType.NETWORK_ERROR;
            try{ledger.finish(ticket,uncertain?"UNCERTAIN":"FAILED",e.getStatusCode(),e.getErrorType().name(),RuntimeEmbeddingService.ms(start),null,null,null);}catch(RagException blocked){throw blocked();}
            throw new AIException(e.getErrorType(),e.getStatusCode(),"Gemini local request failed; see sanitized request journal.","Gemini");
        }
        try{
            if(response==null||response.getPromptTokens()<0||response.getCompletionTokens()<0||response.getThoughtsTokens()<0||response.getTotalTokens()<0||response.getTotalTokens()>tokens||(long)response.getCompletionTokens()+response.getThoughtsTokens()>options.getMaxTokens()){
                ledger.finish(ticket,"UNCERTAIN",200,"INVALID_USAGE",RuntimeEmbeddingService.ms(start),null,null,null);throw new RagException(RagException.Code.EXECUTION_BLOCKED);
            }
            boolean usage=response.getPromptTokens()!=0||response.getCompletionTokens()!=0||response.getThoughtsTokens()!=0||response.getTotalTokens()!=0;
            ledger.finish(ticket,"COMPLETED",200,null,RuntimeEmbeddingService.ms(start),usage?response.getPromptTokens():null,usage?response.getCompletionTokens()+response.getThoughtsTokens():null,usage?response.getTotalTokens():null);return response;
        }catch(RagException e){throw blocked();}
    }
}
