package scheduler;

import process.Process;
import process.ProcessState;
import threads.ProcessThread;
import threads.IOManager;
import java.util.LinkedList;
import java.util.Queue;

public class Scheduler {
    private int tiempoGlobal = 0;
    private final Queue<ProcessThread> readyQueue; 
    private ProcessThread currentThread = null;
    private IOManager ioManager;
    
    public Scheduler() {
        this.readyQueue = new LinkedList<>();
        this.ioManager = new IOManager(this);
    }

    public void addProcessThread(ProcessThread thread) {
        thread.getProcess().setState(ProcessState.READY);
        this.readyQueue.offer(thread);
        System.out.println("[T=" + tiempoGlobal + "] " + thread.getProcess().getPID() + " en READY.");
    }

    public boolean runOneUnit() {
        System.out.println("\n--- CICLO T=" + tiempoGlobal + " ---");
        
        // 1. Manejar proceso actual
        if (currentThread != null) {
            Process currentProcess = currentThread.getProcess();
            
            if (currentThread.isBurstCompleted()) {
                System.out.println("[T=" + tiempoGlobal + "] " + currentProcess.getPID() + " completó burst");
                
                if (currentThread.isProcessTerminated()) {
                    currentProcess.setT_finish(tiempoGlobal);
                    System.out.println(">>> " + currentProcess.getPID() + " TERMINADO <<<");
                    currentThread.terminate();
                } 
                // Si debe iniciar E/S, mantenerlo en RUNNING para el próximo ciclo
                else if (currentThread.shouldStartIO()) {
                    System.out.println(">>> " + currentProcess.getPID() + " programado para E/S <<<");
                    readyQueue.offer(currentThread);
                }
                else if (currentThread.isBlockedByIO()) {
                    System.out.println(">>> " + currentProcess.getPID() + " en E/S <<<");
                } else {
                    currentProcess.setState(ProcessState.READY);
                    readyQueue.offer(currentThread);
                    System.out.println("[T=" + tiempoGlobal + "] " + currentProcess.getPID() + " a READY");
                }
                
                currentThread = null;
            } else {
                // Necesita más tiempo de CPU
                System.out.println("[T=" + tiempoGlobal + "] " + currentProcess.getPID() + " necesita más CPU");
                readyQueue.offer(currentThread);
                currentThread = null;
            }
        }
        
        // 2. Despachar siguiente proceso
        if (currentThread == null && !readyQueue.isEmpty()) {
            currentThread = readyQueue.poll();
            Process currentProcess = currentThread.getProcess();
            
            if (currentProcess.getT_start() == -1) {
                currentProcess.setT_start(tiempoGlobal);
            }
            
            currentProcess.setState(ProcessState.RUNNING);
            currentThread.startExecution();
            
            System.out.println("[T=" + tiempoGlobal + "] DISPATCH: " + currentProcess.getPID() + " → RUNNING");
        }
        
        // 3. Tiempo de espera
        for (ProcessThread thread : readyQueue) {
            thread.getProcess().setT_wait(thread.getProcess().getT_wait() + 1);
        }
        
        // 4. Estado actual
        if (currentThread == null && !readyQueue.isEmpty()) {
            System.out.println("[T=" + tiempoGlobal + "] IDLE: " + readyQueue.size() + " en READY");
        } else if (currentThread == null) {
            System.out.println("[T=" + tiempoGlobal + "] IDLE: Sin procesos");
        } else {
            System.out.println("[T=" + tiempoGlobal + "] EXEC: " + currentThread.getProcess().getPID());
        }
        
        System.out.println("[T=" + tiempoGlobal + "] E/S activas: " + ioManager.getActiveIOOperations());
        
        tiempoGlobal++;
        
        return !readyQueue.isEmpty() || currentThread != null || ioManager.hasActiveIO();
    }
    
    public IOManager getIOManager() {
        return ioManager;
    }
    
    public void shutdown() {
        ioManager.shutdown();
    }
    
    public int getTiempoGlobal() {
        return tiempoGlobal;
    }
}