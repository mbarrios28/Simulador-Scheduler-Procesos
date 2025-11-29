package scheduler;

import process.Process;
import process.ProcessState;
import threads.ProcessThread;
import threads.IOManager;
import synchronization.SyncManager;
import java.util.LinkedList;
import java.util.Collections;
import java.util.List;

public class Scheduler {
    public enum Algorithm { FCFS, SJF, RR }

    private int tiempoGlobal = 0;
    private final List<ProcessThread> readyQueue; 
    private ProcessThread currentThread = null;
    private IOManager ioManager;
    private final SyncManager syncManager; // final para asegurar inicialización

    // Configuración
    private Algorithm currentAlgorithm = Algorithm.FCFS;
    private int quantum = 2;
    private int currentQuantumUsed = 0;

    public Scheduler() {
        this.readyQueue = Collections.synchronizedList(new LinkedList<>());
        this.syncManager = SyncManager.getInstance(); // Inicialización aquí
        this.ioManager = new IOManager(this);
        System.out.println("[Scheduler] Scheduler inicializado con SyncManager");
    }

    public void addProcess(Process p) {
        syncManager.acquireGlobalLock();
        try {
            ProcessThread thread = new ProcessThread(p, this.ioManager);
            thread.start(); 
            addProcessThread(thread);
        } finally {
            syncManager.releaseGlobalLock();
        }
    }

    public void addProcessThread(ProcessThread thread) {
        syncManager.acquireGlobalLock();
        try {
            Process p = thread.getProcess();
            p.setState(ProcessState.READY);
            
            synchronized (readyQueue) {
                this.readyQueue.add(thread);
            }
            
            System.out.println("[T=" + tiempoGlobal + "] " + p.getPID() + " en READY.");
            
        } finally {
            syncManager.releaseGlobalLock();
        }
    }

    private void dispatch() {
        syncManager.acquireGlobalLock();
        try {
            if (currentThread == null && !readyQueue.isEmpty()) {
                ProcessThread selectedThread = null;
                
                synchronized (readyQueue) {
                    if (readyQueue.isEmpty()) return;

                    switch (currentAlgorithm) {
                        case FCFS:
                        case RR:
                            selectedThread = readyQueue.remove(0);
                            break;
                        case SJF:
                            int minTime = Integer.MAX_VALUE;
                            int selectedIndex = -1;
                            for (int i = 0; i < readyQueue.size(); i++) {
                                ProcessThread thread = readyQueue.get(i);
                                syncManager.acquireProcessLock(thread.getProcess().getPID());
                                try {
                                    int remaining = thread.getProcess().getBurst().getTime_remaining();
                                    if (remaining < minTime) {
                                        minTime = remaining;
                                        selectedThread = thread;
                                        selectedIndex = i;
                                    }
                                } finally {
                                    syncManager.releaseProcessLock(thread.getProcess().getPID());
                                }
                            }
                            if (selectedThread != null) readyQueue.remove(selectedIndex);
                            break;
                    }
                }

                if (selectedThread != null) {
                    currentThread = selectedThread;
                    Process p = currentThread.getProcess();
                    
                    syncManager.acquireProcessLock(p.getPID());
                    try {
                        if (p.getT_start() == -1) p.setT_start(tiempoGlobal);
                        p.setState(ProcessState.RUNNING);
                    } finally {
                        syncManager.releaseProcessLock(p.getPID());
                    }
                    
                    currentQuantumUsed = 0;
                    System.out.println("[T=" + tiempoGlobal + "] DISPATCH (" + currentAlgorithm + "): " + 
                                       p.getPID() + " -> RUNNING");
                }
            }
        } finally {
            syncManager.releaseGlobalLock();
        }
    }

    public boolean runOneUnit() {
        syncManager.acquireGlobalLock();
        try {
            System.out.println("\n--- CICLO T=" + tiempoGlobal + " ---");
            
            // 1. Intentar poner un proceso en CPU
            dispatch();

            // 2. Gestionar el proceso actual
            if (currentThread != null) {
                Process p = currentThread.getProcess();
                
                syncManager.acquireProcessLock(p.getPID());
                try {
                    currentThread.startExecution();
                    
                    // Pequeña pausa para simular ejecución
                    try { Thread.sleep(20); } catch (InterruptedException e) {}

                    // Verificamos el estado reportado por el hilo
                    if (currentThread.isBurstCompleted()) {
                        System.out.println("[T=" + tiempoGlobal + "] " + p.getPID() + " completó ráfaga");
                        
                        if (currentThread.isProcessTerminated()) {
                            p.setT_finish(tiempoGlobal);
                            System.out.println(">>> " + p.getPID() + " TERMINADO <<<");
                            currentThread.terminate();
                            syncManager.cleanupProcess(p.getPID());
                        } 
                        else if (currentThread.shouldStartIO()) {
                            currentThread.startExecution();
                            try { Thread.sleep(20); } catch (InterruptedException e) {}
                            
                            System.out.println(">>> " + p.getPID() + " iniciando E/S (bloqueado) <<<");
                        }
                        else {
                            // Terminó ráfaga CPU, vuelve a cola READY
                            p.setState(ProcessState.READY);
                            addProcessThread(currentThread); 
                        }
                        currentThread = null;
                    } 
                    else {
                        // Ráfaga en progreso - verificar Quantum para RR
                        if (currentAlgorithm == Algorithm.RR) {
                            currentQuantumUsed++;
                            if (currentQuantumUsed >= quantum) {
                                System.out.println("[T=" + tiempoGlobal + "] RR QUANTUM: " + p.getPID() + " desalojado.");
                                
                                p.setState(ProcessState.READY);
                                addProcessThread(currentThread);
                                currentThread = null;
                                currentQuantumUsed = 0;
                            } else {
                                System.out.println("[T=" + tiempoGlobal + "] RR: " + p.getPID() + " continua (Q " + currentQuantumUsed + ")");
                            }
                        } else {
                            System.out.println("[T=" + tiempoGlobal + "] EXEC: " + p.getPID() + " continua.");
                        }
                    }
                } finally {
                    syncManager.releaseProcessLock(p.getPID());
                }
            } else {
                System.out.println("[T=" + tiempoGlobal + "] IDLE (CPU inactiva)");
            }
            
            // 3. Métricas de espera
            synchronized (readyQueue) {
                for (ProcessThread thread : readyQueue) {
                    syncManager.acquireProcessLock(thread.getProcess().getPID());
                    try {
                        thread.getProcess().setT_wait(thread.getProcess().getT_wait() + 1);
                    } finally {
                        syncManager.releaseProcessLock(thread.getProcess().getPID());
                    }
                }
            }
            
            // 4. Reporte de E/S
            System.out.println("[T=" + tiempoGlobal + "] E/S activas: " + ioManager.getActiveIOOperations());
            
            tiempoGlobal++;
            
            // Condición de parada
            return !readyQueue.isEmpty() || currentThread != null || ioManager.hasActiveIO();
            
        } finally {
            syncManager.releaseGlobalLock();
        }
    }
    
    public IOManager getIOManager() { return ioManager; }
    public SyncManager getSyncManager() { return syncManager; }
    
    public void shutdown() { 
        if (ioManager != null) {
            ioManager.shutdown(); 
        }
        syncManager.signalAllProcesses();
    }
    
    public int getTiempoGlobal() { return tiempoGlobal; }
    
    // Métodos para configuración
    public void setAlgorithm(Algorithm algo) { 
        this.currentAlgorithm = algo; 
        System.out.println("[Scheduler] Algoritmo cambiado a: " + algo);
    }
    
    public void setQuantum(int q) { 
        this.quantum = q; 
        System.out.println("[Scheduler] Quantum establecido a: " + q);
    }
}