package process;
import java.util.ArrayList;
public class Process {
    // Atributos
    private int PID; //PID del proceso
    private int llegada; //Segundo en el que llega el proceso
    private ArrayList <Burst> rafagas; //Lista de las rafagas que tiene el proceso
    private int paginasNecesarias; //Páginas de memoria virtual que necesita el proceso
    private String estado; //Estado en el que se encuentra el proceso (Nuevo, Listo, Ejecutando, Bloqueado (por memoria o E/S), Terminado)
    private int ind_rafaga; //Indice de rafaga actual
    private int t_espera;
    private int t_retorno;
    private int t_ejecución;

    public Process(int PID, int llegada, int paginasNecesarias, ArrayList<Burst> rafagas) {
        this.PID = PID;
        this.llegada = llegada;
        this.paginasNecesarias = paginasNecesarias;
        this.rafagas = rafagas;
        this.estado = "Nuevo";
    }
}
