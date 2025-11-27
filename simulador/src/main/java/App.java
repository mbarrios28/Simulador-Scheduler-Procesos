import scheduler.Scheduler;
import process.Process;
import process.Burst;
import process.BurstResource;
import threads.ProcessThread;
import java.util.ArrayList;

public class App {
    private static ArrayList<Burst> createBursts(int cpu1, int io, int cpu2) {
        ArrayList<Burst> bursts = new ArrayList<>();
        if (cpu1 > 0) bursts.add(new Burst(BurstResource.CPU, cpu1));
        if (io > 0) bursts.add(new Burst(BurstResource.IO, io));
        if (cpu2 > 0) bursts.add(new Burst(BurstResource.CPU, cpu2));
        return bursts;
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== SEMANA 2 - E/S DETALLADO ===\n");
        
        // Proceso: CPU(1) → E/S(2) → CPU(1)
        Process p1 = new Process("P1", 0, createBursts(1, 2, 1), 1);
        
        Scheduler scheduler = new Scheduler();
        ProcessThread thread1 = new ProcessThread(p1, scheduler.getIOManager());
        
        System.out.println("Estado inicial P1: " + p1.getState());
        
        thread1.start();
        Thread.sleep(500); // Más tiempo para inicialización
        
        scheduler.addProcessThread(thread1);
        
        System.out.println("\n*** INICIO SIMULACIÓN ***");
        
        boolean trabajoPendiente = true;
        int maxCiclos = 20;
        
        while (trabajoPendiente && maxCiclos > 0) {
            System.out.println("\n=== CICLO " + (20 - maxCiclos) + " ===");
            trabajoPendiente = scheduler.runOneUnit();
            Thread.sleep(1000); // 1 segundo entre ciclos para ver E/S
            maxCiclos--;
        }
        
        System.out.println("\n*** FIN SIMULACIÓN ***");
        
        System.out.println("\n=== ESTADO FINAL ===");
        System.out.println("P1 - Estado: " + p1.getState() + 
                          ", CPU: " + p1.getCpu_usage() +
                          ", Espera: " + p1.getT_wait());
        
        // Esperar para ver logs de E/S
        System.out.println("\nEsperando 5 segundos para operaciones E/S...");
        Thread.sleep(5000);
        
        scheduler.shutdown();
        System.out.println("\n=== SIMULACIÓN COMPLETADA ===");
    }
}