package process;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class InputParser {

    private final String filePath; // Ruta absoluta del archivo
    private ArrayList<String> lineasLeidas;
    private ArrayList<Process> list_process;

    public InputParser(String filePath) {
        this.filePath = filePath;
        this.lineasLeidas = new ArrayList<>();
        this.list_process = new ArrayList<>();
    }

    public void obtenerProcesos() throws IOException {
        File archivo = new File(filePath);

        // 1. Verificamos que el archivo exista antes de intentar leerlo
        if (!archivo.exists()) {
            throw new IOException("El archivo no existe en la ruta: " + filePath);
        }

        // 2. Usamos FileReader (para archivos físicos) en vez de getResourceAsStream
        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) { // Ignoramos líneas vacías
                    lineasLeidas.add(line);
                }
            }
        }
    }

    public void crearProcesos() {
        for (String linea : lineasLeidas) {
            try {
                list_process.add(crearProceso(linea));
            } catch (Exception e) {
                System.err.println("Error procesando línea: " + linea + " -> " + e.getMessage());
            }
        }
    }

    private Process crearProceso(String linea) {
        // Separamos por espacios en blanco (uno o más)
        String[] partes = linea.trim().split("\\s+");
        Process process;
        
        // CASO 1: Formato corto (Sin prioridad explícita)
        if (partes.length == 4) {
            String PID = partes[0];
            int t_arrival = Integer.parseInt(partes[1]);
            String rafagasString = partes[2];
            int pages = Integer.parseInt(partes[3]);
            
            ArrayList<Burst> bursts = dividirRafagas(rafagasString);
            
            process = new Process(PID, t_arrival, bursts, pages);
        } 
        // CASO 2: Formato largo (Con prioridad)
        else if (partes.length >= 5) {
            String PID = partes[0];
            int t_arrival = Integer.parseInt(partes[1]);
            String rafagasString = partes[2];
            int priority = Integer.parseInt(partes[3]);
            int pages = Integer.parseInt(partes[4]);
            
            ArrayList<Burst> bursts = dividirRafagas(rafagasString);
            
            process = new Process(PID, t_arrival, bursts, priority, pages);
        } else {
            throw new IllegalArgumentException("Formato de línea inválido (faltan datos): " + linea);
        }
        
        return process;
    }

    private ArrayList<Burst> dividirRafagas(String rafaga) {
        String[] rafagas = rafaga.trim().split(",");
        ArrayList<Burst> bursts = new ArrayList<>();
        for (String temp : rafagas) {
            bursts.add(dividirRafaga(temp));
        }
        return bursts;
    }

    private Burst dividirRafaga(String burst) {
        burst = burst.trim();
        String resource = burst.substring(0, 3);
        BurstResource res;
        
        if (resource.equalsIgnoreCase("CPU")) {
            res = BurstResource.CPU;
        } else {
            res = BurstResource.IO; 
        }
        
        int start = burst.indexOf("(");
        int end = burst.indexOf(")");
        
        if (start == -1 || end == -1) throw new IllegalArgumentException("Formato de ráfaga incorrecto");
        
        int time = Integer.parseInt(burst.substring(start + 1, end));
        return new Burst(res, time);
    }

    public ArrayList<Process> get_process() {
        return this.list_process;
    }
}