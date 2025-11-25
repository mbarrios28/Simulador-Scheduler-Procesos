package scheduler;

import process.Process;
import process.ProcessState;
import threads.ProcessThread;
import java.util.LinkedList;
import java.util.Queue;

public class Scheduler {
    private int tiempoGlobal = 0;
    private final Queue<ProcessThread> readyQueue; 
    private ProcessThread currentThread = null;
    
    public Scheduler() {
        this.readyQueue = new LinkedList<>();
    }

    public void addProcessThread(ProcessThread thread) {
        thread.getProcess().setState(ProcessState.READY);
        this.readyQueue.offer(thread);
        System.out.println("[T=" + tiempoGlobal + "] " + thread.getProcess().getPID() + " entra a READY.");
    }

    public boolean runOneUnit() {
        System.out.println("\n--- CICLO T=" + tiempoGlobal + " ---");
        
        // 1. Si hay un proceso ejecutándose, verificar su estado
        if (currentThread != null) {
            Process currentProcess = currentThread.getProcess();
            
            if (currentThread.isBurstCompleted()) {
                System.out.println("[T=" + tiempoGlobal + "] " + currentProcess.getPID() + " completó ráfaga");
                
                if (currentThread.isProcessTerminated()) {
                    currentProcess.setT_finish(tiempoGlobal);
                    System.out.println(">>> " + currentProcess.getPID() + " TERMINADO <<<");
                    currentThread.terminate();
                    currentThread = null;
                } else if (currentProcess.getState() == ProcessState.BLOCKED_IO) {
                    System.out.println(">>> " + currentProcess.getPID() + " BLOQUEADO por E/S <<<");
                    currentThread = null;
                } else {
                    // Vuelve a READY
                    currentProcess.setState(ProcessState.READY);
                    readyQueue.offer(currentThread);
                    System.out.println("[T=" + tiempoGlobal + "] " + currentProcess.getPID() + " vuelve a READY");
                    currentThread = null;
                }
            } else {
                // El proceso necesita MÁS TIEMPO de la misma ráfaga
                // Ponerlo de nuevo en RUNNING para el siguiente ciclo
                System.out.println("[T=" + tiempoGlobal + "] " + currentProcess.getPID() + " necesita más tiempo de CPU");
                readyQueue.offer(currentThread);
                currentThread = null;
            }
        }
        
        // 2. Despachar siguiente proceso si CPU está libre
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
        
        // 3. Incrementar tiempo de espera para procesos en cola
        for (ProcessThread thread : readyQueue) {
            thread.getProcess().setT_wait(thread.getProcess().getT_wait() + 1);
        }
        
        // 4. Mostrar estado actual
        if (currentThread == null && !readyQueue.isEmpty()) {
            System.out.println("[T=" + tiempoGlobal + "] IDLE: " + readyQueue.size() + " procesos en READY");
        } else if (currentThread == null) {
            System.out.println("[T=" + tiempoGlobal + "] IDLE: No hay procesos");
        } else {
            System.out.println("[T=" + tiempoGlobal + "] EXEC: " + currentThread.getProcess().getPID() + " en CPU");
        }
        
        // 5. Incrementar tiempo global
        tiempoGlobal++;
        
        // 6. Verificar si hay trabajo pendiente
        boolean trabajoPendiente = !readyQueue.isEmpty() || currentThread != null;
        
        return trabajoPendiente;
    }
    
    public int getTiempoGlobal() {
        return tiempoGlobal;
    }
}