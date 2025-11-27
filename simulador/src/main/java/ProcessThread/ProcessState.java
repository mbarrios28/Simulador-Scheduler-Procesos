package ProcessThread;


 //Estados posibles de un proceso

public enum ProcessState {
    NEW,           // Recién creado
    READY,         // Listo para ejecutar
    RUNNING,       // Ejecutándose en CPU
    BLOCKED_MEM,   // Bloqueado por memoria
    BLOCKED_IO,    // Bloqueado por E/S
    TERMINATED     // Finalizado
}