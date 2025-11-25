import scheduler.Scheduler; 
import process.Process;     
import process.Burst;       
import process.BurstResource;
import java.util.ArrayList;

public class App {

    // Método de ayuda para crear ráfagas manualmente para las pruebas
    private static ArrayList<Burst> createBursts(int cpu1, int io, int cpu2) {
        ArrayList<Burst> bursts = new ArrayList<>();
        // Ráfaga 1
        if (cpu1 > 0) bursts.add(new Burst(BurstResource.CPU, cpu1));
        // Ráfaga 2 (E/S)
        if (io > 0) bursts.add(new Burst(BurstResource.IO, io));
        // Ráfaga 3
        if (cpu2 > 0) bursts.add(new Burst(BurstResource.CPU, cpu2));
        return bursts;
    }

    public static void main(String[] args) {
        
        // --- PROCESOS DE PRUEBA (FCFS) ---
        
        // P2: T.Llegada=0, Ráfaga=3 (CPU(3))
        Process p2 = new Process("P2", 0, createBursts(3, 0, 0), 1);
        
        // P3: T.Llegada=0, Ráfaga=4 (CPU(4))
        Process p3 = new Process("P3", 0, createBursts(4, 0, 0), 1);

        // --- INICIAR SCHEDULER ---
        Scheduler scheduler = new Scheduler();

        // Prueba Obligatoria 2: 2 procesos alternando (FCFS)
        // P2 entra primero, por lo tanto, ejecuta primero.
        scheduler.addProcess(p2); 
        scheduler.addProcess(p3); 

        System.out.println("\n*** INICIO SIMULACION FCFS (Tiempo 0) ***");
        
        // Bucle principal de simulación
        while (scheduler.runOneUnit()) {
            // El Scheduler ejecuta una unidad de tiempo en cada ciclo.
        }
        
        System.out.println("*** FIN SIMULACION FCFS ***\n");
    }
}