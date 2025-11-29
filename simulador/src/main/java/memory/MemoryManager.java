package memory;

import memory.algoritmos.ReplacementAlgorithm;
import synchronization.SyncManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class MemoryManager {
    private List<Frame> physicalMemory;
    private Queue<Frame> freeFrames;
    private Map<String, PageTable> processPageTables;
    private ReplacementAlgorithm replacementAlgorithm;
    private SyncManager syncManager;

    // Contadores de estadística
    private Map<String, Integer> pageFaultCount;
    private Map<String, Integer> replacementCount;

    public MemoryManager(int totalFrames, ReplacementAlgorithm algorithm) {
        physicalMemory = new ArrayList<>();
        freeFrames = new java.util.LinkedList<>();
        processPageTables = new java.util.HashMap<>();
        syncManager = SyncManager.getInstance();
        
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

    public void createProcess(String processId, int totalPages) {
        syncManager.acquireGlobalLock();
        try {
            PageTable pageTable = new PageTable(processId, totalPages);
            processPageTables.put(processId, pageTable);
            System.out.println("Proceso " + processId + " creado con " + totalPages + " páginas.");
        } finally {
            syncManager.releaseGlobalLock();
        }
    }

    public boolean isPageLoaded(String processId, int pageNumber) {
        syncManager.acquireProcessLock(processId);
        try {
            PageTable pageTable = processPageTables.get(processId);
            if (pageTable != null) {
                return pageTable.isPageLoaded(pageNumber);
            }
            return false;
        } finally {
            syncManager.releaseProcessLock(processId);
        }
    }

    public boolean loadPage(String processId, int pageNumber) {
        syncManager.acquireGlobalLock();
        try {
            syncManager.acquireProcessLock(processId);
            try {
                // Si ya está cargado solo notificar acceso
                if (isPageLoaded(processId, pageNumber)) {
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
                    String victimProcessId = null;
                    PageTable victimPageTable = null;
                    Integer victimPageNumber = null;
                    
                    for (Map.Entry<String, PageTable> entry : processPageTables.entrySet()) {
                        syncManager.acquireProcessLock(entry.getKey());
                        try {
                            Integer pn = entry.getValue().findPageInFrame(victimFrameId);
                            if (pn != null) {
                                victimProcessId = entry.getKey();
                                victimPageTable = entry.getValue();
                                victimPageNumber = pn;
                                break;
                            }
                        } finally {
                            syncManager.releaseProcessLock(entry.getKey());
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
                
            } finally {
                syncManager.releaseProcessLock(processId);
            }
        } finally {
            syncManager.releaseGlobalLock();
        }
    }

    // Resto de métodos con sincronización similar...
    public PageTable getPageTable(String processId) {
        syncManager.acquireProcessLock(processId);
        try {
            return processPageTables.get(processId);
        } finally {
            syncManager.releaseProcessLock(processId);
        }
    }

    public String findPageInFrame(Frame frame) {
        syncManager.acquireGlobalLock();
        try {
            if (!frame.isOccupied()) {
                return "FREE";
            }
            // Buscar en todas las tablas cuál proceso tiene este frame asignado
            for (Map.Entry<String, PageTable> entry : processPageTables.entrySet()) {
                syncManager.acquireProcessLock(entry.getKey());
                try {
                    Integer pageNumber = entry.getValue().findPageInFrame(frame.getId());
                    if (pageNumber != null) {
                        return entry.getKey() + "-Page" + pageNumber;
                    }
                } finally {
                    syncManager.releaseProcessLock(entry.getKey());
                }
            }
            return "UNKNOWN-Page";
        } finally {
            syncManager.releaseGlobalLock();
        }
    }

    public void printMemoryStatus() {
        syncManager.acquireGlobalLock();
        try {
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
        } finally {
            syncManager.releaseGlobalLock();
        }
    }
    
    public void printAllPageTables() {
        syncManager.acquireGlobalLock();
        try {
            System.out.println("\n=== ALL PAGE TABLES ===");
            for (String processId : processPageTables.keySet()) {
                syncManager.acquireProcessLock(processId);
                try {
                    PageTable pageTable = processPageTables.get(processId);
                    pageTable.printPageTable();
                } finally {
                    syncManager.releaseProcessLock(processId);
                }
            }
        } finally {
            syncManager.releaseGlobalLock();
        }
    }

    public int getPageFaults(String processId) {
        return pageFaultCount.getOrDefault(processId, 0);
    }

    public int getReplacements(String processId) {
        return replacementCount.getOrDefault(processId, 0);
    }

    public void printStatistics() {
        syncManager.acquireGlobalLock();
        try {
            System.out.println("\n=== ESTADÍSTICAS DE MEMORIA ===");
            System.out.println("Algoritmo: " + replacementAlgorithm.getName());
            for (String pid : processPageTables.keySet()) {
                System.out.println(pid + " - Fallos: " + getPageFaults(pid) + 
                                  ", Reemplazos: " + getReplacements(pid));
            }
        } finally {
            syncManager.releaseGlobalLock();
        }
    }
}