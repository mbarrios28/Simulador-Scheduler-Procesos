import scheduler.Scheduler;
import process.Process;
import process.Burst;
import process.BurstResource;
import java.util.ArrayList;
import java.util.Scanner;

public class App {
    // Pon en 'false' si tu terminal no acepta entrada
    private static final boolean MODO_INTERACTIVO = true; 

    private static ArrayList<Burst> createBursts(int cpu1, int io, int cpu2) {
        ArrayList<Burst> bursts = new ArrayList<>();
        if (cpu1 > 0) bursts.add(new Burst(BurstResource.CPU, cpu1));
        if (io > 0) bursts.add(new Burst(BurstResource.IO, io));
        if (cpu2 > 0) bursts.add(new Burst(BurstResource.CPU, cpu2));
        return bursts;
    }

    public static void main(String[] args) throws InterruptedException {
        Scheduler scheduler = new Scheduler();

        System.out.println("========================================");
        System.out.println("   SIMULADOR SISTEMAS OPERATIVOS        ");
        System.out.println("   Datos de Prueba Oficiales            ");
        System.out.println("========================================");

        Scanner scanner = new Scanner(System.in); 

        if (MODO_INTERACTIVO) {
            System.out.println("Seleccione el algoritmo:");
            System.out.println("1. FCFS");
            System.out.println("2. SJF");
            System.out.println("3. Round Robin");
            System.out.print("Opcion: ");
            
            int opcion = 1;
            try { opcion = Integer.parseInt(scanner.nextLine()); } catch (Exception e) {}

            switch (opcion) {
                case 2: scheduler.setAlgorithm(Scheduler.Algorithm.SJF); break;
                case 3: 
                    scheduler.setAlgorithm(Scheduler.Algorithm.RR);
                    System.out.print("Quantum: ");
                    try { scheduler.setQuantum(Integer.parseInt(scanner.nextLine())); } 
                    catch (Exception e) { scheduler.setQuantum(2); }
                    break;
                default: scheduler.setAlgorithm(Scheduler.Algorithm.FCFS); break;
            }
        }

        // PROCESOS
        Process p1 = new Process("P1", 0, createBursts(4, 3, 5), 1, 4);
        Process p2 = new Process("P2", 2, createBursts(6, 2, 3), 2, 5);
        Process p3 = new Process("P3", 4, createBursts(8, 0, 0), 3, 6);

        ArrayList<Process> incomingProcesses = new ArrayList<>();
        incomingProcesses.add(p1);
        incomingProcesses.add(p2);
        incomingProcesses.add(p3);

        System.out.println("\n--- INICIO SIMULACION ---");
        
        boolean trabajoPendiente = true;
        int maxCiclos = 50; 
        
        while ((trabajoPendiente || !incomingProcesses.isEmpty()) && maxCiclos > 0) {
            int tiempoActual = scheduler.getTiempoGlobal();
            
            // Revisar llegadas
            for (int i = 0; i < incomingProcesses.size(); i++) {
                Process p = incomingProcesses.get(i);
                if (p.getT_arrival() <= tiempoActual) {
                    System.out.println("\n[SISTEMA] LLEGADA DE PROCESO: " + p.getPID());
                    scheduler.addProcess(p);
                    incomingProcesses.remove(i);
                    i--; 
                }
            }

            System.out.println("\n[CICLO RESTANTE " + maxCiclos + "]");
            trabajoPendiente = scheduler.runOneUnit();
            
            Thread.sleep(800); 
            maxCiclos--;
        }
        
        System.out.println("\n--- FIN SIMULACION ---");
        
        System.out.println("\nMETRICAS FINALES:");
        mostrarMetricas(p1);
        mostrarMetricas(p2);
        mostrarMetricas(p3);

        scheduler.shutdown();
        scanner.close(); 
    }

    private static void mostrarMetricas(Process p) {
        System.out.println(p.getPID() + " -> Estado: " + p.getState() + 
                           " | Llegada: " + p.getT_arrival() +
                           " | Fin: " + p.getT_finish() +
                           " | T.Retorno: " + (p.getT_finish() - p.getT_arrival()) +
                           " | T.Espera: " + p.getT_wait());
    }
}