package threads;

import process.Process;
import process.ProcessState;
import scheduler.Scheduler;
import synchronization.SyncManager;
import java.util.concurrent.*;

public class IOManager {
    private final ScheduledExecutorService ioExecutor;
    private final ConcurrentHashMap<String, ScheduledFuture<?>> ioOperations;
    private Scheduler scheduler;
    private SyncManager syncManager;
    
    public IOManager(Scheduler scheduler) {
        this.scheduler = scheduler;
        this.syncManager = SyncManager.getInstance();
        this.ioExecutor = Executors.newScheduledThreadPool(2);
        this.ioOperations = new ConcurrentHashMap<>();
        System.out.println("[IOManager]  IOManager inicializado");
    }
    
    public void startIOOperation(Process process, int duration, ProcessThread thread) {
        String pid = process.getPID();
        
        syncManager.acquireProcessLock(pid);
        try {
            System.out.println("[IOManager]  INICIANDO E/S para " + pid + 
                        " - Duración: " + duration + " unidades");
            
            ScheduledFuture<?> future = ioExecutor.schedule(() -> {
                System.out.println("[IOManager]  Timer E/S expirado para " + pid);
                completeIOOperation(process, thread);
            }, duration * 1000L, TimeUnit.MILLISECONDS);
            
            ioOperations.put(pid, future);
            process.setState(ProcessState.BLOCKED_IO);
            
            System.out.println("[IOManager]  E/S activas: " + ioOperations.size());
        } finally {
            syncManager.releaseProcessLock(pid);
        }
    }
    
    private void completeIOOperation(Process process, ProcessThread thread) {
        String pid = process.getPID();
        
        System.out.println("[IOManager]  E/S COMPLETADA para " + pid);
        
        syncManager.acquireProcessLock(pid);
        try {
            // Pasar a la siguiente ráfaga
            System.out.println("[IOManager] Avanzando a siguiente ráfaga de " + pid);
            process.nextBurst();
            
            // Verificar si terminó después de la E/S
            if (process.getState() == ProcessState.TERMINATED) {
                System.out.println("[IOManager]  " + pid + " TERMINÓ después de E/S");
                // No reactivar - el proceso terminó
            } else {
                // Reactivar proceso
                process.setState(ProcessState.READY);
                System.out.println("[IOManager]  " + pid + " reactivado a READY");
                scheduler.addProcessThread(thread);
            }
        } finally {
            syncManager.releaseProcessLock(pid);
        }
        
        ioOperations.remove(pid);
        System.out.println("[IOManager]  E/S activas restantes: " + ioOperations.size());
    }
    
    // Resto de métodos permanecen igual...
    public int getActiveIOOperations() {
        return ioOperations.size();
    }
    
    public boolean hasActiveIO() {
        return !ioOperations.isEmpty();
    }
    
    public void shutdown() {
        System.out.println("[IOManager]  Apagando IOManager...");
        for (String pid : ioOperations.keySet()) {
            ScheduledFuture<?> future = ioOperations.get(pid);
            if (future != null && !future.isDone()) {
                future.cancel(false);
                System.out.println("[IOManager]  E/S cancelada para " + pid);
            }
        }
        ioOperations.clear();
        
        ioExecutor.shutdown();
        try {
            if (!ioExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                ioExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            ioExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        System.out.println("[IOManager]  IOManager apagado");
    }
}