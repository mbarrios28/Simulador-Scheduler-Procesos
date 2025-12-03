import java.util.ArrayList;

import display.SimulatorDisplay;
import memory.MemoryManager;
import memory.algoritmos.FIFO;
import process.Burst;
import process.BurstResource;
import process.Process;
import scheduler.Scheduler;

public class App {
    private static ArrayList<Burst> createBursts(int cpu1, int io, int cpu2) {
        ArrayList<Burst> bursts = new ArrayList<>();
        if (cpu1 > 0) bursts.add(new Burst(BurstResource.CPU, cpu1));
        if (io > 0) bursts.add(new Burst(BurstResource.IO, io));
        if (cpu2 > 0) bursts.add(new Burst(BurstResource.CPU, cpu2));
        return bursts;
    }

    public static void main(String[] args) throws InterruptedException {

        Scheduler scheduler = new Scheduler();

        MemoryManager memory = new MemoryManager(7, new FIFO()); 
        scheduler.setMemoryManager(memory);

        // Inicializar el sistema de visualización
        SimulatorDisplay display = new SimulatorDisplay(scheduler, memory);

        // Configurar algoritmo
        scheduler.setAlgorithm(Scheduler.Algorithm.RR);
        scheduler.setQuantum(2);

        Process p1 = new Process("P1", 0, createBursts(6, 2, 2), 1, 2);
        Process p2 = new Process("P2", 0, createBursts(5, 1, 3), 2, 2);
        Process p3 = new Process("P3", 0, createBursts(4, 2, 1), 3, 2);

        // Registrar procesos en el display
        display.addProcess(p1);
        display.addProcess(p2);
        display.addProcess(p3);

        ArrayList<Process> incoming = new ArrayList<>();
        incoming.add(p1);
        incoming.add(p2);
        incoming.add(p3);

        int maxCiclos = 50; 
        int ciclosSinTrabajo = 0;
        final int MAX_CICLOS_SIN_TRABAJO = 3;
        boolean hayTrabajo = true;
        
        System.out.println("\n" + "=".repeat(80));
        System.out.println("          INICIANDO SIMULACIÓN DE PLANIFICACIÓN Y MEMORIA");
        System.out.println("=".repeat(80) + "\n");
        Thread.sleep(2000);
        
        while (maxCiclos > 0) {
            int tiempoActual = scheduler.getTiempoGlobal();
            
            // Agregar procesos que llegan
            for (int i = 0; i < incoming.size(); i++) {
                Process p = incoming.get(i);
                if (p.getT_arrival() <= tiempoActual) {
                    System.out.println("\n[LLEGADA] Proceso " + p.getPID() + 
                        " ha llegado al sistema en T=" + tiempoActual);
                    scheduler.addProcess(p);
                    incoming.remove(i);
                    i--;
                }
            }

            // Ejecutar el ciclo (internamente captura el snapshot)
            hayTrabajo = scheduler.runOneUnit();
            
            // Obtener el snapshot capturado DURANTE la ejecución
            String[] stateSnapshot = scheduler.getLastExecutionSnapshot();
            
            // Determinar qué proceso ejecutó para el Gantt
            String executingPID = "NONE";
            if (stateSnapshot != null && stateSnapshot.length > 0) {
                executingPID = stateSnapshot[0];
            }
            
            // Registrar en Gantt el proceso que ejecutó
            display.recordGanttEntry(tiempoActual, executingPID);
            
            // Mostrar estado del sistema con la información capturada
            display.displayCycleStatus(tiempoActual, stateSnapshot);
            
            // Verificar si ya no hay trabajo
            if (!hayTrabajo && incoming.isEmpty()) {
                ciclosSinTrabajo++;
                System.out.println("[SISTEMA] Ciclo sin trabajo (" + ciclosSinTrabajo + 
                    "/" + MAX_CICLOS_SIN_TRABAJO + ")");
                
                if (ciclosSinTrabajo >= MAX_CICLOS_SIN_TRABAJO) {
                    System.out.println("\n[SISTEMA] " + MAX_CICLOS_SIN_TRABAJO + 
                        " ciclos consecutivos sin trabajo - Finalizando simulación");
                    break;
                }
            } else {
                ciclosSinTrabajo = 0; 
            }
            
            // Pausa para visualización (ajustar según preferencia)
            Thread.sleep(1500); // 1.5 segundos por ciclo
            
            maxCiclos--;
        }
        
        if (maxCiclos <= 0) {
            System.out.println("\n[SISTEMA] Alcanzado límite de ciclos máximos");
        }
        
        // Mostrar reporte final
        display.printFinalReport();
        
        System.out.println("\n" + "=".repeat(80));
        System.out.println("ESTADÍSTICAS DETALLADAS DE MEMORIA");
        System.out.println("=".repeat(80));
        memory.printStatistics();
        memory.printAllPageTables();
        
        scheduler.shutdown();
        
        Thread.sleep(1000);
        System.out.println("\n✓ Simulación completada exitosamente.");
        System.out.println("✓ Tiempo total: T=" + scheduler.getTiempoGlobal() + " ciclos");
    }
}