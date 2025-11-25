package memory;
import java.util.List;
import java.util.Queue;
import java.util.Map;
import java.util.ArrayList;

public class MemoryManager {
    private List<Frame> physicalMemory;
    private Queue<Frame> freeFrames;
    private Map<String, PageTable> processPageTables;

    public MemoryManager(int totalFrames) {
        physicalMemory = new ArrayList<>();
        freeFrames = new java.util.LinkedList<>();
        processPageTables = new java.util.HashMap<>();
        for(int i = 0; i < totalFrames; i++) {
            Frame frame = new Frame(i);
            physicalMemory.add(frame);
            freeFrames.add(frame);
        }
        System.out.println("Memory Manager inicializada con " + totalFrames + " frames.");
    }

    // crear un nuevo proceso con su pagina por defecto
    public void createProcess(String processId, int totalPages) {
        PageTable pageTable = new PageTable(processId, totalPages);
        processPageTables.put(processId, pageTable);
        System.out.println("Proceso " + processId + " creado con " + totalPages + " páginas.");
    }

    // verificar si una pagina es cargada en memoria
    public boolean isPageLoaded(String processId, int pageNumber) {
        PageTable pageTable = processPageTables.get(processId);
        if (pageTable != null) {
            return pageTable.isPageLoaded(pageNumber);
        }
        return false;
    }

    // cargar una pagina a la memoria
    public boolean loadPage(String processId, int pageNumber) {
        if ( isPageLoaded(processId, pageNumber) ) {
            System.out.println("Página " + pageNumber + " del proceso " + processId + " ya está en memoria.");
            return true;
        }  
        if (freeFrames.isEmpty()) {
            System.out.println("No hay frames disponibles para " + processId + "-Page" + pageNumber);
            // Aquí se podría implementar un algoritmo de reemplazo de páginas ...
            System.out.println("Memoria llena - (aplicar algun algoritmo de reemplazo aquí)");
            return false;
        }
        Frame freeFrame = freeFrames.poll();

        // frame solo guarda el processId
        freeFrame.occupy(processId);

        // PageTable guarda toda la informacion (pageNumber + frameNumber)
        PageTable pageTable = processPageTables.get(processId);
        pageTable.pageLoaded(pageNumber, freeFrame.getId());

        System.out.println("SUCCESS: Página " + pageNumber + " del proceso " + processId + " cargada en Frame " + freeFrame.getId());
        return true;
    }

    // obtener la tabla de páginas de un proceso
    public PageTable getPageTable(String processId) {
        return processPageTables.get(processId);
    }

    // Encontrar qué página está en un frame específico
    public String findPageInFrame(Frame frame) {
        if (!frame.isOccupied()) {
            return "FREE";
        }
        
        PageTable pageTable = processPageTables.get(frame.getProcessId());
        if (pageTable != null) {
            Integer pageNumber = pageTable.findPageInFrame(frame.getId());
            if (pageNumber != null) {
                return frame.getProcessId() + "-Page" + pageNumber;
            }
        }
        return frame.getProcessId() + "-UNKNOWN";
    }

    public void printMemoryStatus() {
        System.out.println("\n=== MEMORY STATUS ===");
        for (Frame frame : physicalMemory) {
            if (frame.isOccupied()) {
                String pageInfo = findPageInFrame(frame);
                System.out.println("Frame " + frame.getId() + ": " + pageInfo);
            } else {
                System.out.println("Frame " + frame.getId() + ": FREE");
            }
        }
        System.out.println("Free frames: " + freeFrames.size());
    }
    
    // Print all page tables
    public void printAllPageTables() {
        System.out.println("\n=== ALL PAGE TABLES ===");
        for (PageTable pageTable : processPageTables.values()) {
            pageTable.printPageTable();
        }
    }

}