package rag.runtime;
import rag.*;
import java.sql.*;
import java.util.UUID;

/** Across JVMs: plan row lock + project advisory transaction lock; unknown wire state blocks reuse. */
public final class PostgresRequestLedger implements ApiRequestLedger {
    private final RagDatabase db;private final GeminiRuntimeConfig cfg;
    public PostgresRequestLedger(RagDatabase db,GeminiRuntimeConfig cfg){this.db=db;this.cfg=cfg;}
    public UUID reserve(String group,String operation,String model,int tokens)throws RagException{
        cfg.current();if(cfg.limit(group)<=0||tokens<=0)throw new RagException(RagException.Code.EXECUTION_BLOCKED);
        UUID id=UUID.randomUUID();
        try(Connection c=db.open()){c.setAutoCommit(false);try{
            try(PreparedStatement p=c.prepareStatement("INSERT INTO public.rag_runtime_plans(plan_id,plan_hash) VALUES(?,?) ON CONFLICT DO NOTHING")){p.setString(1,cfg.planId);p.setString(2,cfg.planHash);p.executeUpdate();}
            try(PreparedStatement p=c.prepareStatement("SELECT plan_hash FROM public.rag_runtime_plans WHERE plan_id=? FOR UPDATE")){p.setString(1,cfg.planId);try(ResultSet r=p.executeQuery()){if(!r.next()||!cfg.planHash.equals(r.getString(1)))throw new RagException(RagException.Code.EXECUTION_BLOCKED);}}
            try(PreparedStatement p=c.prepareStatement("SELECT pg_advisory_xact_lock(hashtext(?))")){p.setString(1,"fpt.phase4."+cfg.project(group));p.execute();}
            try(PreparedStatement p=c.prepareStatement("SELECT COUNT(*) FROM public.rag_api_requests WHERE project_alias=? AND state IN ('RESERVED','UNCERTAIN')")){
                p.setString(1,cfg.project(group));try(ResultSet r=p.executeQuery()){r.next();if(r.getLong(1)>0)throw new RagException(RagException.Code.EXECUTION_BLOCKED);}
            }
            try(PreparedStatement p=c.prepareStatement("SELECT COUNT(*),COUNT(*) FILTER(WHERE request_group=?),COUNT(*) FILTER(WHERE state IN ('RESERVED','UNCERTAIN')) FROM public.rag_api_requests WHERE plan_id=?")){
                p.setString(1,group);p.setString(2,cfg.planId);try(ResultSet r=p.executeQuery()){r.next();if(r.getInt(3)>0)throw new RagException(RagException.Code.EXECUTION_BLOCKED);if(r.getInt(1)>=cfg.totalLimit||r.getInt(2)>=cfg.limit(group))throw new RagException(RagException.Code.BUDGET_EXCEEDED);}
            }
            try(PreparedStatement p=c.prepareStatement("SELECT COUNT(*) FILTER(WHERE created_at>CURRENT_TIMESTAMP-INTERVAL '60 seconds'),COUNT(*) FILTER(WHERE created_at>CURRENT_TIMESTAMP-INTERVAL '24 hours'),COALESCE(SUM(reserved_tokens) FILTER(WHERE created_at>CURRENT_TIMESTAMP-INTERVAL '60 seconds'),0),COALESCE(SUM(reserved_tokens) FILTER(WHERE created_at>CURRENT_TIMESTAMP-INTERVAL '24 hours'),0) FROM public.rag_api_requests WHERE project_alias=?")){
                p.setString(1,cfg.project(group));try(ResultSet r=p.executeQuery()){r.next();if(r.getLong(1)>=cfg.rpm||r.getLong(2)>=cfg.rpd||r.getLong(3)+tokens>cfg.tpm||r.getLong(4)+tokens>cfg.dailyTokens)throw new RagException(RagException.Code.BUDGET_EXCEEDED);}
            }
            cfg.current();
            try(PreparedStatement p=c.prepareStatement("INSERT INTO public.rag_api_requests(id,plan_id,operation_hash,project_alias,request_group,model,reserved_tokens,state) VALUES(?,?,?,?,?,?,?,'RESERVED')")){
                p.setObject(1,id);p.setString(2,cfg.planId);p.setString(3,VectorValues.hash(operation));p.setString(4,cfg.project(group));p.setString(5,group);p.setString(6,model);p.setInt(7,tokens);p.executeUpdate();
            }c.commit();return id;
        }catch(Exception e){c.rollback();throw e;}}catch(RagException e){throw e;}catch(Exception e){throw new RagException(RagException.Code.EXECUTION_BLOCKED);}
    }
    public void finish(UUID id,String state,int status,String error,long latency,Integer prompt,Integer output,Integer total)throws RagException{
        if(!java.util.Arrays.asList("COMPLETED","FAILED","UNCERTAIN").contains(state)||(error!=null&&!error.matches("[A-Z0-9_]{1,32}")))throw new RagException(RagException.Code.EXECUTION_BLOCKED);
        try(Connection c=db.open();PreparedStatement p=c.prepareStatement("UPDATE public.rag_api_requests SET state=?,http_status=?,error_code=?,latency_ms=?,prompt_tokens=?,output_tokens=?,total_tokens=?,finished_at=CURRENT_TIMESTAMP WHERE id=? AND plan_id=? AND state='RESERVED'")){
            p.setString(1,state);p.setInt(2,status);p.setString(3,error);p.setLong(4,latency);p.setObject(5,prompt,Types.INTEGER);p.setObject(6,output,Types.INTEGER);p.setObject(7,total,Types.INTEGER);p.setObject(8,id);p.setString(9,cfg.planId);if(p.executeUpdate()!=1)throw new RagException(RagException.Code.EXECUTION_BLOCKED);
        }catch(RagException e){throw e;}catch(Exception e){throw new RagException(RagException.Code.EXECUTION_BLOCKED);}
    }
}
