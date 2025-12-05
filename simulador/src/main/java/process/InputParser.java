package process;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

        if (!archivo.exists()) {
            throw new IOException("El archivo no existe en la ruta: " + filePath);
        }

        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) {
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
        List<Integer> pageSequence = null;
        
        // Buscar SEQ[] en la línea
        int seqIndex = -1;
        for (int i = 0; i < partes.length; i++) {
            if (partes[i].startsWith("SEQ[")) {
                seqIndex = i;
                pageSequence = parsePageSequence(partes[i]);
                break;
            }
        }
        
        // CASO 1: Formato corto (Sin prioridad explícita)
        // P1 0 CPU(5) 3 [SEQ[...]]
        if (partes.length == 4 || (partes.length == 5 && seqIndex == 4)) {
            String PID = partes[0];
            int t_arrival = Integer.parseInt(partes[1]);
            String rafagasString = partes[2];
            int pages = Integer.parseInt(partes[3]);
            
            ArrayList<Burst> bursts = dividirRafagas(rafagasString);
            
            process = new Process(PID, t_arrival, bursts, pages);
        } 
        // CASO 2: Formato largo (Con prioridad)
        // P1 0 CPU(5) 1 3 [SEQ[...]]
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
        
        // Asignar secuencia si existe
        if (pageSequence != null) {
            process.setFuturePageSequence(pageSequence);
            System.out.println("[InputParser] " + process.getPID() + " cargada con SEQ[] de " + pageSequence.size() + " accesos");
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
    
    private List<Integer> parsePageSequence(String seqString) {
        try {
            if (!seqString.startsWith("SEQ[") || !seqString.endsWith("]")) {
                System.err.println("[InputParser] Formato SEQ[] inválido: " + seqString);
                return null;
            }
            
            String content = seqString.substring(4, seqString.length() - 1);
            
            if (content.trim().isEmpty()) {
                return new ArrayList<>();
            }
            
            String[] pageStrings = content.split(",");
            List<Integer> sequence = new ArrayList<>();
            
            for (String pageStr : pageStrings) {
                sequence.add(Integer.parseInt(pageStr.trim()));
            }
            
            return sequence;
            
        } catch (Exception e) {
            System.err.println("[InputParser] Error parseando SEQ[]: " + seqString + " -> " + e.getMessage());
            return null;
        }
    }
}