package rag;

import com.google.gson.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/** Offline plan validation only. No credentials, HTTP, database or feature activation. */
public final class RagSingleEmbeddingPlan {
    private static void check(boolean value) { if(!value)throw new IllegalArgumentException("Single embedding plan validation failed"); }
    private static JsonObject read(Path path)throws Exception { return JsonParser.parseString(new String(Files.readAllBytes(path),StandardCharsets.UTF_8)).getAsJsonObject(); }
    public static void main(String[] args)throws Exception {
        Path root=Paths.get(args[0]);
        String planId=args.length>1?args[1]:"phase23-single-embedding-20261011-v1";
        boolean corrected="phase23-corrected-payload-20261011-v2".equals(planId);
        check(corrected || "phase23-single-embedding-20261011-v1".equals(planId));
        JsonObject plan=read(root.resolve("test/rag/fixtures/"+(corrected?"phase23-corrected-embedding-plan.json":"phase23-single-embedding-plan.json")));
        check(planId.equals(plan.get("planId").getAsString()));
        // Plan approval does not activate networking; this validator remains offline in either state.
        check(!plan.get("ragEnabled").getAsBoolean());
        check(plan.get("plannedHttpRequests").getAsInt()==1 && plan.get("maximumHttpRequests").getAsInt()==1 && plan.get("embeddingPostRequests").getAsInt()==1);
        for(String name:new String[]{"metadataGetRequests","retryRequests","chatGenerationRequests","countTokensRequests","databaseWrites"})check(plan.get(name).getAsInt()==0);
        check("RAG_EMBEDDING_API_KEY".equals(plan.get("keyEnvironmentVariable").getAsString()));
        check("POST".equals(plan.get("method").getAsString()));
        check("https://generativelanguage.googleapis.com/v1beta/models/gemini-embedding-001:embedContent".equals(plan.get("endpoint").getAsString()));
        JsonObject evidence=read(root.resolve(plan.get("metadataEvidence").getAsString()));
        check(evidence.get("httpStatus").getAsInt()==200 && evidence.get("modelAccessVerified").getAsBoolean() && evidence.get("expectedModelNameReturned").getAsBoolean());
        check(evidence.get("supportsEmbedContent").getAsBoolean() && "gemini-embedding-001".equals(evidence.get("requestedModel").getAsString()));
        check("RAG_EMBEDDING_API_KEY".equals(evidence.get("keySource").getAsString()));
        check(evidence.get("inputTokenLimit").getAsInt()==plan.get("inputTokenLimit").getAsInt());
        check(evidence.get("metadataGetAttempts").getAsInt()==1 && evidence.get("embeddingPostRequests").getAsInt()==0 && evidence.get("chatGenerationRequests").getAsInt()==0);
        RagConfig config=new RagConfig(false,false,false,plan.get("model").getAsString(),plan.get("dimensions").getAsInt());
        GeminiEmbeddingService adapter=new GeminiEmbeddingService(config,"FAKE_OFFLINE_KEY",(method,url,key,body,deadline)->{throw new AssertionError("Offline plan must never send HTTP");});
        String text=plan.get("input").getAsString(),task=plan.get("taskType").getAsString();
        check("RETRIEVAL_QUERY".equals(task) && !text.trim().isEmpty() && text.length()<=2000);
        int points=text.codePointCount(0,text.length()),bytes=text.getBytes(StandardCharsets.UTF_8).length;
        int low=(points+3)/4+32,high=points+32;
        check(low==plan.get("estimatedInputTokensLow").getAsInt() && high==plan.get("estimatedInputTokensHigh").getAsInt());
        JsonObject body=JsonParser.parseString(adapter.requestBody(null,text,task).toString()).getAsJsonObject();
        check(body.get("outputDimensionality").getAsInt()==768 && body.get("outputDimensionality").getAsJsonPrimitive().isNumber());
        check("RETRIEVAL_QUERY".equals(body.get("taskType").getAsString()) && !body.has("embedContentConfig") && !body.has("autoTruncate") && !body.has("title") && body.size()==4);
        // UTF-8 byte count is diagnostic, not a tokenizer-based input cap.
        JsonObject report=new JsonObject();
        report.addProperty("planId",plan.get("planId").getAsString());report.addProperty("offlineOnly",true);report.addProperty("realApiRequests",0);
        report.addProperty("metadataEvidenceValidated",true);report.addProperty("dimensionsConfigured",768);report.addProperty("verifiedInputTokenLimit",2048);
        report.addProperty("inputUnicodeCodePoints",points);report.addProperty("inputUtf8Bytes",bytes);
        report.addProperty("estimatedInputTokensLow",low);report.addProperty("estimatedInputTokensHigh",high);report.add("actualInputTokens",JsonNull.INSTANCE);
        report.add("requestBody",body);
        Path output=root.resolve(".build/"+(corrected?"phase23-corrected-embedding-preview.json":"phase23-single-embedding-preview.json"));Files.createDirectories(output.getParent());
        Files.write(output,new GsonBuilder().serializeNulls().setPrettyPrinting().disableHtmlEscaping().create().toJson(report).getBytes(StandardCharsets.UTF_8));
        System.out.println("Single embedding plan PASS: metadata evidence valid; payload from existing adapter; 768 dimensions configured; "+points+" code points / "+bytes+" bytes; estimated "+low+".."+high+" tokens; real API requests=0.");
    }
}
