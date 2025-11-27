package memory;

public class Frame {
    private int id;
    private boolean occupied;
    
    public Frame(int id) {
        this.id = id;
        this.occupied = false;
    }
    
    public int getId() { return id; }
    public boolean isOccupied() { return occupied; }
    
    public void occupy() {
        this.occupied = true;
    }
    
    public void free() {
        this.occupied = false;
    }
    
    @Override
    public String toString() {
        if (occupied) {
            return String.format("Frame %d: OCCUPIED", id);
        } else {
            return String.format("Frame %d: FREE", id);
        }
    }
}