package memory.algoritmos;
import memory.Frame;
import memory.PageTable;
import java.util.List;
import java.util.Map;

public interface ReplacementAlgorithm {
    /**
     *  Elige el frame a reemplazar cuando la memoria está llena
     * @param phsycalMemory Lista de todos los frames en memoria física
     * @param processPageTables Mapa de tablas de páginas por proceso
     * @return ID del frame a liberar, o null so no hay victima disponible
    
    */
    Integer chooseVictimFrame(List<Frame> phsycalMemory, Map<String, PageTable> processPageTables);

    /**
     * Notifica al algoritmo que una pagina ha sido cargada en memoria
     * @param processId ID del proceso
     * @param pageNumber Número de página cargada
     * @param frameId ID del frame donde se cargó la página
     */
    void onPageLoaded(String processId, int pageNumber, int frameId);


    /**
     * Notifica al algoritmo que una pagina fue accedida (para el LRU)
     * @param processId ID del proceso
     * @param pageNumber Número de página accedida
     */
    void onPageAcces(String processId, int pageNumber);

    /**
     * Notifica al algoritmo que una pagina ha sido descargada de memoria
     * @param processId ID del proceso
     * @param pageNumber Número de página descargada
     */
    void onPageUnloaded(String processId, int pageNumber);
    
    // Nombre del algoritmo
    String getName();
}
