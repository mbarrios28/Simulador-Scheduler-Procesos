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
    public enum Algorithm { FCFS, SJF, RR, PRIORITY }

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
            ProcessThread selectedThread = null;

            synchronized (readyQueue) {
                if (readyQueue.isEmpty()) {
                    return;
                }

                switch (currentAlgorithm) {
                    case SJF:
                        selectedThread = selectSJFPreemptive();
                        break;
                    case PRIORITY:
                        selectedThread = selectPriorityPreemptive();
                        break;
                    case FCFS:
                    case RR:
                        selectedThread = readyQueue.remove(0);
                        break;
                }
            }

            if (selectedThread == null) {
                return;
            }

            Process p = selectedThread.getProcess();

            System.out.println("[DEBUG] Scheduler seleccionó: " + p.getPID());

            // Verificación de Memoria Virtual
            if (memoryManager != null) {
                boolean ok = memoryManager.ensurePages(p);
                
                // Mensaje de diagnostico
                System.out.println("[DEBUG] MemoryManager.ensurePages(" + p.getPID() + ") = " + ok);
                
                if (!ok) {
                    System.out.println("[T=" + tiempoGlobal + "] BLOQUEO MEM: " + p.getPID() + 
                                    " - ESPERANDO CARGA COMPLETA");
                    
                    p.setState(ProcessState.BLOCKED_MEM);
                    ioManager.startFullLoadFault(p, memoryManager, selectedThread);
                    
                    return;
                }
            }

            if (!selectedThread.isAlive()) {
                selectedThread.start();
            }

            currentThread = selectedThread;
            currentQuantumUsed = 0;

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

    private ProcessThread selectSJFPreemptive() {
        // Encontrar el proceso con menor tiempo restante en la cola
        int shortestTime = Integer.MAX_VALUE;
        int selectedIndex = -1;
        
        System.out.println("[DISPATCH] SJF-APROPITIVO evaluando cola:");
        for (int i = 0; i < readyQueue.size(); i++) {
            ProcessThread t = readyQueue.get(i);
            int remaining = t.getProcess().getBurst().getTime_remaining();
            System.out.println("  - " + t.getProcess().getPID() + " tiempo_restante: " + remaining);
            
            if (remaining < shortestTime) {
                shortestTime = remaining;
                selectedIndex = i;
            }
        }
        
        if (selectedIndex == -1) return null;
        
        ProcessThread selectedCandidate = readyQueue.get(selectedIndex);
        
        if (currentThread != null && currentThread.getProcess() != null) {
            int currentRemaining = currentThread.getProcess().getBurst().getTime_remaining();
            int candidateRemaining = selectedCandidate.getProcess().getBurst().getTime_remaining();
            
            System.out.println("[DISPATCH] SJF-APROPITIVO comparando: Actual=" + 
                            currentThread.getProcess().getPID() + "(" + currentRemaining + 
                            ") vs Candidato=" + selectedCandidate.getProcess().getPID() + 
                            "(" + candidateRemaining + ")");
            
            if (candidateRemaining < currentRemaining) {
                System.out.println("[DISPATCH] SJF-APROPITIVO: ¡APROPIACIÓN! " + 
                                selectedCandidate.getProcess().getPID() + " expulsa a " + 
                                currentThread.getProcess().getPID());
                
                preemptCurrentProcess();
                
                return selectSJFPreemptive(); 
            } else {
                System.out.println("[DISPATCH] SJF-APROPITIVO: Mantener actual " + currentThread.getProcess().getPID());
                return null;
            }
        }
        
        ProcessThread selected = readyQueue.remove(selectedIndex);
        System.out.println("[DISPATCH] SJF-APROPITIVO seleccionado: " + selected.getProcess().getPID());
        return selected;
    }

    private ProcessThread selectPriorityPreemptive() {
        // Encontrar el proceso con mayor prioridad en la cola
        int highestPriority = Integer.MAX_VALUE;
        int selectedIndex = -1;
        
        System.out.println("[DISPATCH] PRIORITY-APROPITIVO evaluando cola:");
        for (int i = 0; i < readyQueue.size(); i++) {
            ProcessThread t = readyQueue.get(i);
            int priority = t.getProcess().getPriority();
            System.out.println("  - " + t.getProcess().getPID() + " prioridad: " + priority);
            
            if (priority < highestPriority) {
                highestPriority = priority;
                selectedIndex = i;
            }
        }
        
        if (selectedIndex == -1) return null;
        
        ProcessThread selectedCandidate = readyQueue.get(selectedIndex);
        
        if (currentThread != null && currentThread.getProcess() != null) {
            int currentPriority = currentThread.getProcess().getPriority();
            int candidatePriority = selectedCandidate.getProcess().getPriority();
            
            System.out.println("[DISPATCH] PRIORITY-APROPITIVO comparando: Actual=" + 
                            currentThread.getProcess().getPID() + "(" + currentPriority + 
                            ") vs Candidato=" + selectedCandidate.getProcess().getPID() + 
                            "(" + candidatePriority + ")");
            
            if (candidatePriority < currentPriority) {
                System.out.println("[DISPATCH] PRIORITY-APROPITIVO: ¡APROPIACIÓN! " + 
                                selectedCandidate.getProcess().getPID() + " expulsa a " + 
                                currentThread.getProcess().getPID());
                
                preemptCurrentProcess();
                
                return selectPriorityPreemptive(); 
            } else {
                System.out.println("[DISPATCH] PRIORITY-APROPITIVO: Mantener actual " + currentThread.getProcess().getPID());
                return null;
            }
        }
        
        ProcessThread selected = readyQueue.remove(selectedIndex);
        System.out.println("[DISPATCH] PRIORITY-APROPITIVO seleccionado: " + selected.getProcess().getPID());
        return selected;
    }

    // Metodo para expulsar proceso actual
    private void preemptCurrentProcess() {
        if (currentThread == null) return;
        
        // Guarado de referencia
        ProcessThread threadToPreempt = currentThread;
        Process p = threadToPreempt.getProcess();
        
        syncManager.acquireProcessLock(p.getPID());
        try {
            p.setState(ProcessState.READY);
            
            synchronized (readyQueue) {
                readyQueue.add(0, threadToPreempt);
            }
            
            System.out.println("[T=" + tiempoGlobal + "] PREEMPT: " + p.getPID() + " expulsado y vuelto a cola");
            
            currentThread = null;
            currentQuantumUsed = 0;
        } finally {
            syncManager.releaseProcessLock(p.getPID()); // Usar p.getPID() en lugar de currentThread
        }
    }

    public boolean runOneUnit() {
        System.out.println("\n--- CICLO T=" + tiempoGlobal + " ---");
        
        dispatch(); 

        if (currentThread != null) {
            Process p = currentThread.getProcess();
            
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
                    currentQuantumUsed = 0;
                } else {
                    // Lógica RR
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