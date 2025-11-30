package memory.algoritmos;
import memory.Frame;
import memory.PageTable;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class LRU implements ReplacementAlgorithm {
    // Mapa clave proceso:página -> timestamp del último acceso
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
    public Integer chooseVictimFrame(List<Frame> physicalMemory, Map<String, PageTable> processPageTables) {
        Integer victim = null;
        long oldestTime = Long.MAX_VALUE;

        for (Frame frame : physicalMemory) {
            if (!frame.isOccupied()) {
                // Si hay un frame libre, podemos usarlo como long lastAccess = -1Líctima inmediata
                return frame.getId();
            }

            // Encontrar qué proceso/página ocupa este frame
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

            // Si no se pudo determinar dueño, elegir este frame directamente
            if (ownerProcess == null || ownerPage == null) {
                return frame.getId();
            }

            // Obtener timestamp (0 si nunca fue accedida)
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
        // La carga implica acceso reciente
        currentTime++;
        accessTime.put(key(processId, pageNumber), currentTime);
    }

    @Override
    public void onPageAccess(String processId, int pageNumber) {
        // Actualiza el tiempo de último acceso
        currentTime++;
        accessTime.put(key(processId, pageNumber), currentTime);
    }

    @Override
    public void onPageUnloaded(String processId, int pageNumber, int frameId) {
        // Remover tracking cuando se descarga
        accessTime.remove(key(processId, pageNumber));
    }

    @Override
    public String getName() {
        return "LRU";
    }
}
