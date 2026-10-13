package rag.files;

import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import javax.servlet.ServletContext;
import rag.*;

/** Immutable deployed catalog. Loading performs no JDBC or provider calls. Fail closed for repo sources. */
public final class FileKnowledgeCatalog {
    public final Map<String,KnowledgeFile> files;
    private static volatile FileKnowledgeCatalog deployed;
    public FileKnowledgeCatalog(Collection<KnowledgeFile> entries){
        Map<String,KnowledgeFile> out=new LinkedHashMap<>();int total=0;
        for(KnowledgeFile f:entries){if(out.put(f.id,f)!=null)throw new IllegalArgumentException("ID tài liệu trùng");total+=f.body.length();if(out.size()>200||total>2000000)throw new IllegalArgumentException("Catalog vượt 200 file/2 triệu ký tự");}
        for(KnowledgeFile f:out.values()){Set<String> seen=new HashSet<>();String id=f.id;while(out.containsKey(id)){if(!seen.add(id))throw new IllegalArgumentException("replaces có chu kỳ");id=out.get(id).replaces;}}
        files=Collections.unmodifiableMap(out);
    }
    public static void install(FileKnowledgeCatalog catalog){deployed=catalog;}
    public static boolean ready(){return deployed!=null;}
    public static FileKnowledgeCatalog deployed(){if(deployed==null)throw new IllegalStateException("Catalog deployment chưa hợp lệ");return deployed;}
    public Set<String> eligibleIds(){Set<String> ids=new HashSet<>();for(KnowledgeFile f:files.values())if(f.effectiveNow()){
        ids.add(f.id);String ancestor=f.replaces;Set<String> seen=new HashSet<>();while(!ancestor.isEmpty()&&seen.add(ancestor)){
            KnowledgeFile old=files.get(ancestor);if(old!=null&&!old.effectiveNow())break;ids.add(ancestor);ancestor=old==null?"":old.replaces;
        }
    }return ids;}
    public static boolean allowedSource(String source){
        if(source==null||!source.startsWith("repo:"))return true;
        if(!source.matches("repo:[a-z0-9][a-z0-9._-]{0,79}/v[1-9][0-9]*"))return false;
        FileKnowledgeCatalog catalog=deployed;if(catalog==null)return false;
        String id=source.substring(5,source.lastIndexOf("/v"));KnowledgeFile current=catalog.files.get(id);
        try{if(current!=null&&Long.parseLong(source.substring(source.lastIndexOf("/v")+2))>current.version)return false;}catch(NumberFormatException invalid){return false;}
        return catalog.eligibleIds().contains(id);
    }
    public static boolean allowedRevision(String source,String fileSha){
        if(!allowedSource(source))return false;if(!source.startsWith("repo:"))return true;
        KnowledgeFile current=deployed.files.get(source.substring(5,source.lastIndexOf("/v")));
        return current==null||!source.equals(current.source())||current.sha.equals(fileSha);
    }
    private static KnowledgeFile read(InputStream input,String path)throws Exception {
        MessageDigest digest=MessageDigest.getInstance("SHA-256");
        try(InputStream bounded=new FilterInputStream(input){int bytes;public int read(byte[] b,int o,int n)throws IOException{int r=super.read(b,o,n);if(r>0&&(bytes+=r)>KnowledgeLimits.upload())throw new IOException("File vượt giới hạn byte");return r;}public int read()throws IOException{int r=super.read();if(r>=0&&++bytes>KnowledgeLimits.upload())throw new IOException("File vượt giới hạn byte");return r;}};
            Reader reader=new InputStreamReader(new DigestInputStream(bounded,digest),StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT))){
            StringBuilder text=new StringBuilder();char[] buf=new char[4096];int n;while((n=reader.read(buf))!=-1){if(text.length()+n>KnowledgeLimits.extracted()+4100)throw new IOException("File vượt giới hạn ký tự");text.append(buf,0,n);}
            StringBuilder hash=new StringBuilder();for(byte b:digest.digest())hash.append(String.format(Locale.ROOT,"%02x",b&255));return KnowledgeFile.parse(text.toString(),path,hash.toString());
        }
    }
    public static FileKnowledgeCatalog load(ServletContext context)throws Exception {
        if(context.getResourcePaths("/WEB-INF/knowledge/")==null)throw new IOException("Thiếu catalog trong WAR");
        List<KnowledgeFile> files=new ArrayList<>();walk(context,"/WEB-INF/knowledge/",files);return new FileKnowledgeCatalog(files);
    }
    private static void walk(ServletContext context,String dir,List<KnowledgeFile> out)throws Exception {
        Set<String> paths=context.getResourcePaths(dir);if(paths==null)return;
        for(String path:new TreeSet<>(paths)){if(path.substring("/WEB-INF/knowledge/".length()).split("/").length>8)throw new IOException("Catalog quá sâu");if(path.endsWith("/"))walk(context,path,out);else if(path.endsWith(".md")||path.endsWith(".txt")){if(out.size()>=200)throw new IOException("Quá nhiều file");out.add(read(context.getResourceAsStream(path),path.substring("/WEB-INF/knowledge/".length())));}}
    }
    public static FileKnowledgeCatalog load(Path root)throws Exception {
        List<KnowledgeFile> out=new ArrayList<>();Path canonical=root.toRealPath();
        try(java.util.stream.Stream<Path> paths=Files.walk(canonical,8)){
            Iterator<Path> iter=paths.sorted().iterator();while(iter.hasNext()){Path p=iter.next();if(Files.isSymbolicLink(p))throw new IOException("Không nhận symlink");String name=p.toString();if(Files.isRegularFile(p)&&(name.endsWith(".md")||name.endsWith(".txt"))){if(!p.toRealPath().startsWith(canonical)||out.size()>=200)throw new IOException("Đường dẫn/số file không hợp lệ");out.add(read(Files.newInputStream(p),canonical.relativize(p).toString().replace('\\','/')));}}
        }return new FileKnowledgeCatalog(out);
    }
}
