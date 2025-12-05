package process;
import java.util.ArrayList;
import java.util.List;

public class Process {
    private String PID; 
    private int t_arrival; 
    private ArrayList <Burst> bursts; 
    private int pages; 
    private ProcessState state; 
    private int ind_burst; 
    private int priority; 
    
    private int t_start = -1;
    private int t_finish;
    private int t_wait;
    private int cpu_usage;
    private List<Integer> futurePageSequence; // Secuencia SEQ[] para algoritmo Óptimo

    public Process(String PID, int t_arrival, ArrayList<Burst> bursts, int pages) {
        this.PID = PID;
        this.bursts = bursts;
        this.pages = pages;
        this.t_arrival = t_arrival;
        this.priority = -1; 
        this.ind_burst = 0;
        this.state = ProcessState.NEW;
        this.futurePageSequence = null;
    }

    public Process(String PID, int t_arrival, ArrayList<Burst> bursts, int priority, int pages) {
        this.PID = PID;
        this.bursts = bursts;
        this.pages = pages;
        this.priority = priority;
        this.t_arrival = t_arrival;
        this.ind_burst = 0;
        this.state = ProcessState.NEW;
        this.futurePageSequence = null;
    }
    
    public Burst getBurst(){
        return this.bursts.get(this.ind_burst);
    }

    public void nextBurst() {
        this.ind_burst++;
        
        if (isFinished()) {
            this.state = ProcessState.TERMINATED;
            System.out.println("[Process] " + PID + " - ¡PROCESO COMPLETADO!");
        } else {
            if (isCurrentBurstCPU()) {
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

    public int getT_start() {
        return t_start;
    }

    public int getT_finish() {
        return t_finish;
    }

    public int getT_wait() {
        return t_wait;
    }

    public int getCpu_usage() {
        return cpu_usage;
    }

    public void setCpu_usage(int cpu_usage) {
        this.cpu_usage = cpu_usage;
    }
    public void setT_start(int t_start) {
        this.t_start = t_start;
    }

    public void setT_finish(int t_finish) {
        this.t_finish = t_finish;
    }

    public void setState(ProcessState state) {
        this.state = state;
    }

    public void setT_wait(int t_wait) {
        this.t_wait = t_wait;
    }

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

    public int getInd_burst() {
        return ind_burst;
    }

    public List<Integer> getFuturePageSequence() {
        return futurePageSequence;
    }

    public void setFuturePageSequence(List<Integer> futurePageSequence) {
        this.futurePageSequence = futurePageSequence;
    }


}
