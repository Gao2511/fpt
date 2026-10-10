package controller;

import ai.ConsultationBehaviorTest;
import ai.dto.ChatResponse;
import ai.session.*;
import dto.*;
import utils.RegistrationSubmission;
import javax.servlet.http.*;
import java.lang.reflect.*;
import java.io.*;
import java.util.*;

/** Servlet behavior with isolated sessions and mocked persistence/email. */
public class ChatRegistrationTest {
    static int passed;
    static void check(boolean value,String reason){ConsultationBehaviorTest.check(value,reason);}
    static class Session {
        final Map<String,Object> values=new HashMap<>();final String id=UUID.randomUUID().toString();
        final HttpSession proxy=(HttpSession)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpSession.class},(object,method,args)->{
            switch(method.getName()){
                case "getId":return id;case "getAttribute":return values.get(args[0]);
                case "setAttribute":values.put((String)args[0],args[1]);return null;
                case "removeAttribute":values.remove(args[0]);return null;
                default:return defaultValue(method.getReturnType());
            }
        });
    }
    static class Exchange {
        final Map<String,String> params=new HashMap<>();final Session session;
        final StringWriter output=new StringWriter();int status=200;String redirect;
        final HttpServletRequest request;final HttpServletResponse response;
        Exchange(Session session){
            this.session=session;
            request=(HttpServletRequest)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpServletRequest.class},(o,m,a)->{
                switch(m.getName()){case "getParameter":return params.get(a[0]);case "getSession":return session.proxy;case "getContextPath":return "/fpt-sale";default:return defaultValue(m.getReturnType());}
            });
            response=(HttpServletResponse)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpServletResponse.class},(o,m,a)->{
                switch(m.getName()){case "getWriter":return new PrintWriter(output);case "setStatus":status=(int)a[0];return null;case "sendRedirect":redirect=(String)a[0];return null;default:return defaultValue(m.getReturnType());}
            });
        }
    }
    static Object defaultValue(Class<?> type){if(type==boolean.class)return false;if(type==int.class)return 0;if(type==long.class)return 0L;return null;}
    static class Chat extends AIChatServlet {
        final ConsultationBehaviorTest.MockProvider provider=new ConsultationBehaviorTest.MockProvider();
        protected ChatResponse consult(String question,String id) {
            try{return ConsultationBehaviorTest.ask(question,ChatSessionManager.getOrCreate(id),provider,null);}catch(Exception e){throw new RuntimeException(e);}
        }
    }
    static class Registration extends ContactServlet {
        int saves,notifications,saveResult=42;CustomerDTO saved;
        protected int saveCustomer(CustomerDTO customer){saves++;saved=customer;return saveResult;}
        protected PackageDTO findPackage(int id){if(id!=3)return null;PackageDTO p=new PackageDTO();p.setId(3);p.setName("220K");return p;}
        protected boolean sendNotification(String recipient,String subject,String body){notifications++;return true;}
        protected void logNotification(boolean sent,int customerId,String subject){}
    }
    static Exchange form(Session s,String token){Exchange e=new Exchange(s);e.params.put("registration_token",token);e.params.put("registration_consent","yes");e.params.put("user_name","Khách thử nghiệm");e.params.put("user_phone","+84 918 234 567");e.params.put("user_address","Địa chỉ thử nghiệm");e.params.put("user_package_id","3");return e;}
    interface Test{void run()throws Exception;}
    static void test(String label,Test test)throws Exception{test.run();passed++;System.out.println("PASS " + label);}
    public static void main(String[]args)throws Exception{
        test("HTTP provider failures return technical contact with error status",()->{
            AIChatServlet servlet=new AIChatServlet() {
                protected ChatResponse consult(String message,String id) throws ai.exception.AIException {
                    throw new ai.exception.AIException(ai.exception.AIException.ErrorType.TIMEOUT,"offline","Mock");
                }
            };
            Exchange e=new Exchange(new Session());e.params.put("message","Xin hỗ trợ.");servlet.doPost(e.request,e.response);
            check(e.status==503 && e.output.toString().contains("0932 079 469") && e.output.toString().contains("trục trặc") && !e.output.toString().contains("ngoài phạm vi"),e.output.toString());
        });
        test("HTTP gaming consultation is not rejected by substring middleware",()->{
            Exchange e=new Exchange(new Session());e.params.put("message","Tôi có 200 nghìn, ở trọ 2 người, chủ yếu chơi game.");new Chat().doPost(e.request,e.response);
            check(e.status==200 && e.output.toString().contains("195.000") && !e.output.toString().contains("Xin lỗi"),e.output.toString());
        });
        test("HTTP only publishes customer fields and safe registration path",()->{
            Exchange e=new Exchange(new Session());e.params.put("message","Tôi muốn đăng ký gói 220K.");new Chat().doPost(e.request,e.response);
            String output=e.output.toString();check(output.contains("/package-detail?id=3#registration") && !output.contains("packageIds") && !output.contains("state") && !output.contains("promptTokens"),output);
        });
        test("HTTP validation and rate limiting return actual error status",()->{
            Session s=new Session();Chat servlet=new Chat();Exchange empty=new Exchange(s);empty.params.put("message","");servlet.doPost(empty.request,empty.response);check(empty.status==400,"Missing 400");
            for(int i=0;i<10;i++){Exchange e=new Exchange(s);e.params.put("message","Chào bạn!");servlet.doPost(e.request,e.response);check(e.status==200,"Premature limit");}
            Exchange blocked=new Exchange(s);blocked.params.put("message","Chào bạn!");servlet.doPost(blocked.request,blocked.response);check(blocked.status==429,"Missing 429");
        });
        test("registration requires explicit consent and a private form token",()->{
            Registration servlet=new Registration();Session s=new Session();String token=RegistrationSubmission.issue(s.proxy);
            Exchange noConsent=form(s,token);noConsent.params.remove("registration_consent");servlet.doPost(noConsent.request,noConsent.response);check(servlet.saves==0,"Saved without consent");
            Exchange foreign=form(new Session(),token);servlet.doPost(foreign.request,foreign.response);check(servlet.saves==0,"Cross-session token accepted");
        });
        test("server validates phone, address and package",()->{
            Registration servlet=new Registration();Session s=new Session();String token=RegistrationSubmission.issue(s.proxy);
            for(String field:Arrays.asList("user_phone","user_address","user_package_id")){
                Exchange invalid=form(s,token);invalid.params.put(field,field.equals("user_package_id")?"999":field.equals("user_phone")?"123":"");servlet.doPost(invalid.request,invalid.response);check(servlet.saves==0,"Invalid " + field + " persisted");
            }
        });
        test("persistence result controls success, duplicate request never re-inserts or emails",()->{
            Registration servlet=new Registration();Session s=new Session();String token=RegistrationSubmission.issue(s.proxy);
            Exchange e=form(s,token);servlet.doPost(e.request,e.response);check(servlet.saves==1 && servlet.notifications==1 && "0918234567".equals(servlet.saved.getPhone()),"Failed persistence");
            check("success".equals(s.values.get("messageType")) && s.values.get("message").toString().contains("tiếp nhận"),"Missing confirmed acknowledgement");
            Exchange duplicate=form(s,token);servlet.doPost(duplicate.request,duplicate.response);check(servlet.saves==1 && servlet.notifications==1,"Duplicate submission");
        });
        test("failed insert reports error and allows safe retry",()->{
            Registration servlet=new Registration();servlet.saveResult=-1;Session s=new Session();String token=RegistrationSubmission.issue(s.proxy);
            Exchange failure=form(s,token);servlet.doPost(failure.request,failure.response);check("error".equals(s.values.get("messageType")) && servlet.notifications==0,"Claimed false success");
            servlet.saveResult=43;Exchange retry=form(s,token);servlet.doPost(retry.request,retry.response);check(servlet.saves==2 && servlet.notifications==1 && "success".equals(s.values.get("messageType")),"Retry blocked");
        });
        test("session destruction removes public and preview histories",()->{
            Session s=new Session();ChatSessionData publicData=ChatSessionManager.getOrCreate(s.id),previewData=ChatSessionManager.getOrCreate("playground_"+s.id);
            new ChatSessionLifecycle().sessionDestroyed(new HttpSessionEvent(s.proxy));
            check(ChatSessionManager.getOrCreate(s.id)!=publicData && ChatSessionManager.getOrCreate("playground_"+s.id)!=previewData,"Expired customer data retained");
        });
        System.out.println("ChatRegistrationTest: " + passed + " passed");
    }
}
