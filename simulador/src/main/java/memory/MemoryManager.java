package memory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import memory.algoritmos.ReplacementAlgorithm;
import process.Process;
import synchronization.SyncManager;

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

        for (int i = 0; i < totalFrames; i++) {
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
            System.out.println("[MemoryManager-DEBUG] createProcess INICIO: " + processId + ", páginas: " + totalPages);
            
            PageTable pageTable = new PageTable(processId, totalPages);
            processPageTables.put(processId, pageTable);
            
            System.out.println("[MemoryManager-DEBUG] createProcess COMPLETADO: " + processId);
            System.out.println("[MemoryManager-DEBUG] Procesos después de crear: " + processPageTables.keySet());
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
                pageFaultCount.put(processId, pageFaultCount.getOrDefault(processId, 0) + 1);

                if (freeFrames.isEmpty()) {
                    // REEMPLAZO (evitando expulsar del mismo proceso)
                    Integer victimFrameId = replacementAlgorithm.chooseVictimFrame(physicalMemory, processPageTables, processId);

                    if (victimFrameId == null) {
                        System.out.println("ERROR: No se pudo elegir víctima para reemplazo");
                        return false;
                    }

                    targetFrame = physicalMemory.get(victimFrameId);

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

                    victimPageTable.pageUnloaded(victimPageNumber);
                    replacementAlgorithm.onPageUnloaded(victimProcessId, victimPageNumber, victimFrameId);
                    targetFrame.free();
                    replacementCount.put(processId, replacementCount.getOrDefault(processId, 0) + 1);

                } else {
                    targetFrame = freeFrames.poll();
                }

                targetFrame.occupy();
                PageTable pageTable = processPageTables.get(processId);
                pageTable.pageLoaded(pageNumber, targetFrame.getId());
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
                    System.out.println("Frame " + frame.getId() + ": Ocupado");
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

    public boolean ensurePages(Process process) {
        String pid = process.getPID();
        int totalPages = process.getPages();

        System.out.println("[MemoryManager-DEBUG] ensurePages INICIO para: " + pid);

        // 1. Asegurar tabla de procesos
        boolean exists;
        syncManager.acquireGlobalLock();
        try {
            exists = processPageTables.containsKey(pid);
            System.out.println("[MemoryManager-DEBUG] Proceso " + pid + " existe en tabla: " + exists);
        } finally {
            syncManager.releaseGlobalLock();
        }

        if (!exists) {
            System.out.println("[MemoryManager-DEBUG] Creando proceso: " + pid);
            createProcess(pid, totalPages);
            
            // *** VERIFICAR INMEDIATAMENTE DESPUÉS DE CREAR ***
            syncManager.acquireGlobalLock();
            try {
                boolean existsAfterCreate = processPageTables.containsKey(pid);
                System.out.println("[MemoryManager-DEBUG] Después de createProcess, existe: " + existsAfterCreate);
            } finally {
                syncManager.releaseGlobalLock();
            }
        }

        // 2. Cargar todas las páginas
        System.out.println("[MemoryManager-DEBUG] Llamando loadAllPages para: " + pid);
        boolean result = loadAllPages(pid);
        System.out.println("[MemoryManager-DEBUG] ensurePages RESULTADO para " + pid + ": " + result);
        
        return result;
    }

    public boolean loadAllPages(String processId) {
        System.out.println("[MemoryManager-DEBUG] loadAllPages INICIO para: " + processId);
        
        syncManager.acquireGlobalLock();
        try {
            PageTable pt = processPageTables.get(processId);
            System.out.println("[MemoryManager-DEBUG] PageTable obtenida para " + processId + ": " + (pt != null ? "NO-NULL" : "NULL"));
            
            if (pt == null){
                System.out.println("[MemoryManager-ERROR] PageTable es NULL para: " + processId);
                System.out.println("[MemoryManager-ERROR] Procesos en tabla: " + processPageTables.keySet());
                return false;
            }

            int totalPages = pt.getTotalPages();
            System.out.println("[MemoryManager-DEBUG] Total páginas a cargar: " + totalPages);

            // Intentar cargar todas las páginas
            for (int i = 0; i < totalPages; i++) {
                if (!isPageLoaded(processId, i)) {
                    System.out.println("[MemoryManager-DEBUG] Cargando página " + i + " para " + processId);
                    boolean success = loadPage(processId, i);
                    if (!success) {
                        System.out.println("[MemoryManager-ERROR] Falló carga de página " + i + " para " + processId);
                        return false;
                    }
                } else {
                    System.out.println("[MemoryManager-DEBUG] Página " + i + " ya cargada para " + processId);
                }
            }

            System.out.println("[MemoryManager-DEBUG] loadAllPages ÉXITO para: " + processId);
            return true;
        } finally {
            syncManager.releaseGlobalLock();
        }
    }
    // libera la memoria de un proceso cuando termina
    public void releaseProcessMemory(String processId) {
        syncManager.acquireGlobalLock();
        try {
            PageTable pt = processPageTables.get(processId);
            if (pt == null)
                return;

            int totalPages = pt.getTotalPages();

            // Liberar todas las páginas
            for (int i = 0; i < totalPages; i++) {
                if (pt.isPageLoaded(i)) {
                    int frameId = pt.getEntry(i).getFrameNumber();
                    Frame frame = physicalMemory.get(frameId);

                    // Notificar al algoritmo
                    replacementAlgorithm.onPageUnloaded(processId, i, frameId);

                    // Liberar frame
                    frame.free();
                    freeFrames.add(frame);

                    // Actualizar tabla de páginas
                    pt.pageUnloaded(i);
                }
            }

            System.out.println("Memoria liberada para proceso " + processId);
        } finally {
            syncManager.releaseGlobalLock();
        }
    }

    public boolean canLoadAllPages(String processId) {
        syncManager.acquireGlobalLock();
        try {
            PageTable pt = processPageTables.get(processId);
            if (pt == null)
                return false;

            int needed = pt.getTotalPages();
            int available = freeFrames.size();

            return available >= needed;
        } finally {
            syncManager.releaseGlobalLock();
        }
    }

}