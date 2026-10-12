package rag.runtime;
import rag.*;
import java.util.UUID;

/** Reserve durably BEFORE wire; failures consume slots. Evidence contains no key/body/vector. */
public interface ApiRequestLedger {
    UUID reserve(String group,String operation,String model,int tokenUpperBound)throws RagException;
    void finish(UUID id,String state,int status,String error,long latency,Integer prompt,Integer output,Integer total)throws RagException;
}
