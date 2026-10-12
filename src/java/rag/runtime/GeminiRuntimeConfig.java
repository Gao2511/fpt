package rag.runtime;
import com.google.gson.*;
import rag.*;
import java.util.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/** Operator-only short-lived authorization. No key lookup in database or production fallback. */
public final class GeminiRuntimeConfig {
    public enum Mode {OFFLINE_MOCK,LOCAL_GEMINI,STAGING,PRODUCTION}
    public final String planId,planHash,embeddingProject,chatProject;
    public final int totalLimit,documentLimit,queryLimit,chatLimit,rpm,rpd,tpm,dailyTokens,maxChatTokens;
    public final long expires;
    public final boolean backupEnabled;
    public final boolean onlineMode;
    public final boolean allowOnlineCatalog;
    private final String embeddingKey,chatKey;
    private final JsonObject plan;
    public static Mode mode(Map<String,String> env){
        String raw = env != null ? env.get("RAG_RUNTIME_MODE") : null;
        if (raw == null || raw.trim().isEmpty()) return Mode.OFFLINE_MOCK;
        try { return Mode.valueOf(raw.trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ex) { return Mode.OFFLINE_MOCK; }
    }
    public static boolean requested(Map<String,String> env){return mode(env)==Mode.LOCAL_GEMINI;}
    public static boolean onlineRequested(Map<String,String> env){Mode m=mode(env);return m==Mode.STAGING||m==Mode.PRODUCTION;}
    public static boolean activeRequested(Map<String,String> env){return requested(env)||onlineRequested(env);}
    public static GeminiRuntimeConfig load(Map<String,String> env)throws Exception{
        if(!requested(env))throw new IllegalArgumentException("LOCAL_GEMINI required; staging/production activation is disabled");
        String name=env.get("RAG_LIVE_PLAN_PATH");if(name==null)throw new IllegalArgumentException("RAG_LIVE_PLAN_PATH missing");
        Path path=Paths.get(name);if(!path.isAbsolute()||!Files.isRegularFile(path)||Files.size(path)>65536)throw new IllegalArgumentException("Invalid operator plan file");
        return new GeminiRuntimeConfig(env,JsonParser.parseString(new String(Files.readAllBytes(path),StandardCharsets.UTF_8)).getAsJsonObject());
    }
    public static GeminiRuntimeConfig loadOnline(Map<String,String> env)throws Exception{
        if(!onlineRequested(env))throw new IllegalArgumentException("STAGING or PRODUCTION runtime mode required");
        String inlineJson=env.get("RAG_ONLINE_PLAN_JSON");
        if(inlineJson!=null&&!inlineJson.trim().isEmpty()){
            if(inlineJson.length()>65536)throw new IllegalArgumentException("Online plan JSON too large");
            return online(env,JsonParser.parseString(inlineJson).getAsJsonObject());
        }
        String planPath=env.get("RAG_LIVE_PLAN_PATH");
        if(planPath!=null&&!planPath.trim().isEmpty()){
            rejectWindowsPath(planPath,"RAG_LIVE_PLAN_PATH");
            Path path=Paths.get(planPath);
            if(!path.isAbsolute()||!Files.isRegularFile(path)||Files.size(path)>65536)throw new IllegalArgumentException("Invalid online plan file");
            return online(env,JsonParser.parseString(new String(Files.readAllBytes(path),StandardCharsets.UTF_8)).getAsJsonObject());
        }
        Path repoPlan=Paths.get("config/phase52-online-smoke-plan.json");
        if(Files.isRegularFile(repoPlan)&&Files.size(repoPlan)<=65536){
            return online(env,JsonParser.parseString(new String(Files.readAllBytes(repoPlan),StandardCharsets.UTF_8)).getAsJsonObject());
        }
        try(java.io.InputStream in=GeminiRuntimeConfig.class.getResourceAsStream("/phase52-online-smoke-plan.json")){
            if(in!=null){
                java.io.ByteArrayOutputStream bos=new java.io.ByteArrayOutputStream();
                byte[] buf=new byte[4096];int r;
                while((r=in.read(buf))!=-1)bos.write(buf,0,r);
                return online(env,JsonParser.parseString(new String(bos.toByteArray(),StandardCharsets.UTF_8)).getAsJsonObject());
            }
        }
        throw new IllegalArgumentException("Online runtime plan missing (set RAG_ONLINE_PLAN_JSON or RAG_LIVE_PLAN_PATH)");
    }
    public static GeminiRuntimeConfig loadActive(Map<String,String> env)throws Exception{
        if(onlineRequested(env))return loadOnline(env);
        return load(env);
    }
    public static GeminiRuntimeConfig online(Map<String,String> env,JsonObject plan){
        return new GeminiRuntimeConfig(env,plan,true);
    }
    public GeminiRuntimeConfig(Map<String,String> env,JsonObject plan){
        this(env,plan,false);
    }
    private GeminiRuntimeConfig(Map<String,String> env,JsonObject plan,boolean online){
        this.onlineMode=online;
        if(!online){
            if(!requested(env)||!Arrays.asList("development","test").contains(env.get("RAG_ENVIRONMENT"))||!"127.0.0.1".equals(env.get("DB_HOST"))||!"fpt_rag_test".equals(env.get("DB_NAME"))||!"true".equals(env.get("RAG_DATABASE_APPROVED"))||!"false".equals(env.get("RAG_ENABLED")))throw new IllegalArgumentException("Require isolated loopback test DB and production RAG disabled");
            int port=Integer.parseInt(required(env,"DB_PORT"));if(port<1||port>65535)throw new IllegalArgumentException("Invalid isolated port");required(env,"DB_USER");required(env,"DB_PASSWORD");
            if(!"true".equals(env.get("RAG_LOCAL_GEMINI_AUTHORIZED")))throw new IllegalArgumentException("Separate LOCAL_GEMINI authorization missing");
            embeddingKey=required(env,"RAG_EMBEDDING_API_KEY");chatKey=required(env,"RAG_CHAT_TEST_API_KEY");
        }else{
            Mode m=mode(env);
            String ragEnv=required(env,"RAG_ENVIRONMENT").trim().toUpperCase(Locale.ROOT);
            if(!onlineRequested(env)||!m.name().equals(ragEnv))throw new IllegalArgumentException("RAG_RUNTIME_MODE and RAG_ENVIRONMENT must both be STAGING or PRODUCTION");
            rejectWindowsPath(env.get("KB_STORAGE_DIR"),"KB_STORAGE_DIR");
            rejectWindowsPath(env.get("RAG_LIVE_PLAN_PATH"),"RAG_LIVE_PLAN_PATH");
            String dbHost=required(env,"DB_HOST");
            String sslMode=env.get("DB_SSLMODE");
            if(sslMode==null||sslMode.trim().isEmpty())sslMode="require";
            utils.DBUtils.validateOnlineDatabase(ragEnv,dbHost,sslMode);
            if(!"require".equals(sslMode)&&!"verify-full".equals(sslMode))throw new IllegalArgumentException("Online database requires DB_SSLMODE=require or verify-full");
            int port=Integer.parseInt(required(env,"DB_PORT"));if(port<1||port>65535)throw new IllegalArgumentException("Invalid database port");
            required(env,"DB_NAME");required(env,"DB_USER");required(env,"DB_PASSWORD");
            if(!"true".equals(env.get("RAG_DATABASE_APPROVED"))||!"true".equals(env.get("RAG_ONLINE_GEMINI_AUTHORIZED"))||!"true".equals(env.get("RAG_EMBEDDING_CALLS_APPROVED")))
                throw new IllegalArgumentException("Explicit RAG_DATABASE_APPROVED, RAG_ONLINE_GEMINI_AUTHORIZED, and RAG_EMBEDDING_CALLS_APPROVED required");
            embeddingKey=required(env,"RAG_EMBEDDING_API_KEY");
            String testChatKey=env.get("RAG_CHAT_TEST_API_KEY");
            chatKey=(testChatKey!=null&&!testChatKey.trim().isEmpty())?required(env,"RAG_CHAT_TEST_API_KEY"):required(env,"GEMINI_API_KEY");
        }
        this.plan=plan.deepCopy();planId=string("planId");
        String idPattern=online?"(phase4|phase52|phase53|online)-[a-z0-9-]{4,64}":"phase4-[a-z0-9-]{4,64}";
        if(!planId.matches(idPattern)||!bool("approved")||(this.plan.has("executionAuthorized")&&!bool("executionAuthorized"))||!bool("projectModelsVerified")||!bool("restConfigurationVerified")||!bool("quotaVerified")||!bool("pricingVerified")||!"Free Tier".equals(string("embeddingTier"))||!"Free Tier".equals(string("chatTier")))throw new IllegalArgumentException("Fresh model/quota/pricing verification and approval required");
        if(online){
            boolean ragEnabled="true".equalsIgnoreCase(env.get("RAG_ENABLED"));
            if(ragEnabled&&!bool("productionRagEnabled"))throw new IllegalArgumentException("RAG_ENABLED=true requires productionRagEnabled=true in approved plan");
            if(!ragEnabled&&!"false".equalsIgnoreCase(env.getOrDefault("RAG_ENABLED","false")))throw new IllegalArgumentException("Invalid RAG_ENABLED value");
        }
        if(!"gemini-embedding-001".equals(string("embeddingModel"))||number("dimensions")!=768||!"gemini-3.8-flash".equals(string("chatModel"))||!"medium".equals(string("thinkingLevel"))||!"gemini-3.5-flash-lite".equals(string("backupModel"))||!"low".equals(string("backupThinkingLevel")))throw new IllegalArgumentException("Exact approved model/thinking configuration required");
        long now=Instant.now().getEpochSecond(),checked=this.plan.get("verifiedAtEpochSeconds").getAsLong();expires=this.plan.get("expiresAtEpochSeconds").getAsLong();
        if(checked>now||now-checked>3600||expires<=now||expires>checked+3600)throw new IllegalArgumentException("Verification expired (one-hour maximum)");
        embeddingProject=string("embeddingProjectAlias");chatProject=string("chatProjectAlias");
        if(!embeddingProject.matches("[A-Za-z0-9_-]{1,64}")||!chatProject.matches("[A-Za-z0-9_-]{1,64}"))throw new IllegalArgumentException("Verified project aliases required");
        totalLimit=bounded("maxRequests",1,100);documentLimit=bounded("documentRequests",0,100);queryLimit=bounded("queryRequests",0,100);chatLimit=bounded("chatRequests",0,100);
        if(documentLimit+queryLimit+chatLimit!=totalLimit)throw new IllegalArgumentException("Group and total budgets mismatch");
        rpm=bounded("requestsPerMinute",1,100);rpd=bounded("requestsPerDay",1,1000);tpm=bounded("tokensPerMinute",1,30000);dailyTokens=bounded("tokensPerDay",1,1000000);maxChatTokens=bounded("maxChatOutputTokens",1,1200);
        if(rpd<totalLimit||number("embeddingRequestsRemaining")<documentLimit+queryLimit||number("chatRequestsRemaining")<chatLimit||number("embeddingTokensRemaining")<tpm||number("chatTokensRemaining")<tpm)throw new IllegalArgumentException("Insufficient verified quotas");
        backupEnabled=bool("backupEnabled");if(backupEnabled&&(!bool("backupProjectModelVerified")||chatLimit<2))throw new IllegalArgumentException("Backup requires separate model access and request budget");
        allowOnlineCatalog=online&&bool("allowOnlineCatalog");
        // All outgoing document/query bodies and customer questions must be approved explicitly unless allowOnlineCatalog=true in online mode.
        for(String field:Arrays.asList("documentPayloadHashes","queryPayloadHashes","questions","documents"))
            if(!this.plan.has(field)||!this.plan.get(field).isJsonArray())throw new IllegalArgumentException("Missing approved allowlist "+field);
        planHash=VectorValues.hash(this.plan.toString());
    }
    private static void rejectWindowsPath(String value,String name){
        if(value==null||value.trim().isEmpty())return;
        String v=value.trim();
        if(v.matches("^[A-Za-z]:.*")||v.indexOf('\\')>=0)throw new IllegalArgumentException(name+" must not use a local Windows path in STAGING/PRODUCTION");
    }
    private static String required(Map<String,String> e,String n){String v=e.get(n);if(v==null||v.trim().isEmpty()||v.indexOf('\n')>=0||v.indexOf('\r')>=0)throw new IllegalArgumentException(n+" missing/invalid");return v;}
    private String string(String n){return plan.get(n).getAsString();}private boolean bool(String n){return plan.has(n)&&plan.get(n).isJsonPrimitive()&&plan.get(n).getAsJsonPrimitive().isBoolean()&&plan.get(n).getAsBoolean();}
    private int number(String n){JsonPrimitive v=plan.getAsJsonPrimitive(n);if(!v.isNumber()||!v.toString().matches("[0-9]+"))throw new IllegalArgumentException("Invalid limit "+n);return v.getAsInt();}
    private int bounded(String n,int lo,int hi){int v=number(n);if(v<lo||v>hi)throw new IllegalArgumentException("Unsafe limit "+n);return v;}
    public void current()throws RagException{if(Instant.now().getEpochSecond()>=expires)throw new RagException(RagException.Code.EXECUTION_BLOCKED);}
    public boolean isOnline(){return onlineMode;}
    String embeddingKey(){return embeddingKey;}String chatKey(){return chatKey;}
    public String project(String group){return "CHAT".equals(group)?chatProject:embeddingProject;}
    public int limit(String group){return "DOCUMENT".equals(group)?documentLimit:"QUERY".equals(group)?queryLimit:"CHAT".equals(group)?chatLimit:0;}
    public void payload(String group,JsonObject body)throws RagException{
        if(allowOnlineCatalog)return;
        String field="DOCUMENT".equals(group)?"documentPayloadHashes":"queryPayloadHashes";String hash=VectorValues.hash(body.toString());
        for(JsonElement e:plan.getAsJsonArray(field))if(hash.equals(e.getAsString()))return;throw new RagException(RagException.Code.EXECUTION_BLOCKED);
    }
    public void question(String text)throws RagException{
        if(allowOnlineCatalog&&text!=null&&!text.trim().isEmpty()&&text.length()<=1000)return;
        for(JsonElement q:plan.getAsJsonArray("questions"))if(text.equals(q.getAsString()))return;throw new RagException(RagException.Code.EXECUTION_BLOCKED);
    }
    public void document(long id,long version,String hash)throws RagException{
        if(allowOnlineCatalog&&id>0&&version>0&&hash!=null&&!hash.isEmpty())return;
        for(JsonElement e:plan.getAsJsonArray("documents")){JsonObject d=e.getAsJsonObject();if(d.has("id")&&d.get("id").getAsLong()==id&&d.get("version").getAsLong()==version&&hash.equals(d.get("hash").getAsString()))return;}
        throw new RagException(RagException.Code.EXECUTION_BLOCKED);
    }
    public RagConfig rag(){return new RagConfig(true,true,true,"gemini-embedding-001",768);}
    public String getPlanId(){return planId;}public int getTotalLimit(){return totalLimit;}public int getRpm(){return rpm;}public int getRpd(){return rpd;}public int getTpm(){return tpm;}
}
