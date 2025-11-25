package scheduler;

import process.Process;
import process.ProcessState;
import process.Burst;
import java.util.LinkedList;
import java.util.Queue;

public class Scheduler {
    private int tiempoGlobal = 0;
    // Cola de Procesos Listos (FIFO - FCFS)
    private final Queue<Process> readyQueue; 
    private Process currentProcess = null; // Proceso en estado RUNNING

    public Scheduler() {
        this.readyQueue = new LinkedList<>();
    }


    // Transición NEW -> READY: Agrega un proceso a la cola de listos.
    public void addProcess(Process p) {
        p.setState(ProcessState.READY);
        this.readyQueue.offer(p);
        System.out.println("[T=" + tiempoGlobal + "] " + p.getPID() + " entra a READY.");
    }

    // Transición READY -> RUNNING: Selecciona el siguiente proceso (FIFO).
    private void dispatch() {
        // Solo si la CPU esta libre Y la cola no esta vacia
        if (currentProcess == null && !readyQueue.isEmpty()) {
            currentProcess = readyQueue.poll(); // Obtiene el primero (FCFS)
            
            currentProcess.setState(ProcessState.RUNNING);
            
            // Registrar tiempo de inicio si es la primera vez
            if (currentProcess.getT_start() == -1) {
                currentProcess.setT_start(tiempoGlobal);
            }
            
            // NOTA: Se reemplaza "ráfaga" por "rafaga" para evitar problemas de encoding
            System.out.println("[T=" + tiempoGlobal + "] DISPATCH: " + currentProcess.getPID() + " inicia rafaga CPU.");
        }
    }

    // Simula una unidad de tiempo y gestiona las transiciones.
    public boolean runOneUnit() {
        // 1. Intentar Dispatch si la CPU esta libre
        dispatch();

        // 2. Ejecutar el proceso actual
        if (currentProcess != null) {
            
            Burst currentBurst = currentProcess.getBurst();
            
            // a) Consumir una unidad de tiempo de la rafaga
            currentBurst.consumirUnidad();
            
            // Acumular uso de CPU para metricas
            currentProcess.setCpu_usage(currentProcess.getCpu_usage() + 1); 

            System.out.println("[T=" + tiempoGlobal + "] EXEC: " + currentProcess.getPID() + 
                               " (Restante: " + currentBurst.getTime_remaining() + ")");

            // b) Verificar fin de rafaga
            if (currentBurst.isFinished()) {
                
                // Pasa a la siguiente rafaga/estado (TERMINATED, BLOCKED_IO, o READY)
                currentProcess.nextBurst(); 
                
                System.out.println("[T=" + tiempoGlobal + "] FIN RAFAGA: " + currentProcess.getPID());

                // Verificar el nuevo estado:
                if (currentProcess.getState() == ProcessState.TERMINATED) {
                    currentProcess.setT_finish(tiempoGlobal + 1); 
                    System.out.println("--- " + currentProcess.getPID() + " TERMINADO. ---");
                } else if (currentProcess.getState() == ProcessState.BLOCKED_IO) {
                    System.out.println("--- " + currentProcess.getPID() + " BLOQUEADO por E/S. ---");
                } 
                
                currentProcess = null; // Liberar CPU
            }
        } else {
            // 3. Manejar tiempo de espera para procesos en READY (metricas)
            for (Process p : readyQueue) {
                p.setT_wait(p.getT_wait() + 1); 
            }
            System.out.println("[T=" + tiempoGlobal + "] IDLE: CPU inactiva.");
        }

        // 4. Incrementar tiempo global. 
        tiempoGlobal++;

        // 5. Devolver si queda trabajo pendiente
        return !readyQueue.isEmpty() || currentProcess != null;
    }
}