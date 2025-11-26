package memory.algoritmos;
import java.util.LinkedList;
import java.util.Queue;
import memory.Frame;
import memory.PageTable;
import java.util.List;
import java.util.Map;


public class FIFO implements ReplacementAlgorithm {
    private Queue<Integer> frameQueue; // cola de frames en orden de carga

    public FIFO() {
        this.frameQueue = new LinkedList<>();
    }

    @Override
    public Integer chooseVictimFrame(List<Frame> phsycalMemory, Map<String, PageTable> processPageTables) {
        // Elige el frame que lleva más tiempo en memoria (el primero en la cola)
        return frameQueue.peek(); // solo mira el primero sin removerlo
    }

    @Override
    public void onPageLoaded(String processId, int pageNumber, int frameId) {
        frameQueue.add(frameId);
    }

     @Override
    public void onPageUnloaded(String processId, int pageNumber, int frameId) {
        // Remover el frame de la cola (debería ser el primero)
        frameQueue.poll();
    }

    @Override
    public void onPageAccess(String processId, int pageNumber) {
        // FIFO no necesita saber sobre accesos
    }
  
    @Override
    public String getName() {
        return "FIFO";
    }
}   