package memory;

public class Frame {
    private int id;
    private boolean occupied;
    private String processId;
    
    public Frame(int id) {
        this.id = id;
        this.occupied = false;
        this.processId = "None";
    }
    
    public int getId() { return id; }
    public boolean isOccupied() { return occupied; }
    public String getProcessId() { return processId; }
    
    public void occupy(String processId) {
        this.occupied = true;
        this.processId = processId;
    }
    
    public void free() {
        this.occupied = false;
        this.processId = "None";
    }
    
    @Override
    public String toString() {
        if (occupied) {
            return String.format("Frame %d: %s", id, processId);
        } else {
            return String.format("Frame %d: FREE", id);
        }
    }
}