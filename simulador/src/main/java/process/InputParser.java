package process;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class InputParser {

    private final String file;
    private ArrayList <String> procesos;
    private ArrayList <Process> list_process;

    public InputParser(String file) {
        this.file = file;
        this.procesos = new ArrayList<>();
        this.list_process = new ArrayList<>();
    }

    public void obtenerProcesos(){
        try (BufferedReader br = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(file)))){
            String line;
            while ((line = br.readLine())!= null){
                procesos.add(line);
            }

        } catch (IOException e){
            System.out.println("Error cargando el archivo");
        }
    }

    public void crearProcesos(){
        for (String proceso : procesos){
            list_process.add(crearProceso(proceso));
        }
        for (Process temp : list_process){
            System.out.println(temp);
        }
    }

    private Process crearProceso(String proceso){
        String[] partes = proceso.trim().split("\\s+");
        String PID = partes[0];
        String t_arrival_string = partes[1];
        String rafagas = partes[2];
        String pages_string = partes[3];
        String priority_string = partes[4];
        ArrayList<Burst> bursts = dividirRafagas(rafagas);
        int t_arrival = Integer.parseInt(t_arrival_string);
        int pages = Integer.parseInt(pages_string);
        int priority = Integer.parseInt(priority_string);
        Process process = new Process(PID, bursts, t_arrival, pages, priority);
        return process;
    }

    private ArrayList<Burst> dividirRafagas(String rafaga){
        String[] rafagas = rafaga.trim().split(",");
        ArrayList <Burst> bursts = new ArrayList<>();
        for (String temp : rafagas) {
            bursts.add(dividirRafaga(temp));
        }
        return bursts;
    }
    private Burst dividirRafaga(String burst){
        String resource = burst.substring(0,3);
        BurstResource res;
        if (resource.equals("CPU")){
            res = BurstResource.CPU;
        } else {
            res = BurstResource.IO;
        }
        int start = burst.indexOf("(");
        int end = burst.indexOf(")");
        int time = Integer.parseInt(burst.substring(start + 1, end));
        return new Burst(res, time);
    }
}
