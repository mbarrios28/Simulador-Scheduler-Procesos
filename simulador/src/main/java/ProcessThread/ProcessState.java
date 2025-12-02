package ProcessThread;


 //Estados posibles de un proceso

public enum ProcessState {
    NEW,           
    READY,         
    RUNNING,       
    BLOCKED_MEM,   
    BLOCKED_IO,    
    TERMINATED     
}