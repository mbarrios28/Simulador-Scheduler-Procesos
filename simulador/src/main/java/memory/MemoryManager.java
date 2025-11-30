package memory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import memory.algoritmos.ReplacementAlgorithm;

public class MemoryManager {
    private List<Frame> physicalMemory;
    private Queue<Frame> freeFrames;
    private Map<String, PageTable> processPageTables;

    private ReplacementAlgorithm replacementAlgorithm;

    // CONTADORES DE ESTADISTICA
    private Map<String, Integer> pageFaultCount;
    private Map<String, Integer> replacementCount;



    public MemoryManager(int totalFrames, ReplacementAlgorithm algorithm) {
        physicalMemory = new ArrayList<>();
        freeFrames = new java.util.LinkedList<>();
        processPageTables = new java.util.HashMap<>();
        for(int i = 0; i < totalFrames; i++) {
            Frame frame = new Frame(i);
            physicalMemory.add(frame);
            freeFrames.add(frame);
        }
        this.replacementAlgorithm = algorithm;
        this.pageFaultCount = new HashMap<>();
        this.replacementCount = new HashMap<>();
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
        // Si ya está cargado solo notificar acceso
        if ( isPageLoaded(processId, pageNumber) ) {
            System.out.println("Página " + pageNumber + " del proceso " + processId + " ya está en memoria.");
            replacementAlgorithm.onPageAccess(processId, pageNumber);
            return true;
        }  
        Frame targetFrame;

        // Incrementar contador de page faults
        pageFaultCount.put(processId, pageFaultCount.getOrDefault(processId, 0) + 1);

        if (freeFrames.isEmpty()) {

            // REEMPLAZO 
            Integer victimFrameId = replacementAlgorithm.chooseVictimFrame(physicalMemory, processPageTables);
            
            if (victimFrameId == null) {
                System.out.println("ERROR: No se pudo elegir víctima para reemplazo");
                return false;
            }
            
            targetFrame = physicalMemory.get(victimFrameId);
            // Expulsar la página víctima
            // Determinar proceso y página víctima recorriendo tablas
            String victimProcessId = null;
            PageTable victimPageTable = null;
            Integer victimPageNumber = null;
            for (Map.Entry<String, PageTable> entry : processPageTables.entrySet()) {
                Integer pn = entry.getValue().findPageInFrame(victimFrameId);
                if (pn != null) {
                    victimProcessId = entry.getKey();
                    victimPageTable = entry.getValue();
                    victimPageNumber = pn;
                    break;
                }
            }
            if (victimProcessId == null || victimPageTable == null || victimPageNumber == null) {
                System.out.println("ERROR: No se encontró la página víctima en tablas de páginas");
                return false;
            }
            System.out.println("REEMPLAZO: Expulsando " + victimProcessId + "-Page" + victimPageNumber + 
                          " del Frame " + victimFrameId);
        
            // Actualizar tabla de páginas de la víctima
            victimPageTable.pageUnloaded(victimPageNumber);
            
            // Notificar al algoritmo
            replacementAlgorithm.onPageUnloaded(victimProcessId, victimPageNumber, victimFrameId);
            
            // Liberar el frame
            targetFrame.free();
            
            // Incrementar contador de reemplazos
            replacementCount.put(processId, replacementCount.getOrDefault(processId, 0) + 1);
            
        } else {
            targetFrame = freeFrames.poll();
        }
       
        // cargar la nueva página 
        targetFrame.occupy();
        PageTable pageTable = processPageTables.get(processId);
        pageTable.pageLoaded(pageNumber, targetFrame.getId());

        // Notificar al algoritmo
        replacementAlgorithm.onPageLoaded(processId, pageNumber, targetFrame.getId());
        
        System.out.println("SUCCESS: Página " + pageNumber + " del proceso " + processId + 
                        " cargada en Frame " + targetFrame.getId());
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
        // Buscar en todas las tablas cuál proceso tiene este frame asignado
        for (Map.Entry<String, PageTable> entry : processPageTables.entrySet()) {
            Integer pageNumber = entry.getValue().findPageInFrame(frame.getId());
            if (pageNumber != null) {
                return entry.getKey() + "-Page" + pageNumber;
            }
        }
        return "UNKNOWN-Page";
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

    // Obtener fallos de página de un proceso
    public int getPageFaults(String processId) {
        return pageFaultCount.getOrDefault(processId, 0);
    }

    // Obtener reemplazos de un proceso
    public int getReplacements(String processId) {
        return replacementCount.getOrDefault(processId, 0);
    }

    // Imprimir estadísticas completas
    public void printStatistics() {
        System.out.println("\n=== ESTADÍSTICAS DE MEMORIA ===");
        System.out.println("Algoritmo: " + replacementAlgorithm.getName());
        for (String pid : processPageTables.keySet()) {
            System.out.println(pid + " - Fallos: " + getPageFaults(pid) + 
                              ", Reemplazos: " + getReplacements(pid));
        }
    }

    // ===================== NUEVA API DE ACCESO =====================
    // Esta operación representa un acceso lógico a una página por parte de un proceso.
    // Si la página está cargada: HIT (no hay fallo) y se notifica al algoritmo.
    // Si NO está cargada: PAGE FAULT. Se carga (realizando reemplazo si es necesario)
    // y se devuelve información detallada del evento (victima, frame asignado, etc.).
    public AccessResult accessPage(String processId, int pageNumber) {
        PageTable pageTable = processPageTables.get(processId);
        if (pageTable == null) {
            return AccessResult.error("Proceso inexistente: " + processId);
        }
        if (pageNumber < 0 || pageNumber >= pageTable.getTotalPages()) {
            return AccessResult.error("Número de página fuera de rango: " + pageNumber);
        }

        // HIT
        if (pageTable.isPageLoaded(pageNumber)) {
            int frameId = pageTable.getEntry(pageNumber).getFrameNumber();
            replacementAlgorithm.onPageAccess(processId, pageNumber);
            return AccessResult.hit(frameId);
        }

        // PAGE FAULT
        pageFaultCount.put(processId, pageFaultCount.getOrDefault(processId, 0) + 1);

        Frame targetFrame;
        String victimProcessId = null;
        Integer victimPageNumber = null;
        Integer victimFrameId = null;

        if (freeFrames.isEmpty()) {
            Integer chosenVictimFrameId = replacementAlgorithm.chooseVictimFrame(physicalMemory, processPageTables);
            if (chosenVictimFrameId == null) {
                return AccessResult.error("No se pudo seleccionar frame víctima para reemplazo");
            }
            targetFrame = physicalMemory.get(chosenVictimFrameId);

            // Identificar página víctima
            for (Map.Entry<String, PageTable> entry : processPageTables.entrySet()) {
                Integer pn = entry.getValue().findPageInFrame(chosenVictimFrameId);
                if (pn != null) {
                    victimProcessId = entry.getKey();
                    victimPageNumber = pn;
                    victimFrameId = chosenVictimFrameId;
                    // Actualizar tabla de la víctima
                    entry.getValue().pageUnloaded(pn);
                    replacementAlgorithm.onPageUnloaded(victimProcessId, victimPageNumber, victimFrameId);
                    targetFrame.free();
                    replacementCount.put(processId, replacementCount.getOrDefault(processId, 0) + 1);
                    break;
                }
            }
            if (victimFrameId == null) {
                return AccessResult.error("Frame víctima seleccionado pero no se encontró página asociada");
            }
        } else {
            targetFrame = freeFrames.poll();
        }

        // Cargar nueva página
        targetFrame.occupy();
        pageTable.pageLoaded(pageNumber, targetFrame.getId());
        replacementAlgorithm.onPageLoaded(processId, pageNumber, targetFrame.getId());

        return AccessResult.fault(targetFrame.getId(), victimProcessId, victimPageNumber, victimFrameId);
    }

    // Resultado estructurado de un acceso a página.
    public static class AccessResult {
        public final boolean hit;          // true si fue HIT
        public final boolean fault;        // true si fue PAGE FAULT
        public final Integer frameId;      // Frame que contiene (o ahora contiene) la página
        public final String victimProcess; // Proceso víctima (si hubo reemplazo)
        public final Integer victimPage;   // Página víctima
        public final Integer victimFrame;  // Frame víctima
        public final String errorMessage;  // Mensaje de error si aplica

        private AccessResult(boolean hit, boolean fault, Integer frameId,
                              String victimProcess, Integer victimPage, Integer victimFrame,
                              String errorMessage) {
            this.hit = hit;
            this.fault = fault;
            this.frameId = frameId;
            this.victimProcess = victimProcess;
            this.victimPage = victimPage;
            this.victimFrame = victimFrame;
            this.errorMessage = errorMessage;
        }

        public static AccessResult hit(int frameId) {
            return new AccessResult(true, false, frameId, null, null, null, null);
        }

        public static AccessResult fault(int newFrameId, String vp, Integer vPage, Integer vFrame) {
            return new AccessResult(false, true, newFrameId, vp, vPage, vFrame, null);
        }

        public static AccessResult error(String msg) {
            return new AccessResult(false, false, null, null, null, null, msg);
        }

        @Override
        public String toString() {
            if (errorMessage != null) return "ERROR: " + errorMessage;
            if (hit) return "HIT frame=" + frameId;
            String base = "FAULT frame=" + frameId;
            if (victimFrame != null) {
                base += " replaced=" + victimProcess + ":" + victimPage + " @ " + victimFrame;
            }
            return base;
        }
    }

}