package rag;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** Test-only stdin validator; no credentials, HTTP or database. Emits metrics, never vector data. */
public final class RagEmbeddingVectorCheck {
    public static void main(String[] args) {
        JsonObject result=new JsonObject();result.add("returnedDimensions",JsonNull.INSTANCE);
        result.addProperty("vectorValid",false);result.add("normBefore",JsonNull.INSTANCE);result.add("normAfter",JsonNull.INSTANCE);
        result.add("errorCode",JsonNull.INSTANCE);
        try {
            StringBuilder input=new StringBuilder();
            try(Reader reader=new InputStreamReader(System.in,StandardCharsets.UTF_8)) {
                char[] buffer=new char[4096];int n;
                while((n=reader.read(buffer))!=-1){if(input.length()+n>65536)throw new IllegalArgumentException();input.append(buffer,0,n);}
            }
            JsonArray values=JsonParser.parseString(input.toString()).getAsJsonArray();
            result.addProperty("returnedDimensions",values.size());
            if(values.size()!=768)throw new IllegalArgumentException();
            float[] vector=new float[values.size()];double before=0;
            for(int i=0;i<vector.length;i++) {
                JsonElement item=values.get(i);
                if(!item.isJsonPrimitive() || !item.getAsJsonPrimitive().isNumber())throw new IllegalArgumentException();
                vector[i]=item.getAsFloat();
                if(!Float.isFinite(vector[i]))throw new IllegalArgumentException();before+=(double)vector[i]*vector[i];
            }
            float[] normalized=VectorValues.normalized(vector,768);double after=0;
            for(float value:normalized)after+=(double)value*value;
            result.addProperty("normBefore",Math.sqrt(before));result.addProperty("normAfter",Math.sqrt(after));result.addProperty("vectorValid",true);
        }catch(Exception e){result.addProperty("errorCode","INVALID_VECTOR");}
        System.out.println(new GsonBuilder().serializeNulls().create().toJson(result));
    }
}
