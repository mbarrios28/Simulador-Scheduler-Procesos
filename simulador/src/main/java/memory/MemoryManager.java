package memory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import memory.algoritmos.ReplacementAlgorithm;
import process.Process;

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

    //Implementación de ensurePages
    public boolean ensurePages(Process process) {
        String pid = process.getPID();
        int totalPages = process.getPages();

        // Registrar el proceso en la tabla de procesos
        if (processPageTables.get(pid) == null) {
            createProcess(pid, totalPages);
        }

        // Ver cuántos marcos libres hay actualmente
        int free = freeFrames.size();

        // Si no hay marcos suficientes, NO cargar nada
        if (free < totalPages) {
            System.out.println("[MEMORY BLOCK] Proceso " + pid +
                            " necesita " + totalPages + 
                            " páginas pero solo hay " + free + " marcos.");
            return false;
        }

        // Caso contrario cargar las páginas
        for (int pageNumber = 0; pageNumber < totalPages; pageNumber++) {
            boolean ok = loadPage(pid, pageNumber);
            if (!ok) return false; // Manejo de excepción por si falla el algoritmo
        }

        return true;
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

}