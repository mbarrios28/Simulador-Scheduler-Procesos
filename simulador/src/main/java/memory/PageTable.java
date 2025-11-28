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
        
        // Cada entrada en la lista representa una página
        for (int i = 0; i < totalPages; i++) {
            entries.add(new PageTableEntry(i));  // El indice es el numero de página
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

    // Encontrar que pagina está en un marco específico
    public Integer findPageInFrame(int frameNumber) {
        for (int i = 0; i < totalPages; i++) {
            PageTableEntry entry = entries.get(i);
            if (entry.isPresent() && entry.getFrameNumber() == frameNumber) {
                return i;  // retorna el numero de pagina
            }
        }
        return null;
    }

}
