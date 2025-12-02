import java.util.ArrayList;

import memory.MemoryManager;
import memory.algoritmos.FIFO;
import process.Burst;
import process.BurstResource;
import process.Process;
import scheduler.Scheduler;

public class App {
    // Helper para crear ráfagas: CPU -> IO -> CPU
    private static ArrayList<Burst> createBursts(int cpu1, int io, int cpu2) {
        ArrayList<Burst> bursts = new ArrayList<>();
        if (cpu1 > 0) bursts.add(new Burst(BurstResource.CPU, cpu1));
        if (io > 0) bursts.add(new Burst(BurstResource.IO, io));
        if (cpu2 > 0) bursts.add(new Burst(BurstResource.CPU, cpu2));
        return bursts;
    }

    public static void main(String[] args) throws InterruptedException {

        Scheduler scheduler = new Scheduler();

        // CONFIGURACIÓN MEMORIA LIMITADA
        MemoryManager memory = new MemoryManager(7, new FIFO()); 
        scheduler.setMemoryManager(memory);

        // Configuracion en PRIORITY para ver el efecto de las prioridades
        scheduler.setAlgorithm(Scheduler.Algorithm.RR);
        scheduler.setQuantum(2);

        // Procesos prueba
        Process p1 = new Process("P1", 0, createBursts(6, 2, 2), 1, 2);   // Largo: 6 unidades CPU
        Process p2 = new Process("P2", 0, createBursts(5, 1, 3), 1, 2);   // Mediano: 5 unidades CPU  
        Process p3 = new Process("P3", 0, createBursts(4, 2, 1), 1, 2);   // Corto: 4 unidades CPU

        ArrayList<Process> incoming = new ArrayList<>();
        incoming.add(p1);
        incoming.add(p2);
        incoming.add(p3);

        // CONTROL DE SIMULACIÓN
        int maxCiclos = 50; // Suficiente para que terminen
        int ciclosSinTrabajo = 0;
        final int MAX_CICLOS_SIN_TRABAJO = 3;
        boolean hayTrabajo = true;
        
        // BUCLE PRINCIPAL DE SIMULACIÓN
        while (maxCiclos > 0) {
            int tiempoActual = scheduler.getTiempoGlobal();
            
            // 1. MANEJAR LLEGADAS DE PROCESOS
            for (int i = 0; i < incoming.size(); i++) {
                Process p = incoming.get(i);
                if (p.getT_arrival() <= tiempoActual) {
                    scheduler.addProcess(p);
                    incoming.remove(i);
                    i--;
                }
            }

            // 2. EJECUTAR UN CICLO DEL SCHEDULER
            hayTrabajo = scheduler.runOneUnit();
            
            // 3. CONTROL DE FINALIZACIÓN
            if (!hayTrabajo && incoming.isEmpty()) {
                ciclosSinTrabajo++;
                System.out.println("[SISTEMA] Ciclo sin trabajo (" + ciclosSinTrabajo + 
                                 "/" + MAX_CICLOS_SIN_TRABAJO + ")");
                
                if (ciclosSinTrabajo >= MAX_CICLOS_SIN_TRABAJO) {
                    System.out.println("\n[SISTEMA] " + MAX_CICLOS_SIN_TRABAJO + 
                                     " ciclos sin trabajo - Finalizando");
                    break;
                }
            } else {
                ciclosSinTrabajo = 0; // Reiniciar contador
            }
            
            // 4. PAUSA PARA VISUALIZACIÓN (OPCIONAL - ajusta tiempo)
            // Para simulación más rápida: 300ms, para más lenta: 800ms
            Thread.sleep(500); // Medio segundo entre ciclos para leer
            
            // 5. MENSAJE DE PROGRESO
            maxCiclos--;
            if (maxCiclos % 10 == 0) {
                System.out.println("\n[PROGRESO] Ciclos restantes: " + maxCiclos + 
                                 ", Tiempo simulado: T=" + (tiempoActual + 1));
            }
        }
        
        // FINALIZACIÓN
        if (maxCiclos <= 0) {
            System.out.println("\n[SISTEMA] Alcanzado límite de " + 50 + " ciclos");
        }
        
        System.out.println("\n--- FIN SIMULACIÓN ---");
        System.out.println("Tiempo final: T=" + scheduler.getTiempoGlobal());
        
        // ESTADÍSTICAS
        System.out.println("\n=== ESTADÍSTICAS DE MEMORIA ===");
        memory.printStatistics();
        
        System.out.println("\n=== MÉTRICAS DE PROCESOS ===");
        mostrarMetricasProceso(p1, scheduler);
        mostrarMetricasProceso(p2, scheduler);
        
        // APAGADO ORDENADO
        scheduler.shutdown();
        
        // PAUSA FINAL
        Thread.sleep(1000);
        System.out.println("\nSimulación completada exitosamente.");
    }
    
    // MÉTODO AUXILIAR PARA MÉTRICAS
    private static void mostrarMetricasProceso(Process p, Scheduler scheduler) {
        System.out.println("\n" + p.getPID() + ":");
        System.out.println("   Prioridad: " + p.getPriority());
        System.out.println("   Estado final: " + p.getState());
        System.out.println("   Páginas: " + p.getPages());
        System.out.println("   Llegada: T=" + p.getT_arrival());
        System.out.println("   Inicio: T=" + (p.getT_start() != -1 ? p.getT_start() : "No inició"));
        System.out.println("   Fin: T=" + (p.getT_finish() > 0 ? p.getT_finish() : "No terminó"));
        System.out.println("   Espera: " + p.getT_wait() + " ciclos");
        System.out.println("   Uso CPU: " + p.getCpu_usage() + " ciclos");
        
        if (p.getT_start() != -1 && p.getT_finish() > 0) {
            int turnaround = p.getT_finish() - p.getT_start();
            System.out.println("   Turnaround: " + turnaround + " ciclos");
            System.out.println("   Tiempo en sistema: " + (p.getT_finish() - p.getT_arrival()) + " ciclos");
        }
        
        // MOSTRAR RÁFAGAS COMPLETADAS
        System.out.println("   Ráfagas: " + p.getBursts().size());
        for (int i = 0; i < p.getBursts().size(); i++) {
            Burst b = p.getBursts().get(i);
            System.out.println("     " + i + ". " + b.getResource() + "(" + b.getTime_total() + ")");
        }
    }
}