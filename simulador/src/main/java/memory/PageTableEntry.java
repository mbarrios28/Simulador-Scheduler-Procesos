package memory;

public class PageTableEntry {
    private int pageNumber;     
    private int frameNumber;   // aqui es-1 si no en RAM
    private boolean present;    
    
    public PageTableEntry(int pageNumber) {
        this.pageNumber = pageNumber;
        this.frameNumber = -1;
        this.present = false;
    }
    
    public int getPageNumber() { return pageNumber; }
    public int getFrameNumber() { return frameNumber; }
    public boolean isPresent() { return present; }
    
    public void setFrameNumber(int frameNumber) {
        this.frameNumber = frameNumber;
        this.present = (frameNumber != -1);
    }
    
    @Override
    public String toString() {
        if (present) {
            return String.format("Page %d: IN MEMORY (Frame %d)", pageNumber, frameNumber);
        } else {
            return String.format("Page %d: NOT IN MEMORY", pageNumber);
        }
    }
}