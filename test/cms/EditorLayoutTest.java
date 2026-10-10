package cms;
import java.util.*;
public final class EditorLayoutTest {
    public static void main(String[] args){
        for(boolean business:new boolean[]{false,true}){
            Set<String> expected=new HashSet<>(),actual=new HashSet<>();
            for(SiteContent.Field field:SiteContent.fields(business))expected.add(field.getKey());
            List<EditorLayout.Section> sections=EditorLayout.sections(business);
            if(sections.size()!=(business?4:8))throw new AssertionError("Wrong section count");
            for(EditorLayout.Section section:sections)for(EditorLayout.Group group:section.getGroups())for(SiteContent.Field field:group.getFields()){
                if(!actual.add(field.getKey()))throw new AssertionError("Duplicate field: "+field.getKey());
                String key=field.getKey();
                if(key.startsWith("hero.image")&&!section.getId().equals("images"))throw new AssertionError("Banner misplaced");
                if(key.startsWith("business.mesh")&&!section.getId().equals("mesh"))throw new AssertionError("Mesh misplaced");
                if(key.startsWith("business.promotion")&&!section.getId().equals("promotion"))throw new AssertionError("Promotion misplaced");
                if(key.equals("business.standardFee")&&!section.getId().equals("fees"))throw new AssertionError("Internet fee misplaced");
            }
            if(!actual.equals(expected))throw new AssertionError("Lost CMS controls");
        }
        System.out.println("EditorLayoutTest: all CMS fields grouped once; banner, fee, Mesh and promotion sections verified");
    }
}
