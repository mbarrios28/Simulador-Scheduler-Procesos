package memory;

public class Frame {
    private int id;
    private boolean occupied;
    private String processId;
    private int pageNumber;

    public Frame(int id) {
        this.id = id;
        this.occupied = false;
        this.processId = null;
        this.pageNumber = -1;
    }

    public int getId() { return id; }
    public boolean isOccupied() { return occupied; }
    public String getProcessId() { return processId; }
    public int getPageNumber() { return pageNumber; }

    public void occupy(String processId, int pageNumber) {
        this.occupied = true;
        this.processId = processId;
        this.pageNumber = pageNumber;
    }

    public void free() {
        this.occupied = false;
        this.processId = null;
        this.pageNumber = -1;
    }

    @Override
    public String toString() {
        if (occupied) {
            return String.format("Frame %d: %s-Page%d", id, processId, pageNumber);
        } else {
            return String.format("Frame %d: FREE", id);
        }
    }
}
