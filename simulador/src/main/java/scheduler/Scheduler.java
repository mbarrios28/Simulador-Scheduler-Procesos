package scheduler;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

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

    private Algorithm currentAlgorithm = Algorithm.FCFS;
    private int quantum = 2;
    private int currentQuantumUsed = 0;

    // NUEVO: Contador para procesos añadidos durante el ciclo actual
    private int processesAddedThisCycle = 0;

    public Scheduler() {
        this.readyQueue = Collections.synchronizedList(new LinkedList<>());
        this.syncManager = SyncManager.getInstance();
        this.ioManager = new IOManager(this);
        System.out.println("[Scheduler] Scheduler inicializado");
    }

    public void setMemoryManager(MemoryManager mm) {
        this.memoryManager = mm;
        // PASAR LA REFERENCIA AL IOMANAGER
        this.ioManager.setMemoryManager(mm);
        System.out.println("[Scheduler] MemoryManager configurado y pasado a IOManager");
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

    // NUEVO MÉTODO: Obtener ciclo actual para IOManager
    public int getCurrentCycle() {
        return tiempoGlobal;
    }
    
    // MODIFICAR runOneUnit() - ELIMINAR advanceIOTimers()
    public boolean runOneUnit() {
        System.out.println("\n--- CICLO T=" + tiempoGlobal + " ---");
        
        processesAddedThisCycle = 0;
        
        try { Thread.sleep(5); } catch (InterruptedException e) {}
        
        // FASE 1: COMPLETAR OPERACIONES QUE TERMINARON EN EL CICLO ANTERIOR
        System.out.println("[Scheduler-TIMING] Procesando operaciones completadas...");
        ioManager.processCompletedIO();
        
        // FASE 2: EJECUCIÓN NORMAL DEL CICLO
        checkAndHandlePreemption();
        
        if (currentThread == null) {
            dispatchNewProcess();
        }
        
        if (currentThread != null) {
            executeCurrentProcess();
        } else {
            System.out.println("[T=" + tiempoGlobal + "] IDLE");
        }
        
        updateWaitTimes();
        
        // IMPORTANTE: ¡NO LLAMAR advanceIOTimers()!
        // El tiempo avanza automáticamente con tiempoGlobal++
        
        // FASE 3: MOSTRAR ESTADO Y AVANZAR TIEMPO
        System.out.println("[T=" + tiempoGlobal + "] E/S activas: " + ioManager.getActiveIOOperations());
        
        // Opcional: Mostrar operaciones activas
        if (ioManager.getActiveIOOperations() > 0) {
            ioManager.printActiveOperations();
        }
        
        // AVANZAR TIEMPO AL FINAL
        tiempoGlobal++;
        
        return !readyQueue.isEmpty() || currentThread != null || ioManager.hasActiveIO();
    }
    
    // ========== MÉTODO NUEVO 1: VERIFICAR Y MANEJAR APROPIACIÓN ==========
    private void checkAndHandlePreemption() {
        // SOLO APLICABLE PARA ALGORITMOS APROPITATIVOS
        if (currentAlgorithm != Algorithm.PRIORITY && currentAlgorithm != Algorithm.SJF) {
            return;
        }
        
        // SI NO HAY PROCESO ACTUAL, NADA QUE VERIFICAR
        if (currentThread == null) {
            return;
        }
        
        // ENCONTRAR EL MEJOR PROCESO DISPONIBLE
        ProcessThread bestThread = findBestAvailableThread();
        
        // DEBUG: MOSTRAR QUÉ ENCONTRAMOS
        if (bestThread != null && bestThread.getProcess() != null) {
            System.out.println("[PREEMPT-CHECK] Mejor proceso encontrado: " + 
                             bestThread.getProcess().getPID());
        }
        
        // SI EL MEJOR ES DIFERENTE AL ACTUAL, HACER APROPIACIÓN
        if (bestThread != null && bestThread != currentThread) {
            performPreemption(bestThread);
        }
    }
    
    // ========== MÉTODO NUEVO 2: ENCONTRAR MEJOR PROCESO DISPONIBLE ==========
    private ProcessThread findBestAvailableThread() {
        ProcessThread best = null;
        int bestValue = Integer.MAX_VALUE;
        
        synchronized (readyQueue) {
            // CASO PRIORITY: MENOR NÚMERO = MAYOR PRIORIDAD
            if (currentAlgorithm == Algorithm.PRIORITY) {
                // CONSIDERAR PROCESO ACTUAL PRIMERO
                if (currentThread != null && currentThread.getProcess() != null) {
                    best = currentThread;
                    bestValue = currentThread.getProcess().getPriority();
                }
                
                // BUSCAR EN COLA READY
                for (ProcessThread thread : readyQueue) {
                    if (thread.getProcess() == null) continue;
                    
                    int priority = thread.getProcess().getPriority();
                    if (priority < bestValue) {
                        bestValue = priority;
                        best = thread;
                    }
                }
            }
            // CASO SJF: MENOR TIEMPO RESTANTE
            else if (currentAlgorithm == Algorithm.SJF) {
                // CONSIDERAR PROCESO ACTUAL PRIMERO
                if (currentThread != null && currentThread.getProcess() != null) {
                    best = currentThread;
                    bestValue = currentThread.getProcess().getBurst().getTime_remaining();
                }
                
                // BUSCAR EN COLA READY
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
    
    // ========== MÉTODO NUEVO 3: REALIZAR APROPIACIÓN ==========
    private void performPreemption(ProcessThread newThread) {
        Process oldProcess = currentThread.getProcess();
        Process newProcess = newThread.getProcess();
        
        System.out.println("[PREEMPT] ¡APROPIACIÓN! " + 
                         newProcess.getPID() + " (prio=" + newProcess.getPriority() + 
                         ") expulsa a " + oldProcess.getPID() + 
                         " (prio=" + oldProcess.getPriority() + ")");
        
        // 1. PONER PROCESO ACTUAL DE VUELTA EN READY
        syncManager.acquireProcessLock(oldProcess.getPID());
        try {
            oldProcess.setState(ProcessState.READY);
            synchronized (readyQueue) {
                readyQueue.add(0, currentThread); // Al frente para consideración inmediata
            }
            System.out.println("[PREEMPT] " + oldProcess.getPID() + " vuelto a cola READY");
        } finally {
            syncManager.releaseProcessLock(oldProcess.getPID());
        }
        
        // 2. REMOVER NUEVO PROCESO DE LA COLA (SI ESTÁ EN ELLA)
        synchronized (readyQueue) {
            if (readyQueue.contains(newThread)) {
                readyQueue.remove(newThread);
                System.out.println("[PREEMPT] " + newProcess.getPID() + " removido de cola READY");
            }
        }
        
        // 3. ESTABLECER NUEVO PROCESO COMO ACTUAL
        currentThread = newThread;
        currentQuantumUsed = 0;
        
        // 4. CONFIGURAR NUEVO PROCESO
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
    
    // ========== MÉTODO NUEVO 4: DESPACHAR NUEVO PROCESO ==========
    private void dispatchNewProcess() {
        ProcessThread selectedThread = null;
        
        synchronized (readyQueue) {
            if (readyQueue.isEmpty()) {
                System.out.println("[DISPATCH] Cola READY vacía, nada que despachar");
                return;
            }
            
            // SELECCIÓN SEGÚN ALGORITMO
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
        
        // VERIFICACIÓN DE MEMORIA
        if (memoryManager != null) {
            boolean ok = memoryManager.ensurePages(p);
            
            if (!ok) {
                System.out.println("[T=" + tiempoGlobal + "] BLOQUEO MEM: " + p.getPID() + 
                                 " - ESPERANDO CARGA COMPLETA");
                
                p.setState(ProcessState.BLOCKED_MEM);
                
                // AHORA IOManager TIENE ACCESSO A MemoryManager
                ioManager.startFullLoadFault(p, memoryManager, selectedThread);
                
                return;
            }
        }
        
        // INICIAR HILO SI NO ESTÁ ACTIVO
        if (!selectedThread.isAlive()) {
            selectedThread.start();
        }
        
        // ESTABLECER COMO PROCESO ACTUAL
        currentThread = selectedThread;
        currentQuantumUsed = 0;
        
        // CONFIGURAR PROCESO
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
    
    // ========== MÉTODO NUEVO 5: EJECUTAR PROCESO ACTUAL ==========
    private void executeCurrentProcess() {
        Process p = currentThread.getProcess();
        
        // VERIFICAR QUE PUEDA EJECUTAR
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
        
        // EJECUTAR UNA UNIDAD
        currentThread.startExecution();
        try { Thread.sleep(20); } catch (InterruptedException e) {}
        
        // PROCESAR RESULTADO
        syncManager.acquireProcessLock(p.getPID());
        try {
            if (currentThread.isBurstCompleted()) {
                handleBurstCompletion(); // ← Usa el método modificado
            } else {
                handleBurstContinuation();
            }
        } finally {
            syncManager.releaseProcessLock(p.getPID());
        }
    }
    
    // ========== MÉTODO NUEVO 6: MANEJAR COMPLETACIÓN DE RÁFAGA ==========
    private void handleBurstCompletion() {
        Process p = currentThread.getProcess();
        System.out.println("[T=" + tiempoGlobal + "] " + p.getPID() + " completó ráfaga");
        
        // DEBUG: Mostrar estado actual
        System.out.println("[DEBUG] " + p.getPID() + 
                        " - Estado: " + p.getState() +
                        ", Terminado: " + p.isFinished() +
                        ", Índice burst: " + p.getInd_burst() +
                        "/" + p.getBursts().size());
        
        // VERIFICAR SI EL PROCESO TERMINÓ
        if (p.getState() == ProcessState.TERMINATED || p.isFinished()) {
            p.setT_finish(tiempoGlobal);
            System.out.println(">>> " + p.getPID() + " TERMINADO COMPLETAMENTE <<<");
            currentThread.terminate();
            syncManager.cleanupProcess(p.getPID());
            currentThread = null;
            currentQuantumUsed = 0;
            return;
        }
        
        // VERIFICAR SI LA SIGUIENTE RÁFAGA ES E/S
        Burst nextBurst = p.getBurst();
        
        // DOBLE VERIFICACIÓN
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
            // CASO: INICIAR OPERACIÓN DE E/S
            System.out.println(">>> " + p.getPID() + " INICIANDO E/S (" + 
                            nextBurst.getTime_total() + " ciclos) <<<");
            
            try {
                // 1. Cambiar estado a BLOCKED_IO
                p.setState(ProcessState.BLOCKED_IO);
                
                // 2. Iniciar E/S en IOManager
                ioManager.startIOOperation(p, nextBurst.getTime_total(), currentThread);
                
                System.out.println("[Scheduler] E/S iniciada para " + p.getPID());
                
            } catch (Exception e) {
                System.err.println("[Scheduler-ERROR] Error iniciando E/S: " + e.getMessage());
                p.setState(ProcessState.READY);
                addProcessThread(currentThread);
            }
            
            currentThread = null;
            currentQuantumUsed = 0;
            
        } else {
            // CASO: SIGUIENTE RÁFAGA ES CPU
            p.setState(ProcessState.READY);
            addProcessThread(currentThread);
            currentThread = null;
            currentQuantumUsed = 0;
        }
    }
    
    // ========== MÉTODO NUEVO 7: MANEJAR CONTINUACIÓN DE RÁFAGA ==========
    private void handleBurstContinuation() {
        Process p = currentThread.getProcess();
        
        // LÓGICA ESPECIAL PARA RR
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
        // PARA OTROS ALGORITMOS, EL PROCESO CONTINÚA EN PRÓXIMO CICLO
    }
    
    // ========== MÉTODO NUEVO 8: ACTUALIZAR TIEMPOS DE ESPERA ==========
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
    
    // ========== MÉTODOS DE SELECCIÓN (MANTENER EXISTENTES PERO SIMPLIFICAR) ==========
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
    
    // ========== MÉTODO addProcessThread ACTUALIZADO ==========
    public void addProcessThread(ProcessThread thread) {
        syncManager.acquireGlobalLock();
        try {
            Process p = thread.getProcess();
            
            // NO AÑADIR SI ESTÁ BLOQUEADO O TERMINADO
            if (p.getState() == ProcessState.BLOCKED_IO || 
                p.getState() == ProcessState.BLOCKED_MEM ||
                p.getState() == ProcessState.TERMINATED) {
                
                System.out.println("[Scheduler] " + p.getPID() + " en estado " + 
                                 p.getState() + ", no se añade a READY");
                return;
            }
            
            // ESTABLECER COMO READY
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

    // En Scheduler.java, añade:
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