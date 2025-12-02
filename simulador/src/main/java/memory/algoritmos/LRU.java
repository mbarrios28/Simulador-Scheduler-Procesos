package memory.algoritmos;
import memory.Frame;
import memory.PageTable;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class LRU implements ReplacementAlgorithm {
    private final Map<String, Long> accessTime;
    private long currentTime;

    public LRU() {
        this.accessTime = new HashMap<>();
        this.currentTime = 0L;
    }

    private String key(String processId, int pageNumber) {
        return processId + ":" + pageNumber;
    }

    @Override
    public Integer chooseVictimFrame(List<Frame> physicalMemory, Map<String, PageTable> processPageTables, String excludeProcessId) {
        Integer victim = null;
        long oldestTime = Long.MAX_VALUE;

        for (Frame frame : physicalMemory) {
            if (!frame.isOccupied()) {
                return frame.getId();
            }

            String ownerProcess = null;
            Integer ownerPage = null;
            for (Map.Entry<String, PageTable> entry : processPageTables.entrySet()) {
                Integer pageInFrame = entry.getValue().findPageInFrame(frame.getId());
                if (pageInFrame != null) {
                    ownerProcess = entry.getKey();
                    ownerPage = pageInFrame;
                    break;
                }
            }

            if (ownerProcess == null || ownerPage == null) {
                return frame.getId();
            }

            if (excludeProcessId != null && ownerProcess.equals(excludeProcessId)) {
                continue;
            }

            long lastAccess = accessTime.getOrDefault(key(ownerProcess, ownerPage), 0L);

            if (victim == null || lastAccess < oldestTime) {
                victim = frame.getId();
                oldestTime = lastAccess;
            }
        }

        return victim;
    }

    @Override
    public void onPageLoaded(String processId, int pageNumber, int frameId) {
        currentTime++;
        accessTime.put(key(processId, pageNumber), currentTime);
    }

    @Override
    public void onPageAccess(String processId, int pageNumber) {
        currentTime++;
        accessTime.put(key(processId, pageNumber), currentTime);
    }

    @Override
    public void onPageUnloaded(String processId, int pageNumber, int frameId) {
        accessTime.remove(key(processId, pageNumber));
    }

    @Override
    public String getName() {
        return "LRU";
    }
}
