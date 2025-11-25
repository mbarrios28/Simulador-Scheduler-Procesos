import scheduler.Scheduler; 
import process.Process;     
import process.Burst;       
import process.BurstResource;
import java.util.ArrayList;
import threads.ProcessThread;

public class App {

    private static ArrayList<Burst> createBursts(int cpu1, int io, int cpu2) {
        ArrayList<Burst> bursts = new ArrayList<>();
        if (cpu1 > 0) bursts.add(new Burst(BurstResource.CPU, cpu1));
        if (io > 0) bursts.add(new Burst(BurstResource.IO, io));
        if (cpu2 > 0) bursts.add(new Burst(BurstResource.CPU, cpu2));
        return bursts;
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== PRUEBA FINAL - HILOS FUNCIONALES ===\n");
        
        // Procesos de prueba
        Process p1 = new Process("P1", 0, createBursts(3, 0, 0), 1);
        Process p2 = new Process("P2", 0, createBursts(2, 0, 0), 1);
        
        Scheduler scheduler = new Scheduler();
        
        ProcessThread thread1 = new ProcessThread(p1);
        ProcessThread thread2 = new ProcessThread(p2);
        
        // Iniciar hilos
        thread1.start();
        thread2.start();
        Thread.sleep(200);
        
        // Agregar al scheduler
        scheduler.addProcessThread(thread1);
        scheduler.addProcessThread(thread2);
        
        System.out.println("\n*** INICIO SIMULACIÓN ***");
        
        // Bucle principal
        boolean trabajoPendiente = true;
        int maxCiclos = 10; // Límite razonable
        
        while (trabajoPendiente && maxCiclos > 0) {
            trabajoPendiente = scheduler.runOneUnit();
            Thread.sleep(300); // Pausa para visualización
            maxCiclos--;
        }
        
        System.out.println("\n*** FIN SIMULACIÓN ***");
        
        // Métricas finales
        System.out.println("\n=== MÉTRICAS FINALES ===");
        System.out.println("P1 - Inicio: " + p1.getT_start() + 
                          ", Fin: " + p1.getT_finish() +
                          ", Espera: " + p1.getT_wait() + 
                          ", CPU: " + p1.getCpu_usage() +
                          ", Estado: " + p1.getState());
        System.out.println("P2 - Inicio: " + p2.getT_start() + 
                          ", Fin: " + p2.getT_finish() +
                          ", Espera: " + p2.getT_wait() + 
                          ", CPU: " + p2.getCpu_usage() +
                          ", Estado: " + p2.getState());
                          
        // Terminar hilos si aún están activos
        if (!thread1.isProcessTerminated()) thread1.terminate();
        if (!thread2.isProcessTerminated()) thread2.terminate();
    }
}