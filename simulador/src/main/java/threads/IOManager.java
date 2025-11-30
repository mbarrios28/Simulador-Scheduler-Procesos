package threads;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import memory.MemoryManager;
import process.Process;
import process.ProcessState;
import scheduler.Scheduler;
import synchronization.SyncManager;

public class IOManager {
    private final ScheduledExecutorService ioExecutor;
    private final ConcurrentHashMap<String, ScheduledFuture<?>> ioOperations;
    private Scheduler scheduler;
    private SyncManager syncManager;
    
    public IOManager(Scheduler scheduler) {
        this.scheduler = scheduler;
        this.syncManager = SyncManager.getInstance();
        this.ioExecutor = Executors.newScheduledThreadPool(4); 
        this.ioOperations = new ConcurrentHashMap<>();
        System.out.println("[IOManager] IOManager inicializado");
    }
    
    // --- E/S REGULAR ---
    public void startIOOperation(Process process, int duration, ProcessThread thread) {
        String pid = process.getPID();
        
        syncManager.acquireProcessLock(pid);
        try {
            System.out.println("[IOManager] INICIANDO E/S para " + pid + 
                               " - Duración: " + duration + " unidades");
            
            ScheduledFuture<?> future = ioExecutor.schedule(() -> {
                System.out.println("[IOManager] Timer E/S expirado para " + pid);
                completeIOOperation(process, thread);
            }, duration * 1000L, TimeUnit.MILLISECONDS);
            
            ioOperations.put(pid, future);
            process.setState(ProcessState.BLOCKED_IO);
            
            System.out.println("[IOManager] E/S activas: " + ioOperations.size());
        } finally {
            syncManager.releaseProcessLock(pid);
        }
    }
    
    private void completeIOOperation(Process process, ProcessThread thread) {
        String pid = process.getPID();
        System.out.println("[IOManager] E/S COMPLETADA para " + pid);
        
        syncManager.acquireProcessLock(pid);
        try {
            System.out.println("[IOManager] Avanzando a siguiente ráfaga de " + pid);
            process.nextBurst();
            
            if (process.getState() == ProcessState.TERMINATED) {
                System.out.println("[IOManager] " + pid + " TERMINÓ después de E/S");
            } else {
                process.setState(ProcessState.READY);
                System.out.println("[IOManager] " + pid + " reactivado a READY");
                scheduler.addProcessThread(thread);
            }
        } finally {
            syncManager.releaseProcessLock(pid);
        }
        
        ioOperations.remove(pid);
        System.out.println("[IOManager] E/S activas restantes: " + ioOperations.size());
    }

    // --- NUEVO: GESTIÓN DE FALLOS DE PÁGINA (PAGE FAULT) ---
    public void startPageFault(Process process, int pageNumber, MemoryManager memory, ProcessThread thread) {
        String pid = process.getPID();
        int delay = 0; 
        
        syncManager.acquireProcessLock(pid);
        try {
            System.out.println("[IOManager-Swap] PAGE FAULT: Recuperando Pagina " + pageNumber + " para " + pid);
            
            ScheduledFuture<?> future = ioExecutor.schedule(() -> {
                boolean success = memory.loadPage(pid, pageNumber);
                
                if (success) {
                    System.out.println("[IOManager-Swap] Pagina cargada. Desbloqueando " + pid);
                    
                    syncManager.acquireProcessLock(pid);
                    try {
                        process.setState(ProcessState.READY);
                        scheduler.addProcessThread(thread);
                    } finally {
                        syncManager.releaseProcessLock(pid);
                    }
                } else {
                    System.err.println("[IOManager] Error crítico de memoria para " + pid);
                    process.setState(ProcessState.TERMINATED);
                }
                ioOperations.remove(pid + "_PF");
                
            }, delay * 1000L, TimeUnit.MILLISECONDS);
            
            ioOperations.put(pid + "_PF", future);
            
            try { process.setState(ProcessState.valueOf("BLOCKED_MEM")); } 
            catch (Exception e) { process.setState(ProcessState.BLOCKED_IO); }
            
        } finally {
            syncManager.releaseProcessLock(pid);
        }
    }
    
    public int getActiveIOOperations() { return ioOperations.size(); }
    public boolean hasActiveIO() { return !ioOperations.isEmpty(); }
    
    public void shutdown() {
        ioExecutor.shutdownNow();
        System.out.println("[IOManager] Apagado");
    }
    // --- NUEVO: FALLO DE MEMORIA CON CARGA TOTAL ---
    public void startFullLoadFault(Process process, MemoryManager memory, ProcessThread thread) {
        String pid = process.getPID();

        // Tiempo total simulado para cargar todas las páginas
        // Puedes cambiarlo: por ejemplo = process.getPages()
        int delay = 0; // 1 segundo para simular carga completa

        syncManager.acquireProcessLock(pid);
        try {
            System.out.println("[IOManager-MEM] FULL LOAD FAULT: Cargando TODAS las páginas de " + pid);

            // Programar la "carga"
            ScheduledFuture<?> future = ioExecutor.schedule(() -> {

                boolean loaded = memory.ensurePages(process);

                syncManager.acquireProcessLock(pid);
                try {
                    if (loaded) {
                        System.out.println("[IOManager-MEM] Carga completa finalizada para " + pid);
                        process.setState(ProcessState.READY);
                        scheduler.addProcessThread(thread);
                    } else {
                        System.err.println("[IOManager-MEM] Error crítico: no se pudo cargar la memoria de " + pid);
                        process.setState(ProcessState.TERMINATED);
                    }
                } finally {
                    syncManager.releaseProcessLock(pid);
                }

                ioOperations.remove(pid + "_FULL");

            }, delay * 1000L, TimeUnit.MILLISECONDS);

            ioOperations.put(pid + "_FULL", future);

            // Estado mientras se carga memo completa
            try { process.setState(ProcessState.valueOf("BLOCKED_MEM")); }
            catch (Exception e) { process.setState(ProcessState.BLOCKED_IO); }

        } finally {
            syncManager.releaseProcessLock(pid);
        }
    }

}