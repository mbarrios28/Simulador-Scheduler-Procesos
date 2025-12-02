package memory;
import memory.algoritmos.FIFO;
import memory.algoritmos.LRU;
import memory.algoritmos.Optimo;
import memory.algoritmos.ReplacementAlgorithm;
import java.util.*;

public class MemorySimulator {
    private static void runScenario(String title, ReplacementAlgorithm algorithm,
                                    Map<String, List<Integer>> futureSequences) {
        System.out.println("\nESCENARIO: " + title);
        if (algorithm instanceof Optimo && futureSequences != null) {
            ((Optimo) algorithm).setFutureAccessSequences(futureSequences);
        }

        MemoryManager memory = new MemoryManager(3, algorithm);
        memory.createProcess("P1", 4);
        memory.createProcess("P2", 3);

        System.out.println("\nCargando páginas");
        memory.loadPage("P1", 0);
        memory.loadPage("P1", 1);
        memory.loadPage("P2", 0);

        memory.printMemoryStatus();
        memory.printAllPageTables();

        System.out.println("\nProvocando fallos de página");
        memory.loadPage("P2", 1);
        memory.loadPage("P1", 0);
        memory.loadPage("P1", 2);

    System.out.println("\nVerificación final ");
        System.out.println("P1-Page0 cargada: " + memory.isPageLoaded("P1", 0));
        System.out.println("P1-Page3 cargada: " + memory.isPageLoaded("P1", 3));
        System.out.println("P2-Page1 cargada: " + memory.isPageLoaded("P2", 1));

        memory.printMemoryStatus();
        memory.printAllPageTables();
        memory.printStatistics();
    }

    public static void main(String[] args) {
        System.out.println("MEMORY SIMULATOR:");

        runScenario("FIFO", new FIFO(), null);
        runScenario("LRU", new LRU(), null);
        Map<String, List<Integer>> futuras = new HashMap<>();
        futuras.put("P1", Arrays.asList(0, 1, 0, 2));  
        futuras.put("P2", Arrays.asList(0, 1)); 
        runScenario("Óptimo", new Optimo(), futuras);
    }
}