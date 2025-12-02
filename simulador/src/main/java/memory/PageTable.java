package memory;
import java.util.ArrayList;
import java.util.List;

public class PageTable {
    private String processId;
    private List<PageTableEntry> entries;
    private int totalPages;
    
    public PageTable(String processId, int totalPages) {
        this.processId = processId;
        this.totalPages = totalPages;
        this.entries = new ArrayList<>();
        
        for (int i = 0; i < totalPages; i++) {
            entries.add(new PageTableEntry(i));  
        }
    }

    public String getProcessId() { return processId; }
    public int getTotalPages() { return totalPages; }

    public PageTableEntry getEntry(int pageNumber) {
        if (pageNumber < 0 || pageNumber >= totalPages) {
            throw new IndexOutOfBoundsException("Número de página fuera de rango");
        }
        return entries.get(pageNumber);
    }

    public boolean isPageLoaded(int pageNumber) {
        PageTableEntry entry = getEntry(pageNumber);
        return entry != null && entry.isPresent();
    }

    public void pageLoaded(int pageNumber, int frameNumber) {
        PageTableEntry entry = getEntry(pageNumber);
        if (entry != null) {
            entry.setFrameNumber(frameNumber);
        }
    }

    public void pageUnloaded(int pageNumber) {
        PageTableEntry entry = getEntry(pageNumber);
        if (entry != null) {
            entry.setFrameNumber(-1);
        }
    }

    public void printPageTable() {
        System.out.println("\n=== PAGE TABLE for " + processId + " ===");
        for (int i = 0; i < totalPages; i++) {
            PageTableEntry entry = entries.get(i);
            System.out.println(entry.toString());
        }
    }

    public Integer findPageInFrame(int frameNumber) {
        for (int i = 0; i < totalPages; i++) {
            PageTableEntry entry = entries.get(i);
            if (entry.isPresent() && entry.getFrameNumber() == frameNumber) {
                return i; 
            }
        }
        return null;
    }

}
