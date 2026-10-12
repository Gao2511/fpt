package rag;
import java.sql.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;

/** Explicit opt-in DDL for an EMPTY, separate LOOPBACK fpt_rag_test database, never website credentials. */
public final class RagLocalDatabaseSetup {
    public static void main(String[] args) {
        try {
            if(!"true".equals(System.getenv("RAG_TEST_DATABASE_APPROVED")) || !"development".equals(System.getenv("RAG_ENVIRONMENT")) ||
                !"127.0.0.1".equals(System.getenv("DB_HOST")) || !"fpt_rag_test".equals(System.getenv("DB_NAME")))throw new IllegalStateException();
            try(Connection c=utils.DBUtils.getConnection();Statement s=c.createStatement()) {
                try(ResultSet r=s.executeQuery("SELECT count(*) FROM information_schema.tables WHERE table_schema='public' AND table_type='BASE TABLE'")){r.next();if(r.getLong(1)!=0)throw new IllegalStateException();}
                try(ResultSet r=s.executeQuery("SELECT count(*) FROM information_schema.views WHERE table_schema='public'")){r.next();if(r.getLong(1)!=0)throw new IllegalStateException();}
                s.execute("SET statement_timeout='10000ms'");s.execute("CREATE EXTENSION IF NOT EXISTS vector");
                Path root=Paths.get(args[0]);
                for(String file:new String[]{"002-rag-mvp.sql","003-rag-source-policy.sql"})s.execute(new String(Files.readAllBytes(root.resolve("database/migrations").resolve(file)),StandardCharsets.UTF_8));
                System.out.println("Isolated local test database prepared; migrations 002+003 only, no AI calls.");
            }
        }catch(Exception e){System.err.println("Local test setup refused/failed: "+e.getClass().getSimpleName()+"; existing data preserved, no secret diagnostic");System.exit(1);}
    }
}
