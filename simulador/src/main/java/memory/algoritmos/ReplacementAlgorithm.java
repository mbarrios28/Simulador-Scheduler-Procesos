package memory.algoritmos;

import memory.Frame;
import memory.PageTable;
import java.util.List;
import java.util.Map;

public interface ReplacementAlgorithm {
    
    Integer chooseVictimFrame(List<Frame> physicalMemory, 
        Map<String, PageTable> processPageTables,
        String excludeProcessId);
    
    void onPageLoaded(String processId, int pageNumber, int frameId);

    void onPageAccess(String processId, int pageNumber);
    
    void onPageUnloaded(String processId, int pageNumber, int frameId);
    
    String getName();
}