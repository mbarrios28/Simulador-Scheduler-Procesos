package process;

import org.junit.Test;

public class InputParserTest {
    @Test
    public void testObtenerLineas(){
        InputParser parser = new InputParser("/prueba1.txt");
        parser.ObtenerLineas();
    }

}
