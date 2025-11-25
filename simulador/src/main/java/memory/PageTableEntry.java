package memory;

public class PageTableEntry {
    private int pageNumber; // numero de pagina virtual
    private int frameNumber; // numero de frame (-1 si está en disco y otro valor si está en memoria)
    private boolean present; // bit de presencia 

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
        this.present = ( frameNumber != -1); 
    }

    @Override
    public String toString() {
        if (present) {
            return String.format("Pagina %d: en memoria (Frame %d)", pageNumber, frameNumber);
        } else {
            return String.format("Pagina %d: no está en memoria", pageNumber);
        }
    }

}
