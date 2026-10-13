package rag.files;

/** Build-time, offline validation. Never print file bodies or raw parser exception messages. */
public final class FileKnowledgeValidator {
    public static void main(String[] args){
        try{FileKnowledgeCatalog catalog=FileKnowledgeCatalog.load(java.nio.file.Paths.get(args[0]));System.out.println("Validated repository knowledge files: "+catalog.files.size()+"; API calls=0");}
        catch(Exception e){System.err.println("Invalid knowledge catalog: check UTF-8, metadata, size, uniqueness and source policy offline. No content logged.");System.exit(1);}
    }
}
