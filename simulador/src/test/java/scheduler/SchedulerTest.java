package scheduler;

import java.util.ArrayList;

import org.junit.Test;

import process.InputParser;
import process.Process;

public class SchedulerTest {

    @Test
    public void testPrueba1() {
        // Cargar el archivo de prueba
        InputParser parser = new InputParser("/prueba1.txt");
        parser.obtenerProcesos();
        parser.crearProcesos();

        // Obtener procesos
        ArrayList<Process> procesos = parser.get_process();

        System.out.println("=== Procesos cargados desde prueba1.txt ===");
        for (Process p : procesos) {
            System.out.println(p);
        }

        // Crear scheduler (lo que ya tienes implementado)
        Scheduler scheduler = new Scheduler();

        // Agregar procesos
        for (Process p : procesos) {
            scheduler.addProcess(p);
        }

        System.out.println("\n===== INICIO DE LA SIMULACIÓN =====\n");

        // Ejecutar simulación
        while (scheduler.runOneUnit()) {
            // runOneUnit ya imprime su propio estado
        }

        System.out.println("\n===== FIN DE LA SIMULACIÓN =====");
    }
}

