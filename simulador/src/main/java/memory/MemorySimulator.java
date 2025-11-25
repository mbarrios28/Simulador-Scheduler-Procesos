package memory;

public class MemorySimulator {
    public static void main(String[] args) {
        System.out.println("=== MEMORY SIMULATOR - WEEK 1 ===");
        
        // Configuración: 3 marcos, 2 procesos
        MemoryManager memory = new MemoryManager(3);
        memory.createProcess("P1", 4);
        memory.createProcess("P2", 3);
        
        System.out.println("--- Cargando páginas ---");
        memory.loadPage("P1", 0);
        memory.loadPage("P1", 1);
        memory.loadPage("P2", 0);
        
        memory.printMemoryStatus();
        memory.printAllPageTables();
        
        System.out.println("--- Provocando fallos de página ---");
        memory.loadPage("P2", 1);
        memory.loadPage("P1", 2);
        
        System.out.println("--- Verificación final ---");
        System.out.println("P1-Page0 cargada: " + memory.isPageLoaded("P1", 0));
        System.out.println("P1-Page3 cargada: " + memory.isPageLoaded("P1", 3));
        System.out.println("P2-Page1 cargada: " + memory.isPageLoaded("P2", 1));
        
        memory.printMemoryStatus();
¿    }
}