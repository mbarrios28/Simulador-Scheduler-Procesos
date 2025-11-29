import scheduler.Scheduler;
import process.Process;
import process.Burst;
import process.BurstResource;
import java.util.ArrayList;

public class App {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== STRESS TEST - 10 PROCESOS ===");
        
        Scheduler scheduler = new Scheduler();
        scheduler.setAlgorithm(Scheduler.Algorithm.RR);
        scheduler.setQuantum(3);

        // Crear 10 procesos con diferentes características
        ArrayList<Process> processes = new ArrayList<>();
        
        // Procesos con diferentes patrones de ejecución
        processes.add(new Process("P1", 0, createBursts(3, 2, 4), 1, 3));
        processes.add(new Process("P2", 1, createBursts(5, 1, 3), 2, 4));
        processes.add(new Process("P3", 2, createBursts(2, 3, 2), 1, 2));
        processes.add(new Process("P4", 3, createBursts(4, 0, 6), 3, 5));
        processes.add(new Process("P5", 4, createBursts(1, 4, 1), 2, 3));
        processes.add(new Process("P6", 5, createBursts(6, 2, 3), 1, 4));
        processes.add(new Process("P7", 6, createBursts(3, 1, 4), 2, 3));
        processes.add(new Process("P8", 7, createBursts(2, 3, 2), 1, 2));
        processes.add(new Process("P9", 8, createBursts(4, 2, 3), 3, 4));
        processes.add(new Process("P10", 9, createBursts(5, 1, 2), 2, 3));

        System.out.println("\n--- INICIO STRESS TEST ---");
        System.out.println("Total procesos: " + processes.size());
        System.out.println("Algoritmo: Round Robin");
        System.out.println("Quantum: " + 3);
        
        int maxCiclos = 100;
        int processIndex = 0;
        boolean trabajoPendiente = true;
        
        while ((trabajoPendiente || processIndex < processes.size()) && maxCiclos > 0) {
            int tiempoActual = scheduler.getTiempoGlobal();
            
            // Agregar procesos según su tiempo de llegada
            while (processIndex < processes.size()) {
                Process p = processes.get(processIndex);
                if (p.getT_arrival() <= tiempoActual) {
                    System.out.println("\n[STRESS TEST] LLEGADA: " + p.getPID() + " en t=" + tiempoActual);
                    scheduler.addProcess(p);
                    processIndex++;
                } else {
                    break;
                }
            }

            System.out.println("\n[CICLO " + (100 - maxCiclos + 1) + "] Tiempo: " + tiempoActual + 
                             ", Procesos pendientes: " + (processes.size() - processIndex));
            
            trabajoPendiente = scheduler.runOneUnit();
            
            Thread.sleep(500); // Pausa para ver los logs
            maxCiclos--;
            
            if (maxCiclos % 10 == 0) {
                System.out.println("\n[PROGRESO] Ciclos restantes: " + maxCiclos);
            }
        }
        
        System.out.println("\n--- FIN STRESS TEST ---");
        
        // Mostrar métricas finales
        System.out.println("\n=== MÉTRICAS FINALES STRESS TEST ===");
        for (Process p : processes) {
            mostrarMetricas(p);
        }

        // Verificación final
        verificarProcesos(processes);
        
        scheduler.shutdown();
        System.out.println("\n✅ Stress Test completado exitosamente");
    }

    private static ArrayList<Burst> createBursts(int cpu1, int io, int cpu2) {
        ArrayList<Burst> bursts = new ArrayList<>();
        if (cpu1 > 0) bursts.add(new Burst(BurstResource.CPU, cpu1));
        if (io > 0) bursts.add(new Burst(BurstResource.IO, io));
        if (cpu2 > 0) bursts.add(new Burst(BurstResource.CPU, cpu2));
        return bursts;
    }

    private static void mostrarMetricas(Process p) {
        int tRetorno = (p.getT_finish() != 0) ? p.getT_finish() - p.getT_arrival() : -1;
        double eficiencia = (p.getT_finish() != 0 && tRetorno > 0) ? 
            (double) p.getCpu_usage() / tRetorno * 100 : 0;
        
        System.out.printf("%s -> Estado: %-12s | Retorno: %2d | Espera: %2d | CPU: %2d | Eficiencia: %5.1f%%%n",
                p.getPID(), 
                p.getState(), 
                tRetorno,
                p.getT_wait(), 
                p.getCpu_usage(), 
                eficiencia);
    }
    
    private static void verificarProcesos(ArrayList<Process> processes) {
        int terminados = 0;
        int conProblemas = 0;
        
        for (Process p : processes) {
            if (p.getState() == process.ProcessState.TERMINATED) {
                terminados++;
            } else {
                conProblemas++;
                System.out.println("⚠️  " + p.getPID() + " no terminó - Estado: " + p.getState());
            }
        }
        
        System.out.println("\n=== VERIFICACIÓN FINAL ===");
        System.out.println("Procesos terminados: " + terminados + "/" + processes.size());
        System.out.println("Procesos con problemas: " + conProblemas);
        
        if (terminados == processes.size()) {
            System.out.println("🎉 TODOS LOS PROCESOS COMPLETARON EXITOSAMENTE");
        } else if (terminados >= processes.size() * 0.8) {
            System.out.println("✅ La mayoría de procesos completaron (80%+)");
        } else {
            System.out.println("❌ Menos del 80% de procesos completaron");
        }
    }
    /*public static void main(String[] args) throws InterruptedException {
        System.out.println("=== STRESS TEST - 10 PROCESOS ===");
        
        Scheduler scheduler = new Scheduler();
        scheduler.setAlgorithm(Scheduler.Algorithm.RR);
        scheduler.setQuantum(3);

        // Crear 10 procesos con diferentes características
        ArrayList<Process> processes = new ArrayList<>();
        
        // Procesos con diferentes patrones de ejecución
        processes.add(new Process("P1", 0, createBursts(3, 2, 4), 1, 3));
        processes.add(new Process("P2", 1, createBursts(5, 1, 3), 2, 4));
        processes.add(new Process("P3", 2, createBursts(2, 3, 2), 1, 2));
        processes.add(new Process("P4", 3, createBursts(4, 0, 6), 3, 5));
        processes.add(new Process("P5", 4, createBursts(1, 4, 1), 2, 3));
        processes.add(new Process("P6", 5, createBursts(6, 2, 3), 1, 4));
        processes.add(new Process("P7", 6, createBursts(3, 1, 4), 2, 3));
        processes.add(new Process("P8", 7, createBursts(2, 3, 2), 1, 2));
        processes.add(new Process("P9", 8, createBursts(4, 2, 3), 3, 4));
        processes.add(new Process("P10", 9, createBursts(5, 1, 2), 2, 3));

        System.out.println("\n--- INICIO STRESS TEST ---");
        
        int maxCiclos = 100;
        int processIndex = 0;
        boolean trabajoPendiente = true;
        
        while ((trabajoPendiente || processIndex < processes.size()) && maxCiclos > 0) {
            int tiempoActual = scheduler.getTiempoGlobal();
            
            // Agregar procesos según su tiempo de llegada
            while (processIndex < processes.size()) {
                Process p = processes.get(processIndex);
                if (p.getT_arrival() <= tiempoActual) {
                    System.out.println("\n[STRESS TEST] LLEGADA: " + p.getPID());
                    scheduler.addProcess(p);
                    processIndex++;
                } else {
                    break;
                }
            }

            System.out.println("\n[CICLO " + (100 - maxCiclos) + "] Procesos pendientes: " + 
                             (processes.size() - processIndex));
            
            trabajoPendiente = scheduler.runOneUnit();
            
            Thread.sleep(500); // Más rápido para el stress test
            maxCiclos--;
        }
        
        System.out.println("\n--- FIN STRESS TEST ---");
        
        // Mostrar métricas finales
        System.out.println("\n=== MÉTRICAS FINALES STRESS TEST ===");
        for (Process p : processes) {
            mostrarMetricas(p);
        }

        scheduler.shutdown();
    }

    private static ArrayList<Burst> createBursts(int cpu1, int io, int cpu2) {
        ArrayList<Burst> bursts = new ArrayList<>();
        if (cpu1 > 0) bursts.add(new Burst(BurstResource.CPU, cpu1));
        if (io > 0) bursts.add(new Burst(BurstResource.IO, io));
        if (cpu2 > 0) bursts.add(new Burst(BurstResource.CPU, cpu2));
        return bursts;
    }

    private static void mostrarMetricas(Process p) {
        System.out.println(p.getPID() + " -> Estado: " + p.getState() + 
                           " | Llegada: " + p.getT_arrival() +
                           " | Fin: " + p.getT_finish() +
                           " | T.Retorno: " + (p.getT_finish() - p.getT_arrival()) +
                           " | T.Espera: " + p.getT_wait() +
                           " | Uso CPU: " + p.getCpu_usage());
    }*/
}