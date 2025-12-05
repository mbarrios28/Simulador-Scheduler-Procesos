package display;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import memory.MemoryManager;
import process.Process;
import scheduler.Scheduler;

public class SimulatorGUI {
    private JFrame frame;
    private JTextPane statusArea;
    private JTextPane metricsArea;
    private JPanel controlPanel;
    private JPanel ganttPanel;
    
    private Scheduler scheduler;
    private MemoryManager memoryManager;
    private SimulatorDisplay display;
    
    private java.util.List<Process> allProcesses;
    private boolean simulationRunning = false;
    private int currentCycle = 0;
    private int maxCycles = 100;
    
    private Map<Integer, String[]> cycleSnapshots;
    private Map<Integer, String> ganttData;
    
    public SimulatorGUI(Scheduler scheduler, MemoryManager memoryManager, java.util.List<Process> processes) {
        this.scheduler = scheduler;
        this.memoryManager = memoryManager;
        this.allProcesses = processes;
        this.display = new SimulatorDisplay(scheduler, memoryManager);
        this.cycleSnapshots = new LinkedHashMap<>();
        this.ganttData = new LinkedHashMap<>();
        
        for (Process p : processes) {
            display.addProcess(p);
        }
        
        initializeGUI();
    }
    
    private void initializeGUI() {
        frame = new JFrame("Simulador de Planificación y Memoria");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        
        // Panel superior - Título
        JPanel titlePanel = new JPanel();
        titlePanel.setBackground(new Color(41, 128, 185));
        titlePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JLabel title = new JLabel("SIMULADOR DE PLANIFICACIÓN Y GESTIÓN DE MEMORIA");
        title.setFont(new Font("Arial", Font.BOLD, 20));
        title.setForeground(Color.WHITE);
        titlePanel.add(title);
        
        frame.add(titlePanel, BorderLayout.NORTH);
        
        // Panel central - Dividido en dos partes
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setDividerLocation(400);
        
        // Panel superior - Estado del sistema
        JPanel statusPanel = createStatusPanel();
        splitPane.setTopComponent(statusPanel);
        
        // Panel inferior - Métricas y controles
        JPanel bottomPanel = new JPanel(new BorderLayout());
        
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Métricas", createMetricsPanel());
        tabbedPane.addTab("Diagrama Gantt", createGanttPanel());
        tabbedPane.addTab("Controles", createControlPanel());
        
        bottomPanel.add(tabbedPane, BorderLayout.CENTER);
        splitPane.setBottomComponent(bottomPanel);
        
        frame.add(splitPane, BorderLayout.CENTER);
        
        // Panel inferior - Información de ciclo
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        infoPanel.setBackground(new Color(52, 73, 94));
        infoPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        
        JLabel cycleLabel = new JLabel("Ciclo actual: T=0");
        cycleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        cycleLabel.setForeground(Color.WHITE);
        cycleLabel.setName("cycleLabel");
        infoPanel.add(cycleLabel);
        
        frame.add(infoPanel, BorderLayout.SOUTH);
        
        frame.setSize(1200, 800);
        frame.setLocationRelativeTo(null);
    }
    
    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(44, 62, 80), 2),
            "ESTADO DEL SISTEMA",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 14),
            new Color(44, 62, 80)
        ));
        
        statusArea = new JTextPane();
        statusArea.setContentType("text/html");
        statusArea.setEditable(false);
        statusArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        statusArea.setBackground(new Color(245, 245, 245));
        
        JScrollPane scrollPane = new JScrollPane(statusArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Mostrar estado inicial
        updateStatusDisplay(0, null);
        
        return panel;
    }
    
    private JPanel createMetricsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        metricsArea = new JTextPane();
        metricsArea.setContentType("text/html");
        metricsArea.setEditable(false);
        metricsArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        
        JScrollPane scrollPane = new JScrollPane(metricsArea);
        
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createGanttPanel() {
        ganttPanel = new JPanel();
        ganttPanel.setLayout(new BorderLayout());
        ganttPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JLabel placeholder = new JLabel("El diagrama Gantt se mostrará al finalizar la simulación");
        placeholder.setHorizontalAlignment(SwingConstants.CENTER);
        placeholder.setFont(new Font("Arial", Font.ITALIC, 14));
        placeholder.setForeground(Color.GRAY);
        
        ganttPanel.add(placeholder, BorderLayout.CENTER);
        
        return ganttPanel;
    }
    
    private JPanel createControlPanel() {
        controlPanel = new JPanel();
        controlPanel.setLayout(new GridBagLayout());
        controlPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Botón Iniciar/Reiniciar
        JButton startButton = new JButton("Iniciar Simulación");
        startButton.setFont(new Font("Arial", Font.BOLD, 14));
        startButton.setBackground(new Color(46, 204, 113));
        startButton.setForeground(Color.WHITE);
        startButton.setPreferredSize(new Dimension(200, 40));
        
        startButton.addActionListener(e -> startSimulation());
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        controlPanel.add(startButton, gbc);
        
        // Botón Siguiente Ciclo
        JButton nextButton = new JButton("Siguiente Ciclo (T=" + currentCycle + ")");
        nextButton.setName("nextButton");
        nextButton.setFont(new Font("Arial", Font.BOLD, 14));
        nextButton.setBackground(new Color(52, 152, 219));
        nextButton.setForeground(Color.WHITE);
        nextButton.setPreferredSize(new Dimension(200, 40));
        
        nextButton.addActionListener(e -> nextCycle());
        
        gbc.gridy = 1;
        controlPanel.add(nextButton, gbc);
        
        // Botón Avance Automático
        JButton autoButton = new JButton("Avance Automático");
        autoButton.setFont(new Font("Arial", Font.BOLD, 14));
        autoButton.setBackground(new Color(241, 196, 15));
        autoButton.setForeground(Color.BLACK);
        autoButton.setPreferredSize(new Dimension(200, 40));
        
        autoButton.addActionListener(e -> runAutoAdvance());
        
        gbc.gridy = 2;
        controlPanel.add(autoButton, gbc);
        
        // Panel de configuración
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        
        JPanel configPanel = new JPanel(new GridLayout(3, 2, 10, 5));
        configPanel.setBorder(BorderFactory.createTitledBorder("Configuración"));
        
        configPanel.add(new JLabel("Ciclos máximos:"));
        JTextField maxCyclesField = new JTextField(String.valueOf(maxCycles));
        maxCyclesField.addActionListener(e -> {
            try {
                maxCycles = Integer.parseInt(maxCyclesField.getText());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(frame, "Ingrese un número válido");
            }
        });
        configPanel.add(maxCyclesField);
        
        configPanel.add(new JLabel("Velocidad (ms):"));
        JSlider speedSlider = new JSlider(50, 1000, 200);
        speedSlider.setMajorTickSpacing(200);
        speedSlider.setMinorTickSpacing(50);
        speedSlider.setPaintTicks(true);
        speedSlider.setPaintLabels(true);
        speedSlider.setName("speedSlider");
        configPanel.add(speedSlider);
        
        configPanel.add(new JLabel("Delay entre ciclos:"));
        JLabel delayLabel = new JLabel(speedSlider.getValue() + " ms");
        delayLabel.setName("delayLabel");
        configPanel.add(delayLabel);
        
        // Listener para actualizar el label cuando cambia el slider
        speedSlider.addChangeListener(e -> {
            int value = ((JSlider) e.getSource()).getValue();
            delayLabel.setText(value + " ms");
        });
        
        controlPanel.add(configPanel, gbc);
        
        // Botón Finalizar
        gbc.gridy = 4;
        
        JButton finishButton = new JButton("Finalizar Simulación");
        finishButton.setFont(new Font("Arial", Font.BOLD, 14));
        finishButton.setBackground(new Color(231, 76, 60));
        finishButton.setForeground(Color.WHITE);
        
        finishButton.addActionListener(e -> finishSimulation());
        
        controlPanel.add(finishButton, gbc);
        
        return controlPanel;
    }
    
    private void startSimulation() {
        simulationRunning = true;
        currentCycle = 0;
        cycleSnapshots.clear();
        ganttData.clear();
        
        // Agregar procesos al scheduler
        for (Process p : allProcesses) {
            scheduler.addProcess(p);
        }
        
        updateStatusDisplay(0, null);
        updateCycleLabel();
        
        JOptionPane.showMessageDialog(frame, 
            "Simulación iniciada.\n" +
            "Use el botón 'Siguiente Ciclo' para avanzar paso a paso.\n" +
            "O use 'Avance Automático' para ejecutar hasta el final.\n" +
            "Ciclos máximos: " + maxCycles,
            "Simulación Iniciada",
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    private boolean allProcessesTerminated() {
        for (Process p : allProcesses) {
            if (p == null) {
                return false;
            }
            
            process.ProcessState state = p.getState();
            if (state == null || state != process.ProcessState.TERMINATED) {
                return false;
            }
        }
        return true;
    }
    
    private void nextCycle() {
        if (!simulationRunning) {
            JOptionPane.showMessageDialog(frame, 
                "Inicie primero la simulación con el botón 'Iniciar Simulación'",
                "Simulación No Iniciada",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Verificar si todos los procesos ya terminaron
        if (allProcessesTerminated()) {
            JOptionPane.showMessageDialog(frame,
                "¡Todos los procesos han terminado!\n" +
                "Simulación finalizada automáticamente.\n" +
                "Revise las métricas finales.",
                "Simulación Completada",
                JOptionPane.INFORMATION_MESSAGE);
            finishSimulation();
            return;
        }
        
        if (currentCycle >= maxCycles) {
            JOptionPane.showMessageDialog(frame, 
                "Se alcanzó el límite máximo de ciclos.\n" +
                "Presione 'Finalizar Simulación' para ver resultados.",
                "Límite Alcanzado",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        // Ejecutar un ciclo
        boolean hayTrabajo = scheduler.runOneUnit();
        
        // Obtener snapshot
        String[] stateSnapshot = scheduler.getLastExecutionSnapshot();
        
        // Guardar snapshot para historial
        cycleSnapshots.put(currentCycle, stateSnapshot);
        
        // Determinar PID para Gantt
        String executingPID = "NONE";
        if (stateSnapshot != null && stateSnapshot.length > 0) {
            String state = stateSnapshot[1];
            if (state.equals("RUNNING")) {
                executingPID = stateSnapshot[0];
            }
        }
        
        // Registrar en Gantt
        ganttData.put(currentCycle, executingPID);
        
        // Actualizar display
        updateStatusDisplay(currentCycle, stateSnapshot);
        
        // Actualizar etiqueta de ciclo
        updateCycleLabel();
        
        currentCycle++;
        
        // Verificar si la simulación debe terminar
        if (!hayTrabajo) {
            int ciclosSinTrabajo = checkIdleCycles();
            if (ciclosSinTrabajo >= 3) {
                JOptionPane.showMessageDialog(frame,
                    "3 ciclos consecutivos sin trabajo.\n" +
                    "Simulación finalizada automáticamente.",
                    "Simulación Completada",
                    JOptionPane.INFORMATION_MESSAGE);
                finishSimulation();
            }
        }
        
        // Verificar si terminaron todos después del ciclo
        if (allProcessesTerminated()) {
            updateStatusDisplay(currentCycle - 1, stateSnapshot);
        }
    }
    
    private void runAutoAdvance() {
        if (!simulationRunning) {
            JOptionPane.showMessageDialog(frame,
                "Primero inicie la simulación con el botón 'Iniciar Simulación'",
                "Simulación No Iniciada",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Obtener velocidad del slider y hacerla final
        final int finalDelay = getDelayFromSlider();
        
        // Deshabilitar botones durante el avance automático
        setButtonsEnabled(false);
        
        // Crear un hilo para el avance automático
        Thread autoThread = new Thread(() -> {
            try {
                while (simulationRunning && currentCycle < maxCycles && !allProcessesTerminated()) {
                    final int cycleToExecute = currentCycle;
                    
                    SwingUtilities.invokeAndWait(() -> {
                        executeSingleCycle(cycleToExecute);
                    });
                    
                    currentCycle++;
                    Thread.sleep(finalDelay);
                    
                    if (allProcessesTerminated()) {
                        break;
                    }
                }
                
                SwingUtilities.invokeLater(() -> {
                    completeAutoAdvance();
                });
                
            } catch (Exception e) {
                handleAutoAdvanceError(e);
            }
        });
        
        autoThread.setDaemon(true);
        autoThread.start();
    }

    private int getDelayFromSlider() {
        int delay = 200; // valor por defecto
        
        // Buscar el slider en el controlPanel
        for (Component comp : controlPanel.getComponents()) {
            if (comp instanceof JPanel) {
                JPanel panel = (JPanel) comp;
                Component[] components = panel.getComponents();
                
                for (Component subComp : components) {
                    if (subComp instanceof JSlider) {
                        JSlider slider = (JSlider) subComp;
                        if ("speedSlider".equals(slider.getName())) {
                            return slider.getValue();
                        }
                    }
                }
            }
        }
        
        return delay; // Retornar valor por defecto si no se encuentra
    }

    private void executeSingleCycle(int cycle) {
        boolean hayTrabajo = scheduler.runOneUnit();
        String[] stateSnapshot = scheduler.getLastExecutionSnapshot();
        
        cycleSnapshots.put(cycle, stateSnapshot);
        
        String executingPID = "NONE";
        if (stateSnapshot != null && stateSnapshot.length > 0) {
            String state = stateSnapshot[1];
            if (state.equals("RUNNING")) {
                executingPID = stateSnapshot[0];
            }
        }
        
        ganttData.put(cycle, executingPID);
        updateStatusDisplay(cycle, stateSnapshot);
        updateCycleLabel();
        
        if (!hayTrabajo) {
            int ciclosSinTrabajo = checkIdleCycles();
            if (ciclosSinTrabajo >= 3) {
                simulationRunning = false;
            }
        }
    }

    private void completeAutoAdvance() {
        setButtonsEnabled(true);
        
        if (allProcessesTerminated()) {
            JOptionPane.showMessageDialog(frame,
                "¡Avance automático completado!\n" +
                "Todos los procesos han terminado.\n" +
                "Mostrando métricas finales y diagrama Gantt...",
                "Simulación Completada",
                JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(frame,
                "Avance automático detenido.\n" +
                "Se alcanzó el límite de ciclos o no hay trabajo.\n" +
                "Mostrando resultados actuales...",
                "Avance Detenido",
                JOptionPane.INFORMATION_MESSAGE);
        }
        
        finishSimulation();
    }

    private void handleAutoAdvanceError(Exception e) {
        e.printStackTrace();
        SwingUtilities.invokeLater(() -> {
            setButtonsEnabled(true);
            JOptionPane.showMessageDialog(frame,
                "Error durante el avance automático: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        });
    }
    
    private void setButtonsEnabled(boolean enabled) {
        for (Component comp : controlPanel.getComponents()) {
            if (comp instanceof JButton) {
                JButton button = (JButton) comp;
                
                // No deshabilitar el botón de Finalizar
                if (!button.getText().contains("Finalizar")) {
                    button.setEnabled(enabled);
                    
                    // Cambiar apariencia cuando está deshabilitado
                    if (!enabled) {
                        button.setBackground(Color.LIGHT_GRAY);
                        button.setForeground(Color.DARK_GRAY);
                    } else {
                        // Restaurar colores originales
                        if (button.getText().contains("Iniciar")) {
                            button.setBackground(new Color(46, 204, 113));
                            button.setForeground(Color.WHITE);
                        } else if (button.getText().contains("Siguiente")) {
                            button.setBackground(new Color(52, 152, 219));
                            button.setForeground(Color.WHITE);
                        } else if (button.getText().contains("Automático")) {
                            button.setBackground(new Color(241, 196, 15));
                            button.setForeground(Color.BLACK);
                        }
                    }
                }
            }
        }
    }
    
    private int checkIdleCycles() {
        int idleCount = 0;
        for (int i = currentCycle - 1; i >= Math.max(0, currentCycle - 3); i--) {
            if (ganttData.containsKey(i) && ganttData.get(i).equals("NONE")) {
                idleCount++;
            } else {
                break;
            }
        }
        return idleCount;
    }
    
    private void updateCycleLabel() {
        Component[] components = frame.getContentPane().getComponents();
        for (Component comp : components) {
            if (comp instanceof JPanel) {
                Component[] subComps = ((JPanel) comp).getComponents();
                for (Component subComp : subComps) {
                    if (subComp instanceof JLabel && "cycleLabel".equals(subComp.getName())) {
                        ((JLabel) subComp).setText("Ciclo actual: T=" + currentCycle);
                        break;
                    }
                }
            }
        }
        
        // Actualizar texto del botón
        for (Component comp : controlPanel.getComponents()) {
            if (comp instanceof JButton && "nextButton".equals(comp.getName())) {
                ((JButton) comp).setText("Siguiente Ciclo (T=" + currentCycle + ")");
                break;
            }
        }
    }
    
    private void updateStatusDisplay(int cycle, String[] snapshot) {
        StringBuilder html = new StringBuilder();
        
        html.append("<html><head><style>")
            .append("body { font-family: 'Courier New', monospace; font-size: 12px; }")
            .append(".header { background-color: #2c3e50; color: white; padding: 5px; text-align: center; }")
            .append(".section { margin: 10px 0; border: 1px solid #bdc3c7; padding: 8px; }")
            .append(".subtitle { color: #2980b9; font-weight: bold; margin-top: 5px; }")
            .append(".running { color: #27ae60; font-weight: bold; }")
            .append(".ready { color: #3498db; }")
            .append(".blocked { color: #f39c12; }")
            .append(".terminated { color: #e74c3c; }")
            .append(".idle { color: #95a5a6; font-style: italic; }")
            .append(".free-frame { color: #27ae60; font-weight: bold; }") // NUEVO
            .append(".used-frame { color: #e74c3c; }") // NUEVO
            .append("</style></head><body>");
        
        // Encabezado
        html.append("<div class='header'>")
            .append("<h3>CICLO T=").append(cycle).append("</h3>")
            .append("</div>");
        
        // Proceso ejecutando
        html.append("<div class='section'>")
            .append("<div class='subtitle'> EJECUTANDO:</div>");
        
        if (snapshot != null && snapshot.length >= 5 && !snapshot[0].equals("NONE")) {
            html.append("<span class='running'>").append(snapshot[0]).append("</span><br>")
                .append("Estado: ").append(snapshot[1]).append("<br>")
                .append("Ráfaga: ").append(snapshot[2]).append(" (")
                .append(snapshot[3]).append("/").append(snapshot[4]).append(" ciclos)");
        } else {
            html.append("<span class='idle'>IDLE (sin procesos ejecutando)</span>");
        }
        html.append("</div>");
        
        // Procesos en cola de listos
        html.append("<div class='section'>")
            .append("<div class='subtitle'>LISTOS (Ready Queue):</div>");
        
        boolean hasReady = false;
        for (Process p : allProcesses) {
            if (p.getState() != null && p.getState().name().equals("READY")) {
                hasReady = true;
                html.append("<span class='ready'>• ").append(p.getPID()).append("</span>")
                    .append(" - Prio: ").append(p.getPriority())
                    .append(", Espera: ").append(p.getT_wait()).append(" ciclos<br>");
            }
        }
        
        if (!hasReady) {
            html.append("<span class='idle'>(Cola vacía)</span>");
        }
        html.append("</div>");
        
        // Procesos bloqueados
        html.append("<div class='section'>")
            .append("<div class='subtitle'>BLOQUEADOS:</div>");
        
        boolean hasBlocked = false;
        for (Process p : allProcesses) {
            if (p.getState() != null && p.getState().name().contains("BLOCKED")) {
                hasBlocked = true;
                html.append("<span class='blocked'>• ").append(p.getPID()).append("</span>")
                    .append(" - ").append(p.getState()).append("<br>");
            }
        }
        
        if (!hasBlocked) {
            html.append("<span class='idle'>(No hay procesos bloqueados)</span>");
        }
        html.append("</div>");
        
        // Memoria - NUEVA SECCIÓN MEJORADA
        html.append("<div class='section'>")
            .append("<div class='subtitle'>MEMORIA (Marcos de Página):</div>");
        
        int totalFrames = memoryManager.getTotalFrames();
        int freeFrames = memoryManager.getFreeFramesCount();
        int usedFrames = totalFrames - freeFrames;
        
        // Estado de cada frame
        html.append("<div style='margin-bottom: 10px;'>");
        html.append("<strong>Estado de marcos:</strong><br>");
        
        // Obtener información de cada frame
        java.util.List<String> frameContents = new ArrayList<>();
        
        for (int i = 0; i < totalFrames; i++) {
            String frameInfo = memoryManager.findPageInFrame(memoryManager.getFrame(i));
            
            if (frameInfo.equals("FREE")) {
                frameContents.add("<span class='free-frame'>[F" + i + ": FREE]</span>");
            } else {
                // Formato: "P1-P0" -> Mostrar como "P1-0"
                String formattedInfo = frameInfo.replace("-P", "-");
                frameContents.add("<span class='used-frame'>[F" + i + ": " + formattedInfo + "]</span>");
            }
        }
        
        // Mostrar frames en filas de 4
        html.append("<div style='font-family: monospace;'>");
        for (int i = 0; i < frameContents.size(); i++) {
            html.append(frameContents.get(i)).append(" ");
            if ((i + 1) % 4 == 0 && i < frameContents.size() - 1) {
                html.append("<br>");
            }
        }
        html.append("</div>");
        
        html.append("</div>");
        
        // Estadísticas de ocupación
        html.append("<div style='margin-top: 10px;'>");
        html.append("<strong>Ocupación:</strong> ").append(usedFrames).append("/").append(totalFrames)
            .append(" (").append(String.format("%.1f", (usedFrames * 100.0 / totalFrames)))
            .append("%)<br>");
        html.append("<strong>Marcos libres:</strong> ").append(freeFrames).append("<br>");
        html.append("<strong>Marcos ocupados:</strong> ").append(usedFrames);
        html.append("</div>");
        
        // Tablas de páginas por proceso
        html.append("<div style='margin-top: 15px;'>");
        html.append("<strong>TABLAS DE PÁGINAS:</strong><br>");
        
        boolean hayTablas = false;
        for (Process p : allProcesses) {
            if (p.getState() != null && !p.getState().name().equals("NEW")) {
                hayTablas = true;
                html.append("<div style='margin-left: 10px; margin-top: 5px;'>");
                html.append("<span style='font-weight: bold;'>").append(p.getPID()).append("</span>")
                    .append(" (").append(p.getPages()).append(" páginas):<br>");
                html.append("<div style='margin-left: 20px;'>");
                
                // Estado de cada página
                for (int i = 0; i < p.getPages(); i++) {
                    if (memoryManager.isPageLoaded(p.getPID(), i)) {
                        int frameNum = memoryManager.getPageTable(p.getPID()).getEntry(i).getFrameNumber();
                        html.append("<span style='color: #27ae60;'>✓ P").append(i)
                            .append(" → F").append(frameNum).append("</span> ");
                    } else {
                        html.append("<span style='color: #e74c3c;'>✗ P").append(i)
                            .append(" → Disco</span> ");
                    }
                    
                    // Salto de línea cada 4 páginas
                    if ((i + 1) % 4 == 0 && i < p.getPages() - 1) {
                        html.append("<br>");
                    }
                }
                
                // Estadísticas del proceso
                int faults = memoryManager.getPageFaults(p.getPID());
                int replacements = memoryManager.getReplacements(p.getPID());
                html.append("<br><span style='font-size: 11px; color: #7f8c8d;'>")
                    .append("Fallos: ").append(faults)
                    .append(", Reemplazos: ").append(replacements)
                    .append("</span>");
                
                html.append("</div></div>");
            }
        }
        
        if (!hayTablas) {
            html.append("<span class='idle'>(No hay procesos con memoria asignada)</span>");
        }
        html.append("</div>");
        
        html.append("</div>");
        
        html.append("</body></html>");
        
        statusArea.setText(html.toString());
        statusArea.setCaretPosition(0);
    }
    
    private int calculateEffectiveCycles() {
        int lastActiveCycle = 0;
        for (int i = currentCycle - 1; i >= 0; i--) {
            if (ganttData.containsKey(i)) {
                String pid = ganttData.get(i);
                if (!pid.equals("NONE")) {
                    if (cycleSnapshots.containsKey(i)) {
                        String[] snapshot = cycleSnapshots.get(i);
                        if (snapshot != null && snapshot.length > 1) {
                            if (snapshot[1].equals("RUNNING")) {
                                lastActiveCycle = i + 1;
                                break;
                            }
                        }
                    }
                }
            }
        }
        
        return Math.max(1, lastActiveCycle);
    }
    
    private void finishSimulation() {
        simulationRunning = false;
        
        setButtonsEnabled(true);
        
        int finalCycle = calculateEffectiveCycles();
        
        for (Process p : allProcesses) {
            if (p.getState() != null && p.getState().name().equals("TERMINATED") && p.getT_finish() <= 0) {
                for (int i = 0; i < finalCycle; i++) {
                    if (cycleSnapshots.containsKey(i)) {
                        String[] snapshot = cycleSnapshots.get(i);
                        if (snapshot != null && snapshot.length > 1) {
                            if (snapshot[0].equals(p.getPID()) && snapshot[1].equals("TERMINATED")) {
                                p.setT_finish(i);
                                break;
                            }
                        }
                    }
                }
                if (p.getT_finish() <= 0) {
                    p.setT_finish(finalCycle);
                }
            }
        }
        
        currentCycle = finalCycle;
        updateCycleLabel();
        String[] lastSnapshot = cycleSnapshots.get(finalCycle - 1);
        updateStatusDisplay(finalCycle - 1, lastSnapshot);
        showMetrics();
        javax.swing.Timer ganttTimer = new javax.swing.Timer(300, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showGanttChart();
            }
        });
        ganttTimer.setRepeats(false);
        ganttTimer.start();
    }
    
    private void showMetrics() {
        StringBuilder html = new StringBuilder();
        
        html.append("<html><head><style>")
            .append("body { font-family: Arial, sans-serif; font-size: 13px; }")
            .append("h2 { color: #2c3e50; border-bottom: 2px solid #3498db; padding-bottom: 5px; }")
            .append("h3 { color: #2980b9; }")
            .append("table { border-collapse: collapse; width: 100%; margin: 10px 0; }")
            .append("th { background-color: #3498db; color: white; padding: 8px; text-align: left; }")
            .append("td { padding: 8px; border-bottom: 1px solid #ddd; }")
            .append("tr:hover { background-color: #f5f5f5; }")
            .append(".metric { font-weight: bold; color: #e74c3c; }")
            .append("</style></head><body>");
        
        html.append("<h2>MÉTRICAS DE SIMULACIÓN</h2>");
        html.append("<p><strong>Ciclo actual:</strong> T=").append(currentCycle).append("</p>");
        
        // Tabla de procesos
        html.append("<h3>Procesos</h3>")
            .append("<table>")
            .append("<tr><th>PID</th><th>Estado</th><th>Llegada</th><th>Inicio</th>")
            .append("<th>Fin</th><th>Espera</th><th>CPU</th><th>Fallos</th></tr>");
        
        int totalWait = 0;
        int totalCPU = 0;
        int completed = 0;
        int totalTurnaround = 0;
        
        for (Process p : allProcesses) {
            int turnaround = 0;
            if (p.getT_finish() > 0 && p.getT_start() != -1) {
                turnaround = p.getT_finish() - p.getT_arrival();
                totalTurnaround += turnaround;
            }
            
            html.append("<tr>")
                .append("<td>").append(p.getPID()).append("</td>")
                .append("<td>").append(p.getState()).append("</td>")
                .append("<td>").append(p.getT_arrival()).append("</td>")
                .append("<td>").append(p.getT_start() != -1 ? p.getT_start() : "-").append("</td>")
                .append("<td>").append(p.getT_finish() > 0 ? p.getT_finish() : "-").append("</td>")
                .append("<td>").append(p.getT_wait()).append("</td>")
                .append("<td>").append(p.getCpu_usage()).append("</td>")
                .append("<td>").append(memoryManager.getPageFaults(p.getPID())).append("</td>")
                .append("</tr>");
            
            if (p.getState() != null && !p.getState().name().equals("NEW")) {
                totalWait += p.getT_wait();
                totalCPU += p.getCpu_usage();
                if (p.getState().name().equals("TERMINATED")) {
                    completed++;
                }
            }
        }
        html.append("</table>");
        
        // Métricas globales
        html.append("<h3>Métricas Globales</h3>")
            .append("<table>")
            .append("<tr><td>Procesos completados:</td><td class='metric'>")
            .append(completed).append("/").append(allProcesses.size()).append("</td></tr>");
        
        if (allProcesses.size() > 0) {
            html.append("<tr><td>Tiempo promedio de espera:</td><td class='metric'>")
                .append(String.format("%.2f", totalWait / (double) allProcesses.size()))
                .append(" ciclos</td></tr>");
        }
        
        if (completed > 0) {
            html.append("<tr><td>Tiempo promedio de retorno:</td><td class='metric'>")
                .append(String.format("%.2f", totalTurnaround / (double) completed))
                .append(" ciclos</td></tr>");
        }
        
        // Utilización de CPU
        int effectiveCycles = calculateEffectiveCycles();
        if (effectiveCycles > 0) {
            double cpuUtilization = (totalCPU * 100.0) / effectiveCycles;
            html.append("<tr><td>Utilización de CPU:</td><td class='metric'>")
                .append(String.format("%.2f", cpuUtilization))
                .append("% (sobre ").append(effectiveCycles).append(" ciclos efectivos)</td></tr>");
        }
        
        html.append("</table>");
        
        // Estadísticas de memoria
        html.append("<h3>Estadísticas de Memoria</h3>")
            .append("<p><strong>Algoritmo:</strong> ")
            .append(memoryManager.getReplacementAlgorithm().getName()).append("</p>");
        
        int totalFaults = 0;
        int totalReplacements = 0;
        for (Process p : allProcesses) {
            totalFaults += memoryManager.getPageFaults(p.getPID());
            totalReplacements += memoryManager.getReplacements(p.getPID());
        }
        
        html.append("<table>")
            .append("<tr><td>Total fallos de página:</td><td class='metric'>")
            .append(totalFaults).append("</td></tr>")
            .append("<tr><td>Total reemplazos:</td><td class='metric'>")
            .append(totalReplacements).append("</td></tr>")
            .append("</table>");
        
        html.append("</body></html>");
        
        metricsArea.setText(html.toString());
        metricsArea.setCaretPosition(0);
    }
    
    private void showGanttChart() {
        ganttPanel.removeAll();
        ganttPanel.setLayout(new BorderLayout());
        
        if (ganttData.isEmpty()) {
            JLabel label = new JLabel("No hay datos para mostrar el diagrama Gantt");
            label.setHorizontalAlignment(SwingConstants.CENTER);
            ganttPanel.add(label, BorderLayout.CENTER);
        } else {
            // Crear panel con scroll para el Gantt
            JPanel chartPanel = new JPanel();
            chartPanel.setLayout(new GridBagLayout());
            
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(2, 5, 2, 5);
            
            // Encabezado con tiempos
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.gridwidth = 1;
            chartPanel.add(new JLabel("T:"), gbc);
            
            // CAMBIO PRINCIPAL: Mostrar TODOS los ciclos, no solo hasta 30
            int start = 0;
            int end = currentCycle - 1; // Eliminar el límite de 30
            
            for (int i = start; i <= end; i++) {
                gbc.gridx = i + 1;
                JLabel timeLabel = new JLabel(String.valueOf(i));
                timeLabel.setHorizontalAlignment(SwingConstants.CENTER);
                timeLabel.setFont(new Font("Arial", Font.BOLD, 10));
                chartPanel.add(timeLabel, gbc);
            }
            
            // Filas de procesos
            Map<String, Color> processColors = new HashMap<>();
            Color[] colors = {
                new Color(46, 204, 113),   // Verde
                new Color(52, 152, 219),   // Azul
                new Color(155, 89, 182),   // Púrpura
                new Color(241, 196, 15),   // Amarillo
                new Color(230, 126, 34),   // Naranja
                new Color(231, 76, 60)     // Rojo
            };
            
            int colorIndex = 0;
            for (Process p : allProcesses) {
                processColors.put(p.getPID(), colors[colorIndex % colors.length]);
                colorIndex++;
            }
            
            // Barra del Gantt
            gbc.gridy = 1;
            gbc.gridx = 0;
            chartPanel.add(new JLabel(" "), gbc);
            
            for (int i = start; i <= end; i++) {
                gbc.gridx = i + 1;
                
                if (ganttData.containsKey(i)) {
                    String pid = ganttData.get(i);
                    
                    if (pid.equals("NONE")) {
                        // Celda vacía para IDLE
                        JLabel cell = new JLabel("─");
                        cell.setOpaque(true);
                        cell.setBackground(Color.LIGHT_GRAY);
                        cell.setHorizontalAlignment(SwingConstants.CENTER);
                        cell.setPreferredSize(new Dimension(30, 25));
                        chartPanel.add(cell, gbc);
                    } else {
                        // Celda con proceso
                        JLabel cell = new JLabel(pid.replace("P", ""));
                        cell.setOpaque(true);
                        cell.setBackground(processColors.get(pid));
                        cell.setForeground(Color.WHITE);
                        cell.setHorizontalAlignment(SwingConstants.CENTER);
                        cell.setFont(new Font("Arial", Font.BOLD, 11));
                        cell.setPreferredSize(new Dimension(30, 25));
                        cell.setBorder(BorderFactory.createLineBorder(Color.WHITE));
                        chartPanel.add(cell, gbc);
                    }
                } else {
                    // Celda sin datos
                    JLabel cell = new JLabel("");
                    cell.setBackground(Color.WHITE);
                    cell.setOpaque(true);
                    cell.setBorder(BorderFactory.createLineBorder(Color.GRAY));
                    cell.setPreferredSize(new Dimension(30, 25));
                    chartPanel.add(cell, gbc);
                }
            }
            
            // Leyenda
            JPanel legendPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            legendPanel.setBorder(BorderFactory.createTitledBorder("Leyenda"));
            
            for (Process p : allProcesses) {
                JPanel legendItem = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
                
                JLabel colorLabel = new JLabel("   ");
                colorLabel.setOpaque(true);
                colorLabel.setBackground(processColors.get(p.getPID()));
                colorLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
                colorLabel.setPreferredSize(new Dimension(20, 15));
                
                JLabel textLabel = new JLabel(p.getPID());
                
                legendItem.add(colorLabel);
                legendItem.add(textLabel);
                legendPanel.add(legendItem);
            }
            
            // IDLE
            JPanel idleLegend = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
            JLabel idleColor = new JLabel("─");
            idleColor.setOpaque(true);
            idleColor.setBackground(Color.LIGHT_GRAY);
            idleColor.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            idleColor.setPreferredSize(new Dimension(20, 15));
            idleLegend.add(idleColor);
            idleLegend.add(new JLabel("IDLE (sin ejecución)"));
            legendPanel.add(idleLegend);
            
            // MEJORA: Configurar el JScrollPane con scroll horizontal
            JScrollPane scrollPane = new JScrollPane(chartPanel);
            
            // Habilitar scroll horizontal siempre
            scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            
            // Ajustar tamaño preferido para permitir scroll
            scrollPane.setPreferredSize(new Dimension(800, 150));
            
            // OPCIONAL: Posicionar el scroll al final (último ciclo) automáticamente
            SwingUtilities.invokeLater(() -> {
                JScrollBar horizontalBar = scrollPane.getHorizontalScrollBar();
                horizontalBar.setValue(horizontalBar.getMaximum());
            });
            
            ganttPanel.add(scrollPane, BorderLayout.CENTER);
            ganttPanel.add(legendPanel, BorderLayout.SOUTH);
        }
        
        ganttPanel.revalidate();
        ganttPanel.repaint();
    }
    
    public void show() {
        frame.setVisible(true);
    }
}