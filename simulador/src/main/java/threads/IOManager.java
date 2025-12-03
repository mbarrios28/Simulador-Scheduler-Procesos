package threads;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import memory.MemoryManager;
import process.Process;
import process.ProcessState;
import scheduler.Scheduler;
import synchronization.SyncManager;

public class IOManager {
    private final Map<String, IOCounter> ioCounters;
    private Scheduler scheduler;
    private SyncManager syncManager;
    private MemoryManager memoryManager;
    
    private class IOCounter {
    int totalCycles;           
    int startConsumeCycle;     
    int availableAtCycle;      
    ProcessThread thread;
    Process process;
    String operationType;      
    Integer pageNumber;        
    MemoryManager memoryManager;
    
    IOCounter(int duration, int currentCycle, ProcessThread thread, 
              Process process, String operationType, Integer pageNumber, 
              MemoryManager memoryManager) {
            this.totalCycles = duration;
            this.startConsumeCycle = currentCycle;  // CAMBIADO: SIN +1
            this.availableAtCycle = this.startConsumeCycle + duration;  // CAMBIADO
            this.thread = thread;
            this.process = process;
            this.operationType = operationType;
            this.pageNumber = pageNumber;
            this.memoryManager = memoryManager;
            
            System.out.println("[IOManager-TIMING] " + process.getPID() + 
                " - Operación: " + operationType +
                ", Creado en: T=" + currentCycle +
                ", Consume desde: T=" + startConsumeCycle +  // CAMBIADO
                ", Duración: " + duration +
                ", Disponible: T=" + availableAtCycle);  // CAMBIADO
        }
        
        int getConsumedCycles(int currentCycle) {
            if (currentCycle < startConsumeCycle) {
                return 0;  
            }
            int cycles = currentCycle - startConsumeCycle;
            return Math.min(totalCycles, cycles);
        }
        
        int getRemainingCycles(int currentCycle) {
            if (currentCycle < startConsumeCycle) {
                return totalCycles;  
            }
            return Math.max(0, availableAtCycle - currentCycle);
        }
        
        float getProgress(int currentCycle) {
            if (totalCycles == 0) return 1.0f;
            int consumed = getConsumedCycles(currentCycle);
            return Math.min(1.0f, (float) consumed / totalCycles);
        }
        
        boolean shouldComplete(int currentCycle) {
            return currentCycle >= availableAtCycle;  // CAMBIADO: >= en lugar de >
        }
        
        String getStatus(int currentCycle) {
            if (currentCycle < startConsumeCycle) {
                return "ESPERANDO (inicia en T=" + startConsumeCycle + ")";
            } else if (currentCycle >= availableAtCycle) {
                return "COMPLETADO (listo desde T=" + availableAtCycle + ")";
            } else {
                int consumed = getConsumedCycles(currentCycle);
                int remaining = getRemainingCycles(currentCycle);
                float progress = getProgress(currentCycle) * 100;
                return String.format("EN PROGRESO (%d/%d ciclos, %.1f%%, restan: %d)", 
                    consumed, totalCycles, progress, remaining);
            }
        }
    }
    
    public IOManager(Scheduler scheduler) {
        this.scheduler = scheduler;
        this.syncManager = SyncManager.getInstance();
        this.ioCounters = new HashMap<>();
        System.out.println("[IOManager] IOManager inicializado");
    }
    
    public void setMemoryManager(MemoryManager memoryManager) {
        this.memoryManager = memoryManager;
        System.out.println("[IOManager] MemoryManager configurado");
    }
    
    private int getCurrentCycle() {
        return scheduler.getCurrentCycle();
    }
    
    public void startIOOperation(Process process, int duration, ProcessThread thread) {
        String pid = process.getPID();
        int currentCycle = getCurrentCycle();
        
        syncManager.acquireProcessLock(pid);
        try {
            if (ioCounters.containsKey(pid + "_IO")) {
                System.out.println("[IOManager-WARN] " + pid + " ya está en E/S");
                return;
            }
            
            System.out.println("[IOManager] INICIANDO E/S para " + pid + 
                " - Creado en: T=" + currentCycle +
                ", Consume desde: T=" + currentCycle +  // CAMBIADO: SIN +1
                ", Duración: " + duration +
                ", Disponible: T=" + (currentCycle + duration));  // CAMBIADO: SIN +1
            
            IOCounter counter = new IOCounter(duration, currentCycle, thread, process, 
                                            "IO", null, null);
            
            ioCounters.put(pid + "_IO", counter);
            process.setState(ProcessState.BLOCKED_IO);
            
        } finally {
            syncManager.releaseProcessLock(pid);
        }
    }
    
    public void startPageFault(Process process, int pageNumber, MemoryManager memory, ProcessThread thread) {
        String pid = process.getPID();
        int currentCycle = getCurrentCycle();
        
        MemoryManager mm = (this.memoryManager != null) ? this.memoryManager : memory;
        if (mm == null) {
            System.err.println("[IOManager-ERROR] No hay MemoryManager para fallo de página");
            return;
        }
        
        int faultDuration = 1;
        
        syncManager.acquireProcessLock(pid);
        try {
            System.out.println("[IOManager-PF] PAGE FAULT: " + pid + 
                " - Página: " + pageNumber +
                ", Creado en: T=" + currentCycle +
                ", Consume desde: T=" + currentCycle +  // CAMBIADO: SIN +1
                ", Disponible: T=" + (currentCycle + faultDuration));  // CAMBIADO: SIN +1
            
            IOCounter counter = new IOCounter(faultDuration, currentCycle, thread, process,
                            "PAGE_FAULT", pageNumber, mm);
            
            ioCounters.put(pid + "_PF", counter);
            
            try {
                process.setState(ProcessState.valueOf("BLOCKED_MEM"));
            } catch (Exception e) {
                process.setState(ProcessState.BLOCKED_IO);
            }
            
        } finally {
            syncManager.releaseProcessLock(pid);
        }
    }
    
    public void startFullLoadFault(Process process, MemoryManager memory, ProcessThread thread) {
        String pid = process.getPID();
        int currentCycle = getCurrentCycle();
        
        MemoryManager mm = (this.memoryManager != null) ? this.memoryManager : memory;
        if (mm == null) {
            System.err.println("[IOManager-ERROR] No hay MemoryManager para carga completa");
            return;
        }
        
        int loadDuration = Math.max(1, process.getPages()); // 1 ciclo por página
        
        syncManager.acquireProcessLock(pid);
        try {
            System.out.println("[IOManager-MEM] FULL LOAD: " + pid + 
                " - Páginas: " + process.getPages() +
                ", Creado en: T=" + currentCycle +
                ", Consume desde: T=" + currentCycle +  // CAMBIADO: SIN +1
                ", Disponible: T=" + (currentCycle + loadDuration));  // CAMBIADO: SIN +1
            
            IOCounter counter = new IOCounter(loadDuration, currentCycle, thread, process,
                            "FULL_LOAD", null, mm);
            
            ioCounters.put(pid + "_FULL", counter);
            
            try {
                process.setState(ProcessState.valueOf("BLOCKED_MEM"));
            } catch (Exception e) {
                process.setState(ProcessState.BLOCKED_IO);
            }
            
        } finally {
            syncManager.releaseProcessLock(pid);
        }
    }
    
    public void processCompletedIO() {
        if (ioCounters.isEmpty()) {
            return;
        }
        
        int currentCycle = getCurrentCycle();
        List<String> completed = new ArrayList<>();
        
        System.out.println("[IOManager-CHECK] T=" + currentCycle + " - Verificando " + ioCounters.size() + " operaciones:");
        
        for (Map.Entry<String, IOCounter> entry : ioCounters.entrySet()) {
            String operationId = entry.getKey();
            IOCounter counter = entry.getValue();
            
            String status = counter.getStatus(currentCycle);
            System.out.println("[IOManager-STATUS] " + operationId + " - " + status);
            
            if (counter.shouldComplete(currentCycle)) {
                completed.add(operationId);
                System.out.println("[IOManager] ¡" + counter.operationType + 
                    " DISPONIBLE para " + counter.process.getPID() + 
                    " en T=" + currentCycle + "!");
            }
        }
        
        for (String operationId : completed) {
            IOCounter counter = ioCounters.remove(operationId);
            if (counter != null) {
                completeOperation(counter);
            }
        }
    }
    
    private void completeOperation(IOCounter counter) {
        String pid = counter.process.getPID();
        int currentCycle = getCurrentCycle();
        
        System.out.println("\n[IOManager] === " + counter.operationType + " COMPLETADO para " + pid + " ===");
        System.out.println("[IOManager-TIMING] Creado: T=" + counter.startConsumeCycle +
            ", Consumió: T=" + counter.startConsumeCycle + " a T=" + (counter.availableAtCycle - 1) +
            ", Disponible: T=" + currentCycle);
        
        syncManager.acquireProcessLock(pid);
        try {
            switch (counter.operationType) {
                case "IO":
                    completeIO(counter);
                    break;
                case "PAGE_FAULT":
                    completePageFault(counter);
                    break;
                case "FULL_LOAD":
                    completeFullLoad(counter);
                    break;
                default:
                    System.err.println("[IOManager-ERROR] Tipo desconocido: " + counter.operationType);
                    counter.process.setState(ProcessState.READY);
                    if (counter.thread != null) {
                        scheduler.addProcessThread(counter.thread);
                    }
            }
            
        } catch (Exception e) {
            System.err.println("[IOManager-ERROR] Error completando operación: " + e.getMessage());
            e.printStackTrace();
        } finally {
            syncManager.releaseProcessLock(pid);
        }
    }
    
    private void completeIO(IOCounter counter) {
        Process p = counter.process;
        
        System.out.println("[IOManager-IO] Avanzando a siguiente ráfaga de " + p.getPID());
        
        p.nextBurst();
        
        if (p.getState() == ProcessState.TERMINATED) {
            System.out.println("[IOManager-IO] " + p.getPID() + " TERMINÓ completamente");
        } else {
            p.setState(ProcessState.READY);
            System.out.println("[IOManager-IO] " + p.getPID() + " reactivado a READY");
            
            if (counter.thread != null) {
                scheduler.addProcessThread(counter.thread);
                System.out.println("[IOManager-IO] " + p.getPID() + " añadido al scheduler");
            }
        }
    }
    
    private void completePageFault(IOCounter counter) {
        Process p = counter.process;
        MemoryManager mm = counter.memoryManager;
        
        if (mm != null && counter.pageNumber != null) {
            System.out.println("[IOManager-PF] Cargando página " + counter.pageNumber + " para " + p.getPID());
            
            boolean success = mm.loadPage(p.getPID(), counter.pageNumber);
            
            if (success) {
                System.out.println("[IOManager-PF] Página cargada exitosamente");
                p.setState(ProcessState.READY);
                
                if (counter.thread != null) {
                    scheduler.addProcessThread(counter.thread);
                    System.out.println("[IOManager-PF] " + p.getPID() + " reactivado");
                }
            } else {
                System.err.println("[IOManager-PF-ERROR] Fallo al cargar página");
                p.setState(ProcessState.TERMINATED);
            }
        } else {
            System.err.println("[IOManager-PF-ERROR] Datos incompletos para " + p.getPID());
            p.setState(ProcessState.READY);
            if (counter.thread != null) {
                scheduler.addProcessThread(counter.thread);
            }
        }
    }
    
    private void completeFullLoad(IOCounter counter) {
        Process p = counter.process;
        MemoryManager mm = counter.memoryManager;
        
        if (mm != null) {
            System.out.println("[IOManager-MEM] Cargando " + p.getPages() + " páginas de " + p.getPID());
            
            boolean success = mm.ensurePages(p);
            
            if (success) {
                System.out.println("[IOManager-MEM] Carga completa exitosa");
                p.setState(ProcessState.READY);
                
                if (counter.thread != null) {
                    scheduler.addProcessThread(counter.thread);
                    System.out.println("[IOManager-MEM] " + p.getPID() + " reactivado");
                }
            } else {
                System.err.println("[IOManager-MEM-ERROR] Fallo en carga completa");
                p.setState(ProcessState.TERMINATED);
            }
        } else {
            System.err.println("[IOManager-MEM-ERROR] No hay MemoryManager para " + p.getPID());
            p.setState(ProcessState.READY);
            if (counter.thread != null) {
                scheduler.addProcessThread(counter.thread);
            }
        }
    }
    
    public int getActiveIOOperations() {
        return ioCounters.size();
    }
    
    public boolean hasActiveIO() {
        return !ioCounters.isEmpty();
    }
    
    public void printActiveOperations() {
        if (ioCounters.isEmpty()) {
            System.out.println("[IOManager] No hay operaciones activas");
            return;
        }
        
        int currentCycle = getCurrentCycle();
        System.out.println("\n[IOManager] Operaciones activas (T=" + currentCycle + "):");
        
        for (Map.Entry<String, IOCounter> entry : ioCounters.entrySet()) {
            IOCounter counter = entry.getValue();
            String status = counter.getStatus(currentCycle);
            System.out.println("  " + entry.getKey() + ": " + status);
        }
    }
    
    public void shutdown() {
        ioCounters.clear();
        System.out.println("[IOManager] Apagado");
    }
}