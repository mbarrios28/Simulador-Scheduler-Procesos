import java.util.ArrayList;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import display.SimulatorGUI;
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

    public static void main(String[] args) {
        // Configuración por defecto
        SwingUtilities.invokeLater(() -> {
            try {
                // 1. Crear componentes del sistema
                Scheduler scheduler = new Scheduler();
                MemoryManager memory = new MemoryManager(7, new FIFO());
                
                // 2. Configurar
                scheduler.setMemoryManager(memory);
                scheduler.setAlgorithm(Scheduler.Algorithm.RR);
                scheduler.setQuantum(2);
                
                // 3. Crear procesos
                ArrayList<Process> processes = new ArrayList<>();
                processes.add(new Process("P1", 0, createBursts(6, 2, 2), 1, 2));
                processes.add(new Process("P2", 0, createBursts(5, 1, 3), 2, 2));
                processes.add(new Process("P3", 0, createBursts(4, 2, 1), 3, 2));
                
                // 4. Mostrar GUI
                SimulatorGUI gui = new SimulatorGUI(scheduler, memory, processes);
                gui.show();
                
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null,
                    "Error al iniciar: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        });
    }
}