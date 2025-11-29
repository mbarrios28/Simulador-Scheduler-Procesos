package scheduler;

import process.Process;
import process.ProcessState;
import threads.ProcessThread;
import threads.IOManager;
import java.util.LinkedList;
import java.util.Collections;
import java.util.List;

public class Scheduler {
    // Enum para seleccionar el algoritmo
    public enum Algorithm { FCFS, SJF, RR }

    private int tiempoGlobal = 0;
    
    private final List<ProcessThread> readyQueue; 
    
    private ProcessThread currentThread = null;
    private IOManager ioManager;

    //configuracion por defecto
    private Algorithm currentAlgorithm = Algorithm.FCFS; // Algoritmo por defecto
    private int quantum = 2; // Quantum por defecto para RR
    private int currentQuantumUsed = 0; // Contador interno para RR

    public Scheduler() {
        // Inicializamos la lista como sincronizada (Thread-Safe)
        this.readyQueue = Collections.synchronizedList(new LinkedList<>());
        this.ioManager = new IOManager(this);
    }

    // Metodos para configuracion 
    public void setAlgorithm(Algorithm algo) { 
        this.currentAlgorithm = algo; 
        System.out.println("[Scheduler] Algoritmo cambiado a: " + algo);
    }
    
    public void setQuantum(int q) { 
        this.quantum = q; 
    }

    public void addProcess(Process p) {
        ProcessThread thread = new ProcessThread(p, this.ioManager);
        thread.start(); 
        addProcessThread(thread);
    }

    public void addProcessThread(ProcessThread thread) {
        thread.getProcess().setState(ProcessState.READY);
        synchronized (readyQueue) {
            this.readyQueue.add(thread);
        }
        System.out.println("[T=" + tiempoGlobal + "] " + thread.getProcess().getPID() + " en READY.");
    }

    private void dispatch() {
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
                            int remaining = thread.getProcess().getBurst().getTime_remaining();
                            if (remaining < minTime) {
                                minTime = remaining;
                                selectedThread = thread;
                                selectedIndex = i;
                            }
                        }
                        if (selectedThread != null) readyQueue.remove(selectedIndex);
                        break;
                }
            }

            if (selectedThread != null) {
                currentThread = selectedThread;
                Process p = currentThread.getProcess();
                
                if (p.getT_start() == -1) p.setT_start(tiempoGlobal);
                
                p.setState(ProcessState.RUNNING);
                
                currentQuantumUsed = 0;
                System.out.println("[T=" + tiempoGlobal + "] DISPATCH (" + currentAlgorithm + "): " + 
                                   p.getPID() + " -> RUNNING");
            }
        }
    }

    public boolean runOneUnit() {
        System.out.println("\n--- CICLO T=" + tiempoGlobal + " ---");
        
        // 1. Intentar poner un proceso en CPU
        dispatch();

        // 2. Gestionar el proceso actual
        if (currentThread != null) {
            Process p = currentThread.getProcess();

            currentThread.startExecution();
            
            try { Thread.sleep(20); } catch (InterruptedException e) {}

            // Verificamos el estado reportado por el hilo
            if (currentThread.isBurstCompleted()) {
                System.out.println("[T=" + tiempoGlobal + "] " + p.getPID() + " completó ráfaga");
                
                if (currentThread.isProcessTerminated()) {
                    p.setT_finish(tiempoGlobal);
                    System.out.println(">>> " + p.getPID() + " TERMINADO <<<");
                    currentThread.terminate();
                } 
                else if (currentThread.shouldStartIO()) {
                    currentThread.startExecution();
                    try { Thread.sleep(20); } catch (InterruptedException e) {}
                    
                    System.out.println(">>> " + p.getPID() + " iniciando E/S (bloqueado) <<<");
                }
                else {
                    // Terminó ráfaga CPU, sigue vivo y no es IO -> vuelve a cola READY
                    p.setState(ProcessState.READY);
                    addProcessThread(currentThread); 
                }
                currentThread = null; // CPU Libre
            } 
            else {
                // Ráfaga en progreso verificar Quantum si es Round Robin
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
        } else {
            System.out.println("[T=" + tiempoGlobal + "] IDLE (CPU inactiva)");
        }
        
        // 3. Métricas de espera
        synchronized (readyQueue) {
            for (ProcessThread thread : readyQueue) {
                thread.getProcess().setT_wait(thread.getProcess().getT_wait() + 1);
            }
        }
        
        // 4. Reporte de E/S
        System.out.println("[T=" + tiempoGlobal + "] E/S activas: " + ioManager.getActiveIOOperations());
        
        tiempoGlobal++;
        
        // Condición de parada
        return !readyQueue.isEmpty() || currentThread != null || ioManager.hasActiveIO();
    }
    
    public IOManager getIOManager() { return ioManager; }
    
    public void shutdown() { 
        if (ioManager != null) ioManager.shutdown(); 
    }
    
    public int getTiempoGlobal() { return tiempoGlobal; }
}