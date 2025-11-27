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
    private IOManager ioManager;
    private volatile boolean shouldStartIO = false;
    
    public ProcessThread(Process process, IOManager ioManager) {
        this.process = process;
        this.ioManager = ioManager;
    }
    
    @Override
    public void run() {
        System.out.println("[ProcessThread] Hilo iniciado para: " + process.getPID());
        
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
            
            executeOneUnit();
            running = false;
        }
        
        System.out.println("[ProcessThread] Hilo terminado para: " + process.getPID());
    }
    
    private void executeOneUnit() {
        synchronized (process) {
            if (process.getState() == ProcessState.TERMINATED) {
                System.out.println("[ProcessThread] " + process.getPID() + " ya terminó");
                burstCompleted = true;
                return;
            }
            
            // CASO 1: Iniciar E/S programada
            if (shouldStartIO) {
                startIOOperation();
                shouldStartIO = false;
                return;
            }
            
            // CASO 2: Verificar si el proceso terminó
            if (process.isFinished()) {
                System.out.println("[ProcessThread] 🏁 " + process.getPID() + " TERMINÓ (no hay más ráfagas)");
                process.setState(ProcessState.TERMINATED);
                burstCompleted = true;
                return;
            }
            
            // CASO 3: Ejecución normal
            Burst currentBurst = process.getBurst();
            if (currentBurst == null) {
                System.out.println("[ProcessThread] ❌ " + process.getPID() + " no tiene ráfaga actual");
                burstCompleted = true;
                return;
            }
            
            System.out.println("[ProcessThread] " + process.getPID() + 
                             " - Ráfaga: " + currentBurst.getResource() + 
                             "(" + currentBurst.getTime_remaining() + "/" + 
                             currentBurst.getTime_total() + "), Estado: " + process.getState());
            
            if (currentBurst.getResource() == BurstResource.CPU) {
                executeCPUUnit(currentBurst);
            } else if (currentBurst.getResource() == BurstResource.IO) {
                System.out.println("[ProcessThread] ⚠️ " + process.getPID() + 
                                 " tiene ráfaga E/S en estado: " + process.getState());
                startIOOperation();
            }
        }
    }
    
    private void executeCPUUnit(Burst currentBurst) {
        process.setState(ProcessState.RUNNING);
        
        // Ejecutar 1 unidad de CPU
        currentBurst.consumirUnidad();
        process.setCpu_usage(process.getCpu_usage() + 1);
        
        System.out.println("[ProcessThread] " + process.getPID() + 
                         " ejecutó CPU. Restante: " + currentBurst.getTime_remaining());
        
        if (currentBurst.isFinished()) {
            System.out.println("[ProcessThread] " + process.getPID() + " COMPLETÓ ráfaga CPU");
            
            // Pasar a la siguiente ráfaga
            process.nextBurst();
            
            // Verificar si el proceso terminó
            if (process.getState() == ProcessState.TERMINATED) {
                System.out.println("[ProcessThread] 🏁 " + process.getPID() + " TERMINÓ completamente");
                burstCompleted = true;
                return;
            }
            
            // Verificar si la siguiente ráfaga es E/S
            Burst nextBurst = process.getBurst();
            if (nextBurst != null && nextBurst.getResource() == BurstResource.IO) {
                System.out.println("[ProcessThread] 🔄 " + process.getPID() + 
                                 " siguiente ráfaga es E/S, programando inicio...");
                shouldStartIO = true;
                burstCompleted = true;
            } else {
                burstCompleted = true;
            }
        } else {
            // No completó, necesita más ciclos
            burstCompleted = false;
        }
    }
    
    private void startIOOperation() {
        // Verificar si el proceso terminó antes de iniciar E/S
        if (process.isFinished() || process.getState() == ProcessState.TERMINATED) {
            System.out.println("[ProcessThread] ❌ " + process.getPID() + 
                             " - Proceso terminó, no se inicia E/S");
            burstCompleted = true;
            return;
        }
        
        Burst ioBurst = process.getBurst();
        if (ioBurst != null && ioBurst.getResource() == BurstResource.IO) {
            System.out.println("[ProcessThread] 🚀 " + process.getPID() + 
                             " INICIANDO E/S - Duración: " + ioBurst.getTime_total());
            
            ioManager.startIOOperation(process, ioBurst.getTime_total(), this);
            burstCompleted = true;
            
            System.out.println("[ProcessThread] ✅ " + process.getPID() + " - E/S iniciada");
        } else {
            System.out.println("[ProcessThread] ❌ " + process.getPID() + 
                             " - No hay ráfaga E/S válida");
            burstCompleted = true;
        }
    }
    
    public void startExecution() {
        synchronized (executionLock) {
            running = true;
            burstCompleted = false;
            executionLock.notify();
        }
    }
    
    public void scheduleIOStart() {
        this.shouldStartIO = true;
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
    
    public boolean isBlockedByIO() {
        return process.getState() == ProcessState.BLOCKED_IO;
    }
    
    public boolean shouldStartIO() {
        return shouldStartIO;
    }
    
    public void terminate() {
        terminated = true;
        running = false;
        synchronized (executionLock) {
            executionLock.notify();
        }
    }
}