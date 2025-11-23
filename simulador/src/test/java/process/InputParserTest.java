package process;

import java.util.ArrayList;

import org.junit.Test;

public class InputParserTest {
    @Test
    public void testObtenerProcesos(){
        InputParser parser = new InputParser("/prueba1.txt");
        parser.obtenerProcesos();
    }

    @Test
    public void testCrearProcesos(){
        InputParser parser = new InputParser("/prueba1.txt");
        parser.obtenerProcesos();
        parser.crearProcesos();
        ArrayList <Process> procesos = parser.get_process();
        for (Process temp : procesos){
            System.out.println(temp);
        }
        
    }

}
