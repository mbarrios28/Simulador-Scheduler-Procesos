package process;

public class Burst {
    private final BurstResource resource; //CPU O E/S
    private final int time_total; //Tiempo de la rafaga
    private int time_remaining; //Tiempo que le falta para terminar la rafaga, útil en Round Robin
    
    public Burst (BurstResource resource, int time){
        this.resource = resource;
        this.time_total = time;
        this.time_remaining = time;
    }

    //Métodos útiles para poder controlar los burst
    public boolean isValid(){
        return time_total > 0;
    }

    public void consumirUnidad(){
        if (time_remaining > 0) time_remaining--;
    }

    public boolean isFinished(){
        return time_remaining == 0;
    }

    //Getters
    public BurstResource getResource() {
        return resource;
    }

    public int getTime_total() {
        return time_total;
    }

    public int getTime_remaining() {
        return time_remaining;
    }

}