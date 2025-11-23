package process;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class InputParser {

    private final String file;

    public InputParser(String file) {
        this.file = file;
    }

    public void ObtenerLineas(){
        try (BufferedReader br = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(file)))){
            String line;
            while ((line = br.readLine())!= null){
                System.out.println(line);
            }

        } catch (IOException e){
            System.out.println("Error cargando el archivo");
        }
    }

}
