package security;
import java.security.*;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.util.Base64;
/** Versioned PBKDF2; legacy passwords remain readable until verified login. */
public final class Passwords {
    private Passwords(){}
    public static String hash(String p){byte[] s=new byte[16];new SecureRandom().nextBytes(s);return "pbkdf2$210000$"+Base64.getEncoder().encodeToString(s)+"$"+Base64.getEncoder().encodeToString(derive(p,s,210000));}
    private static byte[] derive(String p,byte[] s,int n){PBEKeySpec spec=new PBEKeySpec(p.toCharArray(),s,n,256);try{return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();}catch(Exception e){throw new IllegalStateException(e);}finally{spec.clearPassword();}}
    public static boolean verify(String p,String stored){if(p==null||stored==null)return false;if(!stored.startsWith("pbkdf2$"))return MessageDigest.isEqual(p.getBytes(StandardCharsets.UTF_8),stored.getBytes(StandardCharsets.UTF_8));try{String[] parts=stored.split("\\$");if(parts.length!=4)return false;int n=Integer.parseInt(parts[1]);byte[] s=Base64.getDecoder().decode(parts[2]),h=Base64.getDecoder().decode(parts[3]);return n>=210000&&n<=600000&&s.length==16&&h.length==32&&MessageDigest.isEqual(h,derive(p,s,n));}catch(Exception e){return false;}}
    public static void main(String[] args){java.io.Console c=System.console();if(c==null)throw new IllegalStateException("Run in an interactive terminal");char[] p=c.readPassword("Demo password (12+ characters): ");try{if(p==null||p.length<12)throw new IllegalArgumentException("Use 12+ characters");System.out.println(hash(new String(p)));}finally{if(p!=null)java.util.Arrays.fill(p,'\0');}}
}
