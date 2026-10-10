package security;
import dto.UserDTO;
public final class DemoAccount {
    private DemoAccount(){}
    public static UserDTO authenticate(String name,String password){if(!"true".equalsIgnoreCase(System.getenv("DEMO_ADMIN_ENABLED")))return null;String expected=System.getenv("DEMO_ADMIN_IDENTIFIER"),hash=System.getenv("DEMO_ADMIN_PASSWORD_HASH");if(expected==null||hash==null||!hash.startsWith("pbkdf2$")||!expected.equals(name)||!Passwords.verify(password,hash))return null;UserDTO u=new UserDTO();u.setId(-1);u.setUsername(name);u.setFullName("Demo Administrator");u.setRole("demo_admin");u.setActive(true);return u;}
}
