package proccess;

public class Burst {
    private String resource; //CPU O E/S
    private int time; //Tiempo de la rafaga
    public Burst (String resource, int time){
        this.resource = resource;
        this.time = time;
    }
    public void setTime(int time){
        this.time = time;
    }
    public void setResource(String resource){
        this.resource = resource;
    }
    public int getTime(){
        return this.time;
    }
    public String getResource(){
        return this.resource;
    }
}