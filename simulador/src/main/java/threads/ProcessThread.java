package threads;

import process.Burst;
import process.BurstResource;
import process.Process;
import process.ProcessState;
import synchronization.SyncManager;

public class ProcessThread extends Thread {
    private Process process;
    private volatile boolean running = false;
    private volatile boolean terminated = false;
    private final Object executionLock = new Object();
    private volatile boolean burstCompleted = false;
    private IOManager ioManager;
    private volatile boolean shouldStartIO = false;
    private SyncManager syncManager;
    
    public ProcessThread(Process process, IOManager ioManager) {
        this.process = process;
        this.ioManager = ioManager;
        this.syncManager = SyncManager.getInstance();
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
        syncManager.cleanupProcess(process.getPID());
    }
    
    private void executeOneUnit() {
        syncManager.acquireProcessLock(process.getPID());
        try {
            if (process.getState() == ProcessState.BLOCKED_IO || 
                process.getState() == ProcessState.BLOCKED_MEM) {
                System.out.println("[ProcessThread] " + process.getPID() + 
                    " está bloqueado (" + process.getState() + "), no ejecuta");
                burstCompleted = true;
                return;
            }
            
            if (process.getState() == ProcessState.TERMINATED) {
                System.out.println("[ProcessThread] " + process.getPID() + " ya terminó");
                burstCompleted = true;
                return;
            }
            
            if (process.isFinished()) {
                System.out.println("[ProcessThread]  " + process.getPID() + " TERMINÓ");
                process.setState(ProcessState.TERMINATED);
                burstCompleted = true;
                return;
            }
            
            Burst currentBurst = process.getBurst();
            if (currentBurst == null) {
                System.out.println("[ProcessThread]  " + process.getPID() + " no tiene ráfaga actual");
                burstCompleted = true;
                return;
            }
            
            System.out.println("[ProcessThread] " + process.getPID() + 
                " - Ráfaga: " + currentBurst.getResource() + 
                "(" + currentBurst.getTime_remaining() + "/" + 
                currentBurst.getTime_total() + "), Estado: " + process.getState());
            
            if (currentBurst.getResource() == BurstResource.CPU) {
                if (process.getState() == ProcessState.READY || 
                    process.getState() == ProcessState.RUNNING) {
                    executeCPUUnit(currentBurst);
                } else {
                    System.out.println("[ProcessThread] " + process.getPID() + 
                                    " no puede ejecutar CPU en estado: " + process.getState());
                    burstCompleted = true;
                }
            } else if (currentBurst.getResource() == BurstResource.IO) {
                System.out.println("[ProcessThread-WARN] " + process.getPID() + 
                                " tiene ráfaga IO en executeOneUnit() - Estado incorrecto");
                burstCompleted = true;
            }
        } finally {
            syncManager.releaseProcessLock(process.getPID());
        }
    }
    
    private void executeCPUUnit(Burst currentBurst) {
        process.setState(ProcessState.RUNNING);
        
        currentBurst.consumirUnidad();
        process.setCpu_usage(process.getCpu_usage() + 1);
        
        System.out.println("[ProcessThread] " + process.getPID() + 
                        " ejecutó CPU. Restante: " + currentBurst.getTime_remaining());
        
        if (currentBurst.isFinished()) {
            System.out.println("[ProcessThread] " + process.getPID() + " COMPLETÓ ráfaga CPU");
            boolean wasLastBurst = (process.getInd_burst() == process.getBursts().size() - 1);
            
            process.nextBurst();
            burstCompleted = true;
            
            System.out.println("[ProcessThread-DEBUG] " + process.getPID() + 
                " - ¿Era última ráfaga?: " + wasLastBurst +
                ", Estado después: " + process.getState());
            
        } else {
            burstCompleted = false;
        }
    }
    
    private void startIOOperation() {
        if (process.isFinished() || process.getState() == ProcessState.TERMINATED) {
            System.out.println("[ProcessThread]  " + process.getPID() + 
                            " - Proceso terminó, no se inicia E/S");
            burstCompleted = true;
            return;
        }
        
        Burst ioBurst = process.getBurst();
        if (ioBurst != null && ioBurst.getResource() == BurstResource.IO) {
            System.out.println("[ProcessThread]  " + process.getPID() + 
                            " INICIANDO E/S - Duración: " + ioBurst.getTime_total() + " unidades");
            
            process.setState(ProcessState.BLOCKED_IO);
            
            ioManager.startIOOperation(process, ioBurst.getTime_total(), this);
            burstCompleted = true;
            
            System.out.println("[ProcessThread]  " + process.getPID() + 
                            " - E/S iniciada, estado: BLOCKED_IO por " + 
                            ioBurst.getTime_total() + " ciclos");
        } else {
            System.out.println("[ProcessThread] " + process.getPID() + 
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
    
    public void terminate() {
        terminated = true;
        running = false;
        synchronized (executionLock) {
            executionLock.notify();
        }
    }
}