package display;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import memory.Frame;
import memory.MemoryManager;
import memory.PageTable;
import process.Process;
import process.ProcessState;
import scheduler.Scheduler;
import threads.IOManager;

public class SimulatorDisplay {
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_BOLD = "\u001B[1m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_BLUE = "\u001B[34m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_MAGENTA = "\u001B[35m";
    
    private Scheduler scheduler;
    private MemoryManager memoryManager;
    private IOManager ioManager;
    private List<Process> allProcesses;
    private Map<Integer, String> ganttChart;
    
    public SimulatorDisplay(Scheduler scheduler, MemoryManager memoryManager) {
        this.scheduler = scheduler;
        this.memoryManager = memoryManager;
        this.ioManager = scheduler.getIOManager();
        this.allProcesses = new ArrayList<>();
        this.ganttChart = new LinkedHashMap<>();
    }
    
    public void addProcess(Process p) {
        if (!allProcesses.contains(p)) {
            allProcesses.add(p);
        }
    }
    
    public void displayCycleStatus(int cycle) {
        clearScreen();
        printHeader(cycle);
        printProcessQueues(null);
        printMemoryStatus();
        printGanttChart();
        printMetrics();
        printSeparator();
    }
    
    public void displayCycleStatus(int cycle, String[] stateSnapshot) {
        clearScreen();
        printHeader(cycle);
        printProcessQueues(stateSnapshot);
        printMemoryStatus();
        printGanttChart();
        printMetrics();
        printSeparator();
    }
    
    private void clearScreen() {
        // Para terminal ANSI
        System.out.print("\033[H\033[2J");
        System.out.flush();
        // Si no funciona, usar líneas en blanco
        for (int i = 0; i < 2; i++) System.out.println();
    }
    
    private void printHeader(int cycle) {
        String algoName = getAlgorithmName();
        System.out.println(ANSI_BOLD + ANSI_CYAN + 
            "╔══════════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║          SIMULADOR DE PLANIFICACIÓN Y GESTIÓN DE MEMORIA                    ║");
        System.out.println(String.format("║          Ciclo: T=%-4d  Algoritmo: %-43s║", cycle, algoName));
        System.out.println("╚══════════════════════════════════════════════════════════════════════════════╝" + 
            ANSI_RESET);
        System.out.println();
    }
    
    private void printProcessQueues(String[] stateSnapshot) {
        System.out.println(ANSI_BOLD + ANSI_YELLOW + "┌─ ESTADO DE COLAS DE PROCESOS " + 
            "─".repeat(47) + "┐" + ANSI_RESET);
        
        // Proceso en Ejecución
        // stateSnapshot: [0]=PID, [1]=Estado, [2]=TipoRafaga, [3]=Restante, [4]=Total
        
        if (stateSnapshot != null && stateSnapshot.length >= 5) {
            String pid = stateSnapshot[0];
            String burstType = stateSnapshot[2];
            String remaining = stateSnapshot[3];
            String total = stateSnapshot[4];
            
            System.out.println(ANSI_GREEN + "  ► EJECUTANDO: " + ANSI_RESET + 
                ANSI_BOLD + ANSI_GREEN + pid + ANSI_RESET);
            
            System.out.println("    └─ Estado: RUNNING, Ráfaga actual: " + burstType +
                " (" + remaining + "/" + total + " ciclos)");
        } else {
            System.out.println(ANSI_GREEN + "  ► EJECUTANDO: " + ANSI_RESET + 
                ANSI_RED + "IDLE (sin procesos)" + ANSI_RESET);
        }
        System.out.println();
        
        // Cola de Listos
        List<Process> readyProcesses = scheduler.getReadyProcesses();
        System.out.println(ANSI_BLUE + "  ► LISTOS (Ready Queue): " + ANSI_RESET + 
            "[" + readyProcesses.size() + " procesos]");
        
        if (readyProcesses.isEmpty()) {
            System.out.println("    (Cola vacía)");
        } else {
            for (Process p : readyProcesses) {
                String nextBurst = p.isFinished() ? "NINGUNA" : 
                    p.getBurst().getResource() + "(" + p.getBurst().getTime_remaining() + ")";
                System.out.println("    • " + ANSI_BOLD + p.getPID() + ANSI_RESET + 
                    " - Prioridad: " + p.getPriority() + 
                    ", Esperando: " + p.getT_wait() + " ciclos" +
                    ", Próxima ráfaga: " + nextBurst);
            }
        }
        System.out.println();
        
        // Procesos Bloqueados
        System.out.println(ANSI_YELLOW + "  ► BLOQUEADOS: " + ANSI_RESET);
        
        boolean hayBloqueados = false;
        for (Process p : allProcesses) {
            if (p.getState() == ProcessState.BLOCKED_IO) {
                hayBloqueados = true;
                System.out.println("    • " + ANSI_BOLD + p.getPID() + ANSI_RESET + 
                    " - " + ANSI_YELLOW + "[BLOQUEADO por E/S]" + ANSI_RESET +
                    " - Operación en progreso");
            } else if (p.getState() == ProcessState.BLOCKED_MEM) {
                hayBloqueados = true;
                System.out.println("    • " + ANSI_BOLD + p.getPID() + ANSI_RESET + 
                    " - " + ANSI_RED + "[BLOQUEADO por MEMORIA]" + ANSI_RESET +
                    " - Esperando carga de páginas");
            }
        }
        
        if (!hayBloqueados) {
            System.out.println("    (No hay procesos bloqueados)");
        }
        System.out.println();
        
        // Procesos Terminados
        System.out.println(ANSI_MAGENTA + "  ► TERMINADOS: " + ANSI_RESET);
        boolean hayTerminados = false;
        for (Process p : allProcesses) {
            if (p.getState() == ProcessState.TERMINATED) {
                hayTerminados = true;
                int turnaround = p.getT_finish() - p.getT_arrival();
                System.out.println("    • " + ANSI_BOLD + p.getPID() + ANSI_RESET + 
                    " - Completado en T=" + p.getT_finish() +
                    ", Turnaround: " + turnaround + " ciclos");
            }
        }
        if (!hayTerminados) {
            System.out.println("    (Ningún proceso terminado aún)");
        }
        
        System.out.println(ANSI_YELLOW + "└" + "─".repeat(78) + "┘" + ANSI_RESET);
        System.out.println();
    }
    
    private void printMemoryStatus() {
        System.out.println(ANSI_BOLD + ANSI_CYAN + "┌─ ESTADO DE MEMORIA (PAGINACIÓN) " + 
            "─".repeat(44) + "┐" + ANSI_RESET);
        
        // Resumen de marcos
        int totalFrames = memoryManager.getTotalFrames();
        int freeFrames = memoryManager.getFreeFramesCount();
        int usedFrames = totalFrames - freeFrames;
        
        System.out.println(ANSI_CYAN + "  ► MARCOS DE MEMORIA:" + ANSI_RESET);
        System.out.print("    ");
        
        List<Frame> frames = memoryManager.getPhysicalMemory();
        for (int i = 0; i < totalFrames; i++) {
            Frame frame = frames.get(i);
            String content = memoryManager.findPageInFrame(frame);
            
            if (content.equals("FREE")) {
                System.out.print("[" + i + ": " + ANSI_GREEN + "LIBRE" + ANSI_RESET + "] ");
            } else {
                System.out.print("[" + i + ": " + ANSI_YELLOW + content + ANSI_RESET + "] ");
            }
            
            // Salto de línea cada 4 frames para mejor visualización
            if ((i + 1) % 4 == 0 && i < totalFrames - 1) {
                System.out.print("\n    ");
            }
        }
        System.out.println();
        System.out.println("    Ocupación: " + usedFrames + "/" + totalFrames + 
            " (" + String.format("%.1f", (usedFrames * 100.0 / totalFrames)) + "%)");
        System.out.println();
        
        // Tablas de páginas por proceso
        System.out.println(ANSI_CYAN + "  ► TABLAS DE PÁGINAS:" + ANSI_RESET);
        
        boolean hayTablas = false;
        for (Process p : allProcesses) {
            if (p.getState() != ProcessState.NEW) {
                hayTablas = true;
                PageTable pt = memoryManager.getPageTable(p.getPID());
                if (pt != null) {
                    System.out.println("    • " + ANSI_BOLD + p.getPID() + ANSI_RESET + 
                        " (" + p.getPages() + " páginas):");
                    System.out.print("      ");
                    
                    for (int i = 0; i < p.getPages(); i++) {
                        if (pt.isPageLoaded(i)) {
                            int frameNum = pt.getEntry(i).getFrameNumber();
                            System.out.print(ANSI_GREEN + "P" + i + "→F" + frameNum + ANSI_RESET + " ");
                        } else {
                            System.out.print(ANSI_RED + "P" + i + "→✗" + ANSI_RESET + " ");
                        }
                    }
                    System.out.println();
                    
                    // Estadísticas de memoria del proceso
                    int faults = memoryManager.getPageFaults(p.getPID());
                    int replacements = memoryManager.getReplacements(p.getPID());
                    System.out.println("      Fallos: " + faults + ", Reemplazos: " + replacements);
                }
            }
        }
        
        if (!hayTablas) {
            System.out.println("    (No hay procesos con memoria asignada)");
        }
        
        System.out.println(ANSI_CYAN + "└" + "─".repeat(78) + "┘" + ANSI_RESET);
        System.out.println();
    }
    
    private void printGanttChart() {
        System.out.println(ANSI_BOLD + ANSI_MAGENTA + "┌─ DIAGRAMA DE GANTT " + 
            "─".repeat(57) + "┐" + ANSI_RESET);
        
        int currentCycle = scheduler.getTiempoGlobal();
        int startCycle = Math.max(0, currentCycle - 15); // Mostrar últimos 15 ciclos
        
        System.out.print("  T: ");
        for (int i = startCycle; i <= currentCycle; i++) {
            System.out.print(String.format("%4d", i));
        }
        System.out.println();
        
        System.out.print("     ");
        for (int i = startCycle; i <= currentCycle; i++) {
            String pid = ganttChart.getOrDefault(i, "IDLE");
            if (pid.equals("IDLE")) {
                System.out.print(ANSI_RED + " ─── " + ANSI_RESET);
            } else {
                // Extraer solo el número del PID (P1 -> 1)
                String shortPid = pid.replace("P", "");
                System.out.print(ANSI_GREEN + " " + 
                    String.format("%-3s", shortPid) + " " + ANSI_RESET);
            }
        }
        System.out.println();
        
        System.out.println(ANSI_MAGENTA + "└" + "─".repeat(78) + "┘" + ANSI_RESET);
        System.out.println();
    }
    
    private void printMetrics() {
        System.out.println(ANSI_BOLD + ANSI_BLUE + "┌─ MÉTRICAS DEL SISTEMA " + 
            "─".repeat(54) + "┐" + ANSI_RESET);
        
        int totalProcesses = 0;
        int completedProcesses = 0;
        int totalWaitTime = 0;
        int totalTurnaroundTime = 0;
        int totalCPUUsage = 0;
        int totalPageFaults = 0;
        int totalReplacements = 0;
        
        for (Process p : allProcesses) {
            if (p.getState() != ProcessState.NEW) {
                totalProcesses++;
                totalWaitTime += p.getT_wait();
                totalCPUUsage += p.getCpu_usage();
                totalPageFaults += memoryManager.getPageFaults(p.getPID());
                totalReplacements += memoryManager.getReplacements(p.getPID());
                
                if (p.getState() == ProcessState.TERMINATED) {
                    completedProcesses++;
                    totalTurnaroundTime += (p.getT_finish() - p.getT_arrival());
                }
            }
        }
        
        int currentCycle = scheduler.getTiempoGlobal();
        double cpuUtilization = currentCycle > 0 ? (totalCPUUsage * 100.0 / currentCycle) : 0;
        
        System.out.println(ANSI_BLUE + "  ► PLANIFICACIÓN:" + ANSI_RESET);
        System.out.println("    • Algoritmo activo: " + ANSI_BOLD + 
            getAlgorithmName() + ANSI_RESET);
        System.out.println("    • Procesos totales: " + totalProcesses);
        System.out.println("    • Procesos completados: " + completedProcesses);
        
        if (totalProcesses > 0) {
            System.out.println("    • Tiempo promedio de espera: " + 
                String.format("%.2f", totalWaitTime / (double) totalProcesses) + " ciclos");
        }
        
        if (completedProcesses > 0) {
            System.out.println("    • Tiempo promedio de retorno: " + 
                String.format("%.2f", totalTurnaroundTime / (double) completedProcesses) + " ciclos");
        }
        
        System.out.println("    • Utilización de CPU: " + 
            String.format("%.2f", cpuUtilization) + "%");
        System.out.println();
        
        System.out.println(ANSI_BLUE + "  ► MEMORIA:" + ANSI_RESET);
        System.out.println("    • Algoritmo de reemplazo: " + 
            memoryManager.getReplacementAlgorithm().getName());
        System.out.println("    • Total fallos de página: " + totalPageFaults);
        System.out.println("    • Total reemplazos: " + totalReplacements);
        
        System.out.println(ANSI_BLUE + "└" + "─".repeat(78) + "┘" + ANSI_RESET);
        System.out.println();
    }
    
    private void printSeparator() {
        System.out.println(ANSI_CYAN + "═".repeat(80) + ANSI_RESET);
        System.out.println();
    }
    
    public void recordGanttEntry(int cycle, String processId) {
        ganttChart.put(cycle, processId);
    }
    
    private String getAlgorithmName() {
        Scheduler.Algorithm algo = scheduler.getCurrentAlgorithm();
        switch (algo) {
            case FCFS:
                return "First Come First Served (FCFS)";
            case SJF:
                return "Shortest Job First (SJF)";
            case RR:
                return "Round Robin (Q=" + scheduler.getQuantum() + ")";
            case PRIORITY:
                return "Priority (Preemptive)";
            default:
                return "Unknown";
        }
    }
    
    public void printFinalReport() {
        System.out.println("\n\n");
        System.out.println(ANSI_BOLD + ANSI_GREEN + 
            "╔══════════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                         REPORTE FINAL DE SIMULACIÓN                          ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════════════════╝" + 
            ANSI_RESET);
        System.out.println();
        
        // Métricas finales detalladas por proceso
        System.out.println(ANSI_BOLD + "RESUMEN POR PROCESO:" + ANSI_RESET);
        System.out.println("─".repeat(80));
        
        for (Process p : allProcesses) {
            System.out.println(ANSI_BOLD + p.getPID() + ":" + ANSI_RESET);
            System.out.println("  Estado final: " + p.getState());
            System.out.println("  Prioridad: " + p.getPriority());
            System.out.println("  Páginas: " + p.getPages());
            System.out.println("  Llegada: T=" + p.getT_arrival());
            System.out.println("  Inicio: T=" + (p.getT_start() != -1 ? p.getT_start() : "No inició"));
            System.out.println("  Fin: T=" + (p.getT_finish() > 0 ? p.getT_finish() : "No terminó"));
            System.out.println("  Tiempo de espera: " + p.getT_wait() + " ciclos");
            System.out.println("  Uso de CPU: " + p.getCpu_usage() + " ciclos");
            
            if (p.getT_start() != -1 && p.getT_finish() > 0) {
                int turnaround = p.getT_finish() - p.getT_arrival();
                System.out.println("  Tiempo de retorno: " + turnaround + " ciclos");
            }
            
            System.out.println("  Fallos de página: " + memoryManager.getPageFaults(p.getPID()));
            System.out.println("  Reemplazos: " + memoryManager.getReplacements(p.getPID()));
            System.out.println();
        }
        
        System.out.println("─".repeat(80));
        
        // Métricas globales
        int totalWait = 0;
        int totalTurnaround = 0;
        int completed = 0;
        int totalCPU = 0;
        
        for (Process p : allProcesses) {
            if (p.getState() != ProcessState.NEW) {
                totalWait += p.getT_wait();
                totalCPU += p.getCpu_usage();
                if (p.getState() == ProcessState.TERMINATED) {
                    completed++;
                    totalTurnaround += (p.getT_finish() - p.getT_arrival());
                }
            }
        }
        
        System.out.println(ANSI_BOLD + "\nMÉTRICAS GLOBALES:" + ANSI_RESET);
        System.out.println("  Tiempo total de simulación: " + scheduler.getTiempoGlobal() + " ciclos");
        System.out.println("  Procesos completados: " + completed + "/" + allProcesses.size());
        
        if (allProcesses.size() > 0) {
            System.out.println("  Tiempo promedio de espera: " + 
                String.format("%.2f", totalWait / (double) allProcesses.size()) + " ciclos");
        }
        
        if (completed > 0) {
            System.out.println("  Tiempo promedio de retorno: " + 
                String.format("%.2f", totalTurnaround / (double) completed) + " ciclos");
        }
        
        int totalCycles = scheduler.getTiempoGlobal();
        if (totalCycles > 0) {
            System.out.println("  Utilización de CPU: " + 
                String.format("%.2f", (totalCPU * 100.0 / totalCycles)) + "%");
        }
    }
    
    public void printStateTransition(String processId, ProcessState oldState, 
                                     ProcessState newState, String reason) {
        String timestamp = "[T=" + scheduler.getTiempoGlobal() + "]";
        
        System.out.println(ANSI_BOLD + ANSI_YELLOW + "\n>>> TRANSICIÓN DE ESTADO <<<" + ANSI_RESET);
        System.out.println(timestamp + " " + ANSI_BOLD + processId + ANSI_RESET + 
            ": " + oldState + " → " + ANSI_GREEN + newState + ANSI_RESET);
        System.out.println("    Razón: " + reason);
        System.out.println();
    }
}