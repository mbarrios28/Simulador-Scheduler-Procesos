package memory.algoritmos;

import memory.Frame;
import memory.PageTable;
import java.util.List;
import java.util.Map;

public interface ReplacementAlgorithm {
    /**
     * Elige el frame víctima a reemplazar cuando la memoria está llena.
     * @param physicalMemory Lista de todos los frames físicos
     * @param processPageTables Mapa de tablas de páginas por proceso
     * @return ID del frame a liberar, o null si no hay víctima disponible
     */
    Integer chooseVictimFrame(List<Frame> physicalMemory, 
                              Map<String, PageTable> processPageTables);
    
    /**
     * Notifica al algoritmo que una página fue cargada en memoria.
     * @param processId ID del proceso
     * @param pageNumber Número de página cargada
     * @param frameId ID del frame donde se cargó
     */
    void onPageLoaded(String processId, int pageNumber, int frameId);
    
    /**
     * Notifica al algoritmo que una página fue accedida (para LRU).
     * @param processId ID del proceso
     * @param pageNumber Número de página accedida
     */
    void onPageAccess(String processId, int pageNumber);
    
    /**
     * Notifica al algoritmo que una pagina fue descargada de memoria.
     * @param processId ID del proceso
     * @param pageNumber Número de página descargada
     * @param frameId ID del frame liberado
     */
    void onPageUnloaded(String processId, int pageNumber, int frameId);
    
    /**
     * Obtiene el nombre del algoritmo para logging.
     */
    String getName();
}