package memory;
import memory.algoritmos.FIFO;
import memory.algoritmos.LRU;
import memory.algoritmos.Optimo;
import memory.algoritmos.ReplacementAlgorithm;
import java.util.*;

public class MemorySimulator {
    private static void runScenario(String title, ReplacementAlgorithm algorithm,
                                    Map<String, List<Integer>> futureSequences) {
        System.out.println("\n=== ESCENARIO: " + title + " ===");
        if (algorithm instanceof Optimo && futureSequences != null) {
            ((Optimo) algorithm).setFutureAccessSequences(futureSequences);
        }

        MemoryManager memory = new MemoryManager(3, algorithm);
        memory.createProcess("P1", 5);
        memory.createProcess("P2", 4);

        // Secuencia de accesos diseñada para diferenciar FIFO, LRU y Óptimo
        List<String> accessTrace = Arrays.asList(
            "P1:0", "P1:1", "P2:0", // llena memoria
            "P1:0", // refresca recencia Page0 para LRU
            "P2:1", // fault
            "P1:2", // fault
            "P2:0", // puede ser fault según reemplazo previo
            "P1:0", // hit preferente en LRU si conservada
            "P2:1", // hit (refresca)
            "P1:3"  // nuevo fault
        );

        System.out.println("--- Ejecutando trazas de accessPage ---");
        for (String op : accessTrace) {
            String[] parts = op.split(":");
            String pid = parts[0];
            int page = Integer.parseInt(parts[1]);
            MemoryManager.AccessResult r = memory.accessPage(pid, page);
            System.out.println(pid + ":" + page + " -> " + r.toString());
        }

        System.out.println("--- Estado final ---");
        memory.printMemoryStatus();
        memory.printAllPageTables();
        memory.printStatistics();
    }

    public static void main(String[] args) {
        System.out.println("=== MEMORY SIMULATOR ===");

        // Ejecutar casos de prueba para los tres algoritmos en un mismo programa
        runScenario("FIFO", new FIFO(), null);
        runScenario("LRU", new LRU(), null);
        Map<String, List<Integer>> futuras = new HashMap<>();
        futuras.put("P1", Arrays.asList(0,1,2,0,3));
        futuras.put("P2", Arrays.asList(0,1,0,1));
        runScenario("Óptimo", new Optimo(), futuras);
    }
}