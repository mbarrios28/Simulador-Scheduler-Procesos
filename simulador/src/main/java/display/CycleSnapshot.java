package display;

import process.BurstResource;

/**
 * Captura el estado de un proceso ANTES de que ejecute el ciclo
 * Esto permite mostrar correctamente la información en el display
 */
public class CycleSnapshot {
    private String processId;
    private String state;
    private BurstResource burstType;
    private int burstRemaining;
    private int burstTotal;
    private boolean isIdle;
    
    public CycleSnapshot(String processId, String state, BurstResource burstType, 
                        int burstRemaining, int burstTotal) {
        this.processId = processId;
        this.state = state;
        this.burstType = burstType;
        this.burstRemaining = burstRemaining;
        this.burstTotal = burstTotal;
        this.isIdle = false;
    }
    
    public CycleSnapshot() {
        this.isIdle = true;
        this.processId = "NONE";
    }
    
    public String getProcessId() {
        return processId;
    }
    
    public String getState() {
        return state;
    }
    
    public BurstResource getBurstType() {
        return burstType;
    }
    
    public int getBurstRemaining() {
        return burstRemaining;
    }
    
    public int getBurstTotal() {
        return burstTotal;
    }
    
    public boolean isIdle() {
        return isIdle;
    }
    
    @Override
    public String toString() {
        if (isIdle) {
            return "IDLE";
        }
        return processId + " - " + state + " - " + burstType + 
               "(" + burstRemaining + "/" + burstTotal + ")";
    }
}