package memory.algoritmos;
import java.util.LinkedList;
import java.util.Queue;
import memory.Frame;
import memory.PageTable;
import java.util.List;
import java.util.Map;


public class FIFO implements ReplacementAlgorithm {
    private Queue<Integer> frameQueue; 

    public FIFO() {
        this.frameQueue = new LinkedList<>();
    }

    @Override
    public Integer chooseVictimFrame(List<Frame> physicalMemory, Map<String, PageTable> processPageTables, String excludeProcessId) {
        for (Integer frameId : frameQueue) {
            // el frame pertenece al proceso excluido?
            boolean belongsToExcluded = false;
            if (excludeProcessId != null) {
                PageTable pt = processPageTables.get(excludeProcessId);
                if (pt != null && pt.findPageInFrame(frameId) != null) {
                    belongsToExcluded = true;
                }
            }
            
            if (!belongsToExcluded) {
                return frameId;
            }
        }
        
        return null;
    }

    @Override
    public void onPageLoaded(String processId, int pageNumber, int frameId) {
        frameQueue.add(frameId);
    }

     @Override
    public void onPageUnloaded(String processId, int pageNumber, int frameId) {
        // para el frame específico de la cola
        frameQueue.remove(frameId);
    }

    @Override
    public void onPageAccess(String processId, int pageNumber) {
    }
  
    @Override
    public String getName() {
        return "FIFO";
    }
}   