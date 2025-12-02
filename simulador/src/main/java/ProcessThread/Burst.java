package ProcessThread;



public class Burst {
    private BurstType type;
    private int duration;
    private int remainingTime;
    
    public Burst(BurstType type, int duration) {
        this.type = type;
        this.duration = duration;
        this.remainingTime = duration;
    }
    
    public BurstType getType() { return type; }
    public int getDuration() { return duration; }
    public int getRemainingTime() { return remainingTime; }
    public void setRemainingTime(int time) { this.remainingTime = time; }
    
    @Override
    public String toString() {
        return type + "(" + duration + ")";
    }
}