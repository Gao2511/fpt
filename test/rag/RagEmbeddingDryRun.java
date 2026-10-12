package rag;

import ai.consultation.*;
import com.google.gson.*;
import dto.KnowledgeChunkDTO;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

/** OFFLINE ONLY: validates proposed corpus/payloads; never reads keys, opens JDBC, or sends HTTP. */
public final class RagEmbeddingDryRun {
    public static void main(String[] args) throws Exception {
        JsonObject plan=JsonParser.parseString(new String(Files.readAllBytes(Paths.get(args[0])),StandardCharsets.UTF_8)).getAsJsonObject();
        if(plan.get("approved").getAsBoolean())throw new IllegalArgumentException("Dry-run plan must remain unapproved");
        RagConfig cfg=new RagConfig(false,false,false,plan.get("model").getAsString(),plan.get("dimensions").getAsInt());
        GeminiEmbeddingService adapter=new GeminiEmbeddingService(cfg,"FAKE_OFFLINE_KEY",(method,url,key,body,deadline)->{throw new AssertionError("Dry-run must never send HTTP");});
        JsonArray requests=new JsonArray();int chunks=0,chars=0,bytes=0,estimatedLow=0,estimatedHigh=0;
        Set<String> ids=new HashSet<>();
        for(JsonElement item:plan.getAsJsonArray("documents")) {
            JsonObject d=item.getAsJsonObject();String id=d.get("id").getAsString(),title=d.get("title").getAsString(),text=d.get("content").getAsString();
            if(!ids.add(id))throw new IllegalArgumentException("Duplicate document ID");
            List<String> parts=new DocumentChunkingService().chunk(text);if(parts.size()!=1)throw new IllegalArgumentException("Approved plan requires exactly one chunk per document");
            // Synthetic approval proof checks the content filter only. It never approves or persists a document.
            KnowledgeChunkDTO.SourceMetadata proof=new KnowledgeChunkDTO.SourceMetadata("published",d.get("category").getAsString(),1,Instant.EPOCH,d.get("policyKey").getAsString(),d.get("sourceAuthority").getAsString(),false);
            KnowledgeChunkDTO candidate=new KnowledgeChunkDTO(1,1,1,VectorValues.hash(text),title,text,d.get("source").getAsString(),null,null,1,proof);
            if(KnowledgeContentPolicy.reason(candidate,Instant.now())!=KnowledgeContentPolicy.Reason.ACCEPTED)throw new IllegalArgumentException("Proposed document fails existing content policy: "+id);
            JsonObject row=new JsonObject();row.addProperty("id",id);row.addProperty("contentFilterIfApproved","ACCEPTED");row.add("body",adapter.requestBody(title,parts.get(0),"RETRIEVAL_DOCUMENT"));requests.add(row);chunks++;
        }
        ProductCatalog catalog=new ProductCatalog(Collections.emptyList(),Collections.emptyMap());
        for(JsonElement item:plan.getAsJsonArray("questions")) {
            JsonObject q=item.getAsJsonObject();String text=q.get("text").getAsString();CustomerRequirements memory=new CustomerRequirements();memory.update(text);
            RetrievalQuery rewrite=RetrievalQuery.plan(text,Collections.emptyList(),memory,catalog);
            for(JsonElement expected:q.getAsJsonArray("expectedRelevantDocumentIds"))if(!ids.contains(expected.getAsString()))throw new IllegalArgumentException("Unknown expected source");
            JsonObject row=new JsonObject();row.addProperty("id",q.get("id").getAsString());row.addProperty("originalQuestion",text);
            row.addProperty("isolatedPlannerRetrieve",rewrite.retrieve);row.addProperty("isolatedPlannerBusiness",rewrite.business);
            row.addProperty("routingNote","Planner diagnostic uses an empty catalog; live business routes/follow-up require authoritative catalog and session fixture. All six vectors are retrieval probes, not six chatbot calls.");
            row.add("body",adapter.requestBody(null,rewrite.query,"RETRIEVAL_QUERY"));requests.add(row);
        }
        for(JsonElement item:requests) {
            JsonObject body=item.getAsJsonObject().getAsJsonObject("body");
            if(body.get("outputDimensionality").getAsInt()!=768 || !body.get("outputDimensionality").getAsJsonPrimitive().isNumber() || body.has("embedContentConfig") || body.has("autoTruncate") || !body.get("model").getAsString().equals("models/"+cfg.model))throw new IllegalArgumentException("Unexpected embedding request format");
            String input=body.getAsJsonObject("content").getAsJsonArray("parts").get(0).getAsJsonObject().get("text").getAsString();
            if(body.has("title"))input=body.get("title").getAsString()+"\n"+input;
            if(input.length()>2000)throw new IllegalArgumentException("Unexpected input size");
            int n=input.codePointCount(0,input.length());chars+=n;bytes+=input.getBytes(StandardCharsets.UTF_8).length;
            // Explicit heuristic, NOT Gemini tokenizer output or a guaranteed cost bound.
            estimatedLow+=(n+3)/4+32;estimatedHigh+=n+32;
        }
        int planned=requests.size()+1;if(planned!=plan.get("plannedHttpRequests").getAsInt() || plan.get("maximumHttpRequestsIncludingRetries").getAsInt()<planned || plan.get("chatGenerationRequests").getAsInt()!=0)throw new IllegalArgumentException("Request budget mismatch");
        JsonObject result=new JsonObject();result.addProperty("planId",plan.get("planId").getAsString());result.addProperty("dryRunOnly",true);result.addProperty("realApiRequests",0);result.addProperty("actualTokens",(String)null);
        result.addProperty("documentChunks",chunks);result.addProperty("embeddingPostRequests",requests.size());result.addProperty("metadataGetRequests",1);result.addProperty("plannedHttpRequests",planned);result.addProperty("maximumHttpRequestsIncludingRetries",plan.get("maximumHttpRequestsIncludingRetries").getAsInt());
        result.addProperty("inputUnicodeCodePointsIncludingTitles",chars);result.addProperty("inputUtf8BytesIncludingTitles",bytes);result.addProperty("estimatedInputTokensLow",estimatedLow);result.addProperty("estimatedInputTokensHigh",estimatedHigh);result.addProperty("estimateMethod","Per request: ceil(codePoints/4)+32 to codePoints+32; heuristic only; excludes retries.");result.add("requests",requests);
        Files.write(Paths.get(args[1]),new GsonBuilder().serializeNulls().setPrettyPrinting().disableHtmlEscaping().create().toJson(result).getBytes(StandardCharsets.UTF_8));
        System.out.println("Dry-run PASS: "+chunks+" chunks, "+requests.size()+" embedding POSTs + 1 metadata GET = "+planned+" planned requests; hard cap "+plan.get("maximumHttpRequestsIncludingRetries").getAsInt()+" proposed, not yet approved/enforced by a live runner.");
        System.out.println("Input: "+chars+" code points / "+bytes+" UTF-8 bytes; estimated tokens "+estimatedLow+".."+estimatedHigh+" (heuristic, no retries). Real API calls=0; no JDBC writes; no credentials read.");
    }
}
