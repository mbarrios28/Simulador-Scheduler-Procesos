package ProcessThread;

import scheduler.Scheduler;
import process.Process;
import process.Burst;
import process.BurstResource;
import threads.ProcessThread;
import java.util.ArrayList;

public class AppWithThreads {

    // Método de ayuda para crear ráfagas manualmente
    private static ArrayList<Burst> createBursts(int cpu1, int io, int cpu2) {
        ArrayList<Burst> bursts = new ArrayList<>();
        if (cpu1 > 0) bursts.add(new Burst(BurstResource.CPU, cpu1));
        if (io > 0) bursts.add(new Burst(BurstResource.IO, io));
        if (cpu2 > 0) bursts.add(new Burst(BurstResource.CPU, cpu2));
        return bursts;
    }

    public static void main(String[] args) throws InterruptedException {
        
        System.out.println("=== PRUEBA CON HILOS - SEMANA 1 ===\n");
        
        // --- CREAR PROCESOS DE PRUEBA ---
        Process p1 = new Process("P1", 0, createBursts(3, 0, 0), 1);
        Process p2 = new Process("P2", 0, createBursts(2, 0, 0), 1);
        
        // --- INICIAR SCHEDULER CON HILOS ---
        Scheduler scheduler = new Scheduler();
        
        // --- CREAR Y AGREGAR HILOS ---
        ProcessThread thread1 = new ProcessThread(p1);
        ProcessThread thread2 = new ProcessThread(p2);
        
        // Iniciar hilos (quedarán en espera)
        thread1.start();
        thread2.start();
        
        // Dar tiempo para que los hilos inicien
        Thread.sleep(100);
        
        // Agregar hilos al scheduler
        scheduler.addProcessThread(thread1);
        scheduler.addProcessThread(thread2);
        
        System.out.println("\n*** INICIO SIMULACIÓN CON HILOS ***");
        
        // Bucle principal de simulación
        boolean trabajoPendiente = true;
        while (trabajoPendiente) {
            trabajoPendiente = scheduler.runOneUnit();
            
            // Pequeña pausa para visualización
            Thread.sleep(500);
        }
        
        System.out.println("*** FIN SIMULACIÓN CON HILOS ***\n");
        
        // Esperar a que los hilos terminen completamente
        Thread.sleep(1000);
        
        // Mostrar métricas finales
        System.out.println("=== MÉTRICAS FINALES ===");
        System.out.println("P1 - Tiempo espera: " + p1.getT_wait() + 
                          ", Uso CPU: " + p1.getCpu_usage());
        System.out.println("P2 - Tiempo espera: " + p2.getT_wait() + 
                          ", Uso CPU: " + p2.getCpu_usage());
    }
}