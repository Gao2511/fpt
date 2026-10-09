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
        String[] response={""},request={""};int[] status={200},delay={0};AtomicInteger calls=new AtomicInteger();
        HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/",exchange->{
            calls.incrementAndGet();
            java.io.ByteArrayOutputStream body=new java.io.ByteArrayOutputStream();byte[] buffer=new byte[4096];int n;
            while((n=exchange.getRequestBody().read(buffer))!=-1)body.write(buffer,0,n);
            request[0]=new String(body.toByteArray(),StandardCharsets.UTF_8);
            if(delay[0]>0)try{Thread.sleep(delay[0]);}catch(InterruptedException e){Thread.currentThread().interrupt();}
            byte[] output=response[0].getBytes(StandardCharsets.UTF_8);exchange.sendResponseHeaders(status[0],output.length);
            exchange.getResponseBody().write(output);exchange.close();
        });
        server.start();String base="http://127.0.0.1:"+server.getAddress().getPort();
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
        } finally {server.stop(0);}
        System.out.println("ProviderResponseTest: "+passed+" passed");
    }
}
