package process;
import java.util.ArrayList;

public class Process {
    // Atributos
    private String PID; //PID del proceso
    private int t_arrival; //Segundo en el que llega el proceso
    private ArrayList <Burst> bursts; //Lista de las rafagas que tiene el proceso
    private int pages; //Páginas de memoria virtual que necesita el proceso
    private ProcessState state; //Estado en el que se encuentra el proceso (Nuevo, Listo, Ejecutando, Bloqueado (por memoria o E/S), Terminado)
    private int ind_burst; //Indice de rafaga actual
    private int priority; //Prioiridad de un proceso, no todos la usan
    
    //Métricas
    private int t_start = -1;
    private int t_finish;
    private int t_wait;
    private int cpu_usage;

    //Constructores
    public Process(String PID, int t_arrival, ArrayList<Burst> bursts, int pages) {
        this.PID = PID;
        this.bursts = bursts;
        this.pages = pages;
        this.t_arrival = t_arrival;
        this.priority = -1; 
        this.ind_burst = 0;
        this.state = ProcessState.NEW;
    }

    public Process(String PID, int t_arrival, ArrayList<Burst> bursts, int priority, int pages) {
        this.PID = PID;
        this.bursts = bursts;
        this.pages = pages;
        this.priority = priority;
        this.t_arrival = t_arrival;
        this.ind_burst = 0;
        this.state = ProcessState.NEW;
    }
    
    //Métodos para la gestión del proceso
    public Burst getBurst(){
        return this.bursts.get(this.ind_burst);
    }

    public void nextBurst(){
        this.ind_burst++;
        //Verificamos si terminó el proceso
        if (isFinished()){
            this.state = ProcessState.TERMINATED;
        } else {
            if (isCurrentBurstCPU()){
                this.state = ProcessState.READY;
            } else {
                this.state = ProcessState.BLOCKED_IO;
            }
        }
    }

    public boolean isCurrentBurstCPU(){
        Burst temp = getBurst();
        return temp.getResource().compareTo(BurstResource.CPU) == 0;
    }

    public boolean isCurrentBurstIO(){
        Burst temp = getBurst();
        return temp.getResource().compareTo(BurstResource.IO) == 0;
    }

    public boolean isFinished(){
        return this.ind_burst >= bursts.size();
    }

    //Setters y getters

    public String getPID() {
        return PID;
    }

    public int getT_arrival() {
        return t_arrival;
    }

    public ArrayList<Burst> getBursts() {
        return bursts;
    }

    public int getPages() {
        return pages;
    }

    public ProcessState getState() {
        return state;
    }

    public int getPriority() {
        return priority;
    }

    public void setState(ProcessState state) {
        this.state = state;
    }

    //toString()
    @Override
    public String toString() {

        String resultado = "Proceso {\n" +
                "  PID: " + PID + "\n" +
                "  Llegada: " + t_arrival + "\n" +
                "  Prioridad: " + priority + "\n" +
                "  Páginas: " + pages + "\n" +
                "  Ráfagas:\n";

        for (Burst b : bursts) {
            resultado += "    " + b + "\n";
        }

        resultado += "}";

        return resultado;
    }


}
