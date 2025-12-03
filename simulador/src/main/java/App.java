import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class App {
    public static void main(String[] args) {
        // Ejecutar en el hilo de Swing
        SwingUtilities.invokeLater(() -> {
            try {
                // Mostrar interfaz de configuración inicial
                display.ConfiguracionInicialGUI configGUI = new display.ConfiguracionInicialGUI();
                configGUI.show();
                
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null,
                    "Error al iniciar la aplicación: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        });
    }
}