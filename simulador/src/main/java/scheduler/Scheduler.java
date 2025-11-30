package scheduler;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import memory.MemoryManager;
import process.Process;
import process.ProcessState;
import synchronization.SyncManager;
import threads.IOManager;
import threads.ProcessThread;

public class Scheduler {
    public enum Algorithm { FCFS, SJF, RR }

    private int tiempoGlobal = 0;
    private final List<ProcessThread> readyQueue; 
    private ProcessThread currentThread = null;
    private IOManager ioManager;
    private MemoryManager memoryManager; 
    private final SyncManager syncManager;

    private Algorithm currentAlgorithm = Algorithm.FCFS;
    private int quantum = 2;
    private int currentQuantumUsed = 0;

    public Scheduler() {
        this.readyQueue = Collections.synchronizedList(new LinkedList<>());
        this.syncManager = SyncManager.getInstance();
        this.ioManager = new IOManager(this);
        System.out.println("[Scheduler] Scheduler inicializado");
    }

    public void setMemoryManager(MemoryManager mm) {
        this.memoryManager = mm;
    }

    public void setAlgorithm(Algorithm algo) { 
        this.currentAlgorithm = algo; 
        System.out.println("[Scheduler] Algoritmo cambiado a: " + algo);
    }
    public void setQuantum(int q) { this.quantum = q; }

    public void addProcess(Process p) {
        syncManager.acquireGlobalLock();
        try {
            if (memoryManager != null) {
                memoryManager.createProcess(p.getPID(), p.getPages());
            }
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
            // Si ya hay proceso ejecutándose, no despachar otro
            if (currentThread != null) {
                return;
            }

            ProcessThread selectedThread = null;

            // --- 1) Seleccionar y EXTRAER atómicamente ---
            synchronized (readyQueue) {
                if (readyQueue.isEmpty()) {
                    return; // nada para despachar
                }

                switch (currentAlgorithm) {
                    case FCFS:
                    case RR:
                        // Extraer el primer elemento de manera atómica
                        selectedThread = readyQueue.remove(0);
                        break;

                    case SJF:
                        int minTime = Integer.MAX_VALUE;
                        int minIndex = -1;

                        for (int i = 0; i < readyQueue.size(); i++) {
                            ProcessThread t = readyQueue.get(i);
                            int remaining = t.getProcess().getBurst().getTime_remaining();
                            if (remaining < minTime) {
                                minTime = remaining;
                                minIndex = i;
                            }
                        }

                        if (minIndex != -1) {
                            selectedThread = readyQueue.remove(minIndex); // extracción segura
                        }
                        break;
                }
            }

            // Nada seleccionado
            if (selectedThread == null)
                return;

            Process p = selectedThread.getProcess();

            // --- 2) Integración con memoria (Carga Total) ---
            if (memoryManager != null) {
                boolean ok = memoryManager.ensurePages(p);
                if (!ok) {
                    System.out.println("[T=" + tiempoGlobal + "] BLOQUEO MEM: " + p.getPID());

                    // Bloquear proceso por memoria
                    p.setState(ProcessState.BLOCKED_MEM);

                    // REENVIAR AL IOManager para cargar páginas
                    ioManager.startFullLoadFault(p, memoryManager, selectedThread);
                    return;
                }
            }

            // --- 3) Arrancar hilo solo una vez ---
            if (!selectedThread.isAlive()) {
                selectedThread.start();
            }

            // --- 4) Asignar como proceso en CPU ---
            currentThread = selectedThread;
            currentQuantumUsed = 0; // Reset quantum para nuevo proceso

            syncManager.acquireProcessLock(p.getPID());
            try {
                if (p.getT_start() == -1) {
                    p.setT_start(tiempoGlobal);
                }
                p.setState(ProcessState.RUNNING);
            } finally {
                syncManager.releaseProcessLock(p.getPID());
            }

            System.out.println("[T=" + tiempoGlobal + "] DISPATCH (" + currentAlgorithm + "): " + p.getPID());

        } finally {
            syncManager.releaseGlobalLock();
        }
    }


    public boolean runOneUnit() {
        System.out.println("\n--- CICLO T=" + tiempoGlobal + " ---");
        
        dispatch(); 

        if (currentThread != null) {
            Process p = currentThread.getProcess();
            
            // --- CORRECCIÓN: Despertar al hilo SIEMPRE ---
            currentThread.startExecution();
            try { Thread.sleep(20); } catch (InterruptedException e) {}

            syncManager.acquireProcessLock(p.getPID());
            try {
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
                        System.out.println(">>> " + p.getPID() + " iniciando E/S <<<");
                    } 
                    else {
                        p.setState(ProcessState.READY);
                        addProcessThread(currentThread);
                    }
                    currentThread = null;
                } else {
                    if (currentAlgorithm == Algorithm.RR) {
                        currentQuantumUsed++;
                        if (currentQuantumUsed >= quantum) {
                            System.out.println("[T=" + tiempoGlobal + "] RR QUANTUM: " + p.getPID() + " desalojado.");
                            p.setState(ProcessState.READY);
                            addProcessThread(currentThread);
                            currentThread = null;
                            currentQuantumUsed = 0;
                        }
                    }
                }
            } finally {
                syncManager.releaseProcessLock(p.getPID());
            }
        } else {
            System.out.println("[T=" + tiempoGlobal + "] IDLE");
        }
        
        synchronized (readyQueue) {
            for (ProcessThread thread : readyQueue) {
                thread.getProcess().setT_wait(thread.getProcess().getT_wait() + 1);
            }
        }
        
        System.out.println("[T=" + tiempoGlobal + "] Ops Activas: " + ioManager.getActiveIOOperations());
        tiempoGlobal++;
        
        return !readyQueue.isEmpty() || currentThread != null || ioManager.hasActiveIO();
    }
    
    public IOManager getIOManager() { return ioManager; }
    
    public void shutdown() { 
        if (ioManager != null) ioManager.shutdown(); 
        syncManager.signalAllProcesses(); 
    }
    
    public int getTiempoGlobal() { return tiempoGlobal; }
}