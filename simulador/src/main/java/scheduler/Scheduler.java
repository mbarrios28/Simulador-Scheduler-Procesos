package scheduler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import memory.MemoryManager;
import process.Burst;
import process.BurstResource;
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
    private Map<String, ProcessThread> delayedIOStart;

    private Algorithm currentAlgorithm = Algorithm.FCFS;
    private int quantum = 2;
    private int currentQuantumUsed = 0;
    private int processesAddedThisCycle = 0;

    public Scheduler() {
        this.readyQueue = Collections.synchronizedList(new LinkedList<>());
        this.syncManager = SyncManager.getInstance();
        this.ioManager = new IOManager(this);
        this.delayedIOStart = new HashMap<>(); 
        System.out.println("[Scheduler] Scheduler inicializado");
    }

    public void setMemoryManager(MemoryManager mm) {
        this.memoryManager = mm;
        this.ioManager.setMemoryManager(mm);
        System.out.println("[Scheduler] MemoryManager configurado y pasado a IOManager");
    }

    public void setAlgorithm(Algorithm algo) { 
        this.currentAlgorithm = algo; 
        System.out.println("[Scheduler] Algoritmo cambiado a: " + algo);
    }
    
    public void setQuantum(int q) { this.quantum = q; }

    // MÉTODOS AGREGADOS PARA EL DISPLAY
    public Algorithm getCurrentAlgorithm() { 
        return currentAlgorithm; 
    }
    
    public int getQuantum() { 
        return quantum; 
    }
    
    public List<Process> getReadyProcesses() {
        List<Process> ready = new LinkedList<>();
        synchronized (readyQueue) {
            for (ProcessThread thread : readyQueue) {
                if (thread.getProcess() != null && 
                    thread.getProcess().getState() == ProcessState.READY) {
                    ready.add(thread.getProcess());
                }
            }
        }
        return ready;
    }
    
    public Process getCurrentProcess() {
        if (currentThread != null && currentThread.getProcess() != null) {
            Process p = currentThread.getProcess();
            // Solo retornar si realmente está en ejecución
            if (p.getState() == ProcessState.RUNNING) {
                return p;
            }
        }
        return null;
    }
    
    /*
     * Captura el estado del proceso actual ANTES de ejecutar
     * Retorna información sobre qué proceso va a ejecutar y su ráfaga
     */
    public String[] captureCurrentState() {
        if (currentThread != null && currentThread.getProcess() != null) {
            Process p = currentThread.getProcess();
            if (p.getState() == ProcessState.RUNNING && !p.isFinished()) {
                Burst b = p.getBurst();
                if (b != null) {
                    // [0]=PID, [1]=Estado, [2]=TipoRafaga, [3]=Restante, [4]=Total
                    return new String[] {
                        p.getPID(),
                        p.getState().toString(),
                        b.getResource().toString(),
                        String.valueOf(b.getTime_remaining()),
                        String.valueOf(b.getTime_total())
                    };
                }
            }
        }
        return null; // No hay proceso ejecutando
    }
    
    // FIN MÉTODOS AGREGADOS

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

    public int getCurrentCycle() {
        return tiempoGlobal;
    }
    
    // Variable para capturar el estado antes de ejecutar
    private String[] cycleExecutionSnapshot = null;
    
    public boolean runOneUnit() {
        System.out.println("\n--- CICLO T=" + tiempoGlobal + " ---");
        
        processesAddedThisCycle = 0;
        cycleExecutionSnapshot = null; // Resetear snapshot
        
        try { Thread.sleep(5); } catch (InterruptedException e) {}

        processDelayedIOOperations();
        
        System.out.println("[Scheduler-TIMING] Procesando operaciones completadas...");
        ioManager.processCompletedIO();
        
        checkAndHandlePreemption();
        
        if (currentThread == null) {
            dispatchNewProcess();
        }
        
        // *** CAPTURAR ESTADO AQUÍ: después de dispatch, antes de ejecutar ***
        if (currentThread != null) {
            cycleExecutionSnapshot = captureCurrentState();
            executeCurrentProcess();
        } else {
            System.out.println("[T=" + tiempoGlobal + "] IDLE");
        }
        
        updateWaitTimes();

        // chicos, ya no usar advanceIOTimers()poruqe el tiempo avanza automáticamente con tiempoGlobal++
        
        System.out.println("[T=" + tiempoGlobal + "] E/S activas: " + ioManager.getActiveIOOperations());
        
        if (ioManager.getActiveIOOperations() > 0) {
            ioManager.printActiveOperations();
        }
        
        tiempoGlobal++;
        
        return !readyQueue.isEmpty() || currentThread != null || ioManager.hasActiveIO();
    }

    private void processDelayedIOOperations() {
        if (delayedIOStart.isEmpty()) {
            return;
        }
        
        List<String> toRemove = new ArrayList<>();
        
        for (Map.Entry<String, ProcessThread> entry : delayedIOStart.entrySet()) {
            String pid = entry.getKey();
            ProcessThread thread = entry.getValue();
            Process p = thread.getProcess();
            
            if (p != null && p.getState() == ProcessState.READY) {
                Burst nextBurst = p.getBurst();
                if (nextBurst != null && nextBurst.getResource() == BurstResource.IO) {
                    System.out.println(">>> " + p.getPID() + " INICIANDO E/S (" + 
                                    nextBurst.getTime_total() + " ciclos) <<<");
                    
                    syncManager.acquireProcessLock(pid);
                    try {
                        p.setState(ProcessState.BLOCKED_IO);
                        ioManager.startIOOperation(p, nextBurst.getTime_total(), thread);
                        System.out.println("[Scheduler] E/S iniciada para " + p.getPID());
                    } finally {
                        syncManager.releaseProcessLock(pid);
                    }
                    
                    toRemove.add(pid);
                }
            }
        }
        
        for (String pid : toRemove) {
            delayedIOStart.remove(pid);
        }
    }
    
    /**
     * Obtiene el snapshot capturado durante el último runOneUnit()
     * Este snapshot contiene el estado del proceso DESPUÉS de dispatch pero ANTES de ejecutar
     */
    public String[] getLastExecutionSnapshot() {
        return cycleExecutionSnapshot;
    }
    
    private void checkAndHandlePreemption() {
        if (currentAlgorithm != Algorithm.PRIORITY && currentAlgorithm != Algorithm.SJF) {
            return;
        }
        
        if (currentThread == null) {
            return;
        }
        
        ProcessThread bestThread = findBestAvailableThread();
        
        if (bestThread != null && bestThread.getProcess() != null) {
            System.out.println("[PREEMPT-CHECK] Mejor proceso encontrado: " + 
            bestThread.getProcess().getPID());
        }
        
        if (bestThread != null && bestThread != currentThread) {
            performPreemption(bestThread);
        }
    }
    
    private ProcessThread findBestAvailableThread() {
        ProcessThread best = null;
        int bestValue = Integer.MAX_VALUE;
        
        synchronized (readyQueue) {
            if (currentAlgorithm == Algorithm.PRIORITY) {
                if (currentThread != null && currentThread.getProcess() != null) {
                    best = currentThread;
                    bestValue = currentThread.getProcess().getPriority();
                }
                
                for (ProcessThread thread : readyQueue) {
                    if (thread.getProcess() == null) continue;
                    
                    int priority = thread.getProcess().getPriority();
                    if (priority < bestValue) {
                        bestValue = priority;
                        best = thread;
                    }
                }
            }
            else if (currentAlgorithm == Algorithm.SJF) {
                if (currentThread != null && currentThread.getProcess() != null) {
                    best = currentThread;
                    bestValue = currentThread.getProcess().getBurst().getTime_remaining();
                }
                
                for (ProcessThread thread : readyQueue) {
                    if (thread.getProcess() == null) continue;
                    
                    int remaining = thread.getProcess().getBurst().getTime_remaining();
                    if (remaining < bestValue) {
                        bestValue = remaining;
                        best = thread;
                    }
                }
            }
        }
        
        return best;
    }
    
    private void performPreemption(ProcessThread newThread) {
        Process oldProcess = currentThread.getProcess();
        Process newProcess = newThread.getProcess();
        
        System.out.println("[PREEMPT] ¡APROPIACIÓN! " + 
        newProcess.getPID() + " (prio=" + newProcess.getPriority() + 
        ") expulsa a " + oldProcess.getPID() + 
        " (prio=" + oldProcess.getPriority() + ")");
        
        syncManager.acquireProcessLock(oldProcess.getPID());
        try {
            oldProcess.setState(ProcessState.READY);
            synchronized (readyQueue) {
                readyQueue.add(0, currentThread); 
            }
            System.out.println("[PREEMPT] " + oldProcess.getPID() + " vuelto a cola READY");
        } finally {
            syncManager.releaseProcessLock(oldProcess.getPID());
        }
        
        synchronized (readyQueue) {
            if (readyQueue.contains(newThread)) {
                readyQueue.remove(newThread);
                System.out.println("[PREEMPT] " + newProcess.getPID() + " removido de cola READY");
            }
        }
        
        currentThread = newThread;
        currentQuantumUsed = 0;
        
        syncManager.acquireProcessLock(newProcess.getPID());
        try {
            if (newProcess.getT_start() == -1) {
                newProcess.setT_start(tiempoGlobal);
                System.out.println("[PREEMPT] " + newProcess.getPID() + " - Tiempo inicio: " + tiempoGlobal);
            }
            newProcess.setState(ProcessState.RUNNING);
            System.out.println("[PREEMPT] " + newProcess.getPID() + " establecido como RUNNING");
        } finally {
            syncManager.releaseProcessLock(newProcess.getPID());
        }
    }
    
    private void dispatchNewProcess() {
        ProcessThread selectedThread = null;
        
        synchronized (readyQueue) {
            if (readyQueue.isEmpty()) {
                System.out.println("[DISPATCH] Cola READY vacía, nada que despachar");
                return;
            }
            
            switch (currentAlgorithm) {
                case SJF:
                    selectedThread = selectSJF();
                    break;
                case PRIORITY:
                    selectedThread = selectPriority();
                    break;
                case FCFS:
                case RR:
                    selectedThread = readyQueue.remove(0);
                    break;
            }
        }
        
        if (selectedThread == null) {
            System.out.println("[DISPATCH] No se pudo seleccionar proceso");
            return;
        }
        
        Process p = selectedThread.getProcess();
        System.out.println("[DISPATCH] Proceso seleccionado: " + p.getPID());
        
        if (memoryManager != null) {
            boolean ok = memoryManager.ensurePages(p);
            
            if (!ok) {
                System.out.println("[T=" + tiempoGlobal + "] BLOQUEO MEM: " + p.getPID() +"  ESPERANDO CARGA COMPLETA");
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
            System.out.println("[T=" + tiempoGlobal + "] DISPATCH (" + currentAlgorithm + "): " + p.getPID());
        } finally {
            syncManager.releaseProcessLock(p.getPID());
        }
    }
    
    private void executeCurrentProcess() {
        Process p = currentThread.getProcess();
        
        if (p.getState() == ProcessState.BLOCKED_IO || 
            p.getState() == ProcessState.BLOCKED_MEM ||
            p.getState() == ProcessState.TERMINATED) {
            
            System.out.println("[EXECUTE] " + p.getPID() + 
                            " no puede ejecutar (estado: " + p.getState() + ")");
            
            if (p.getState() == ProcessState.TERMINATED) {
                p.setT_finish(tiempoGlobal);
                currentThread.terminate();
                syncManager.cleanupProcess(p.getPID());
            }
            
            currentThread = null;
            currentQuantumUsed = 0;
            return;
        }
        
        currentThread.startExecution();
        try { Thread.sleep(20); } catch (InterruptedException e) {}
        
        syncManager.acquireProcessLock(p.getPID());
        try {
            if (currentThread.isBurstCompleted()) {
                handleBurstCompletion(); 
            } else {
                handleBurstContinuation();
            }
        } finally {
            syncManager.releaseProcessLock(p.getPID());
        }
    }
    
    private void handleBurstCompletion() {
        Process p = currentThread.getProcess();
        System.out.println("[T=" + tiempoGlobal + "] " + p.getPID() + " completó ráfaga");
        
        System.out.println("[DEBUG] " + p.getPID() + 
                        " - Estado: " + p.getState() +
                        ", Terminado: " + p.isFinished() +
                        ", Índice burst: " + p.getInd_burst() +
                        "/" + p.getBursts().size());
        
        if (p.getState() == ProcessState.TERMINATED || p.isFinished()) {
            p.setT_finish(tiempoGlobal);
            System.out.println( p.getPID() + " TERMINADO COMPLETAMENTE ");
            currentThread.terminate();
            syncManager.cleanupProcess(p.getPID());
            currentThread = null;
            currentQuantumUsed = 0;
            return;
        }
        
        Burst nextBurst = p.getBurst();
        
        if (nextBurst == null) {
            System.err.println("[Scheduler-ERROR] " + p.getPID() + " no tiene siguiente ráfaga");
            p.setState(ProcessState.READY);
            addProcessThread(currentThread);
            currentThread = null;
            currentQuantumUsed = 0;
            return;
        }
        
        boolean nextIsIO = (nextBurst.getResource() == BurstResource.IO);
        
        if (nextIsIO) {
            System.out.println(">>> " + p.getPID() + " DEBERÁ INICIAR E/S en T=" + 
                            (tiempoGlobal + 1) + " (" + nextBurst.getTime_total() + " ciclos) <<<");
            
            // *** CAMBIO IMPORTANTE: No cambiar estado aquí ***
            // El IO se iniciará en el PRÓXIMO ciclo
            
            // Agregar a cola para que se inicie IO en siguiente ciclo
            delayedIOStart.put(p.getPID(), currentThread);
            
            // Mantener READY por ahora (será bloqueado en siguiente ciclo)
            p.setState(ProcessState.READY);
            addProcessThread(currentThread);
            
            currentThread = null;
            currentQuantumUsed = 0;
            
        } else {
            p.setState(ProcessState.READY);
            addProcessThread(currentThread);
            currentThread = null;
            currentQuantumUsed = 0;
        }
    }
    
    private void handleBurstContinuation() {
        Process p = currentThread.getProcess();
        
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
    
    private void updateWaitTimes() {
        synchronized (readyQueue) {
            for (ProcessThread thread : readyQueue) {
                Process proc = thread.getProcess();
                if (proc.getState() == ProcessState.READY) {
                    proc.setT_wait(proc.getT_wait() + 1);
                }
            }
        }
    }
    
    private ProcessThread selectSJF() {
        int shortestTime = Integer.MAX_VALUE;
        int selectedIndex = -1;
        
        for (int i = 0; i < readyQueue.size(); i++) {
            ProcessThread t = readyQueue.get(i);
            if (t.getProcess() == null) continue;
            
            int remaining = t.getProcess().getBurst().getTime_remaining();
            if (remaining < shortestTime) {
                shortestTime = remaining;
                selectedIndex = i;
            }
        }
        
        return (selectedIndex != -1) ? readyQueue.remove(selectedIndex) : null;
    }
    
    private ProcessThread selectPriority() {
        int highestPriority = Integer.MAX_VALUE;
        int selectedIndex = -1;
        
        for (int i = 0; i < readyQueue.size(); i++) {
            ProcessThread t = readyQueue.get(i);
            if (t.getProcess() == null) continue;
            
            int priority = t.getProcess().getPriority();
            if (priority < highestPriority) {
                highestPriority = priority;
                selectedIndex = i;
            }
        }
        
        return (selectedIndex != -1) ? readyQueue.remove(selectedIndex) : null;
    }
    
    public void addProcessThread(ProcessThread thread) {
        syncManager.acquireGlobalLock();
        try {
            Process p = thread.getProcess();
            
            if (p.getState() == ProcessState.BLOCKED_IO || 
                p.getState() == ProcessState.BLOCKED_MEM ||
                p.getState() == ProcessState.TERMINATED) {
                
                System.out.println("[Scheduler] " + p.getPID() + " en estado " + 
                p.getState() + ", no se añade a READY");
                return;
            }
            
            if (p.getState() != ProcessState.READY) {
                p.setState(ProcessState.READY);
            }
            
            synchronized (readyQueue) {
                if (!readyQueue.contains(thread)) {
                    readyQueue.add(thread);
                    processesAddedThisCycle++;
                    System.out.println("[T=" + tiempoGlobal + "] " + p.getPID() + " añadido a READY.");
                }
            }
            
        } finally {
            syncManager.releaseGlobalLock();
        }
    }

    public int getReadyQueueSize() {
        synchronized (readyQueue) {
            return readyQueue.size();
        }
    }

    public String getCurrentProcessId() {
        return (currentThread != null && currentThread.getProcess() != null) ? 
            currentThread.getProcess().getPID() : "NONE";
    }
    
    public IOManager getIOManager() { return ioManager; }
    
    public void shutdown() { 
        if (ioManager != null) ioManager.shutdown(); 
        syncManager.signalAllProcesses(); 
    }
    
    public int getTiempoGlobal() { return tiempoGlobal; }
}