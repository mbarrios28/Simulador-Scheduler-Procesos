package threads;

import process.Process;
import process.ProcessState;
import process.Burst;
import process.BurstResource;

public class ProcessThread extends Thread {
    private Process process;
    private volatile boolean running = false;
    private volatile boolean terminated = false;
    private final Object executionLock = new Object();
    private volatile boolean burstCompleted = false;
    
    public ProcessThread(Process process) {
        this.process = process;
    }
    
    @Override
    public void run() {
        System.out.println("[ProcessThread] Hilo iniciado para proceso: " + process.getPID());
        
        while (!terminated) {
            synchronized (executionLock) {
                while (!running && !terminated) {
                    try {
                        executionLock.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
            
            if (terminated) break;
            
            // Ejecutar UNA unidad de CPU
            executeOneUnit();
            
            // Pausar inmediatamente después de una unidad
            running = false;
        }
        
        System.out.println("[ProcessThread] Hilo terminado para proceso: " + process.getPID());
    }
    
    /**
     * Ejecuta EXACTAMENTE una unidad de tiempo de CPU
     */
    private void executeOneUnit() {
        synchronized (process) {
            if (process.getState() == ProcessState.TERMINATED) {
                return;
            }
            
            process.setState(ProcessState.RUNNING);
            Burst currentBurst = process.getBurst();
            
            if (currentBurst != null && currentBurst.getResource() == BurstResource.CPU) {
                // Consumir UNA unidad
                currentBurst.consumirUnidad();
                process.setCpu_usage(process.getCpu_usage() + 1);
                
                System.out.println("[ProcessThread] " + process.getPID() + 
                                 " ejecutó 1 unidad. Restante: " + 
                                 currentBurst.getTime_remaining());
                
                // Verificar si completó la ráfaga
                if (currentBurst.isFinished()) {
                    System.out.println("[ProcessThread] " + process.getPID() + " COMPLETÓ ráfaga CPU");
                    process.nextBurst();
                    burstCompleted = true;
                    
                    if (process.getState() == ProcessState.TERMINATED) {
                        System.out.println("[ProcessThread] " + process.getPID() + " TERMINÓ completamente");
                    }
                }
            }
        }
    }
    
    /**
     * Inicia la ejecución de UNA unidad
     */
    public void startExecution() {
        synchronized (executionLock) {
            running = true;
            burstCompleted = false;
            executionLock.notify();
        }
    }
    
    public Process getProcess() {
        return process;
    }
    
    public boolean isRunning() {
        return running;
    }
    
    public boolean isBurstCompleted() {
        return burstCompleted;
    }
    
    public boolean isProcessTerminated() {
        return process.getState() == ProcessState.TERMINATED;
    }
    
    public void terminate() {
        terminated = true;
        running = false;
        synchronized (executionLock) {
            executionLock.notify();
        }
    }
}