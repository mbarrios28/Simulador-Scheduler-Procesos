package process;

import java.util.ArrayList;

import org.junit.Test;

public class ProcessTest {
    @Test
    public void testCrearProcesos(){
        InputParser parser = new InputParser("/prueba1.txt");
        parser.obtenerProcesos();
        parser.crearProcesos();
        ArrayList <Process> procesos = parser.get_process();
        for (Process proceso: procesos){
            while (!proceso.isFinished()){
                System.out.println("Proceso: " + proceso.getPID());
                System.out.println("Burst actual: " + proceso.getBurst());
                proceso.nextBurst();
                System.out.println("Estado luego de terminar el burst: " + proceso.getState());
            }
        }
    }
}
