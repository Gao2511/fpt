package rag;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

public final class VectorValues {
    private VectorValues() {}
    public static String hash(String text) {
        try {
            byte[] digest=MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder out=new StringBuilder(); for(byte b:digest) out.append(String.format(java.util.Locale.ROOT,"%02x",b&255));
            return out.toString();
        } catch(Exception e) { throw new IllegalStateException("Hash unavailable"); }
    }
    public static float[] normalized(float[] values, int dimensions) throws RagException {
        if(values==null || values.length!=dimensions) throw new RagException(RagException.Code.INVALID_VECTOR);
        double norm=0; for(float v:values) { if(!Float.isFinite(v)) throw new RagException(RagException.Code.INVALID_VECTOR); norm+=(double)v*v; }
        if(norm<=0 || !Double.isFinite(norm)) throw new RagException(RagException.Code.INVALID_VECTOR);
        float[] result=new float[values.length]; norm=Math.sqrt(norm);
        for(int i=0;i<values.length;i++) result[i]=(float)(values[i]/norm); return result;
    }
    public static String sql(float[] values) throws RagException {
        float[] checked=normalized(values,768); StringBuilder s=new StringBuilder("[");
        for(int i=0;i<checked.length;i++) { if(i>0)s.append(','); s.append(Float.toString(checked[i])); }
        return s.append(']').toString();
    }
    public static float[] parse(String text) throws RagException {
        try {
            if(text==null || !text.startsWith("[") || !text.endsWith("]")) throw new IllegalArgumentException();
            String[] parts=text.substring(1,text.length()-1).split(","); float[] values=new float[parts.length];
            for(int i=0;i<parts.length;i++)values[i]=Float.parseFloat(parts[i]); return normalized(values,768);
        } catch(Exception e) { throw new RagException(RagException.Code.INVALID_VECTOR); }
    }
}
