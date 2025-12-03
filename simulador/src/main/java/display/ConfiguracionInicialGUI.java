package display;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

import memory.MemoryManager;
import memory.algoritmos.FIFO;
import memory.algoritmos.LRU;
import memory.algoritmos.Optimo;
import process.InputParser;
import process.Process;
import scheduler.Scheduler;

public class ConfiguracionInicialGUI {
    private JFrame frame;
    private JComboBox<String> schedulerCombo;
    private JComboBox<String> memoryCombo;
    private JTextField quantumField;
    private JTextField framesField;
    private JList<String> fileList;
    private DefaultListModel<String> fileListModel;
    
    private ArrayList<Process> selectedProcesses;
    private String selectedScheduler;
    private String selectedMemory;
    private int quantum;
    private int frames;
    
    public ConfiguracionInicialGUI() {
        selectedProcesses = new ArrayList<>();
        initializeGUI();
    }
    
    private void initializeGUI() {
        frame = new JFrame("Configuración Inicial - Simulador");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        
        // Panel de título
        JPanel titlePanel = new JPanel();
        titlePanel.setBackground(new Color(41, 128, 185));
        titlePanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        JLabel title = new JLabel("CONFIGURACIÓN DE SIMULACIÓN");
        title.setFont(new Font("Arial", Font.BOLD, 20));
        title.setForeground(Color.WHITE);
        titlePanel.add(title);
        
        frame.add(titlePanel, BorderLayout.NORTH);
        
        // Panel central con pestañas
        JTabbedPane tabbedPane = new JTabbedPane();
        
        // Pestaña 1: Configuración básica
        tabbedPane.addTab("Algoritmos", createAlgorithmPanel());
        
        // Pestaña 2: Archivos de procesos
        tabbedPane.addTab("Procesos", createFilesPanel());
        
        // Pestaña 3: Resumen
        tabbedPane.addTab("Resumen", createSummaryPanel());
        
        frame.add(tabbedPane, BorderLayout.CENTER);
        
        // Panel inferior con botones
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JButton cancelButton = new JButton("Cancelar");
        cancelButton.addActionListener(e -> System.exit(0));
        
        JButton startButton = new JButton("Iniciar Simulación");
        startButton.setBackground(new Color(46, 204, 113));
        startButton.setForeground(Color.WHITE);
        startButton.setFont(new Font("Arial", Font.BOLD, 14));
        startButton.addActionListener(e -> startSimulation());
        
        buttonPanel.add(cancelButton);
        buttonPanel.add(startButton);
        
        frame.add(buttonPanel, BorderLayout.SOUTH);
        
        frame.setSize(700, 500);
        frame.setLocationRelativeTo(null);
    }
    
    private JPanel createAlgorithmPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        
        // Algoritmo de Scheduler
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Algoritmo de Planificación:"), gbc);
        
        String[] schedulers = {"Round Robin (RR)", "First Come First Served (FCFS)", 
                              "Shortest Job First (SJF)", "Priority"};
        schedulerCombo = new JComboBox<>(schedulers);
        schedulerCombo.setSelectedIndex(0);
        gbc.gridx = 1;
        panel.add(schedulerCombo, gbc);
        
        // Quantum (solo para RR)
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Quantum (para RR):"), gbc);
        
        quantumField = new JTextField("2", 10);
        gbc.gridx = 1;
        panel.add(quantumField, gbc);
        
        // Algoritmo de Memoria
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("Algoritmo de Memoria:"), gbc);
        
        String[] memoryAlgos = {"FIFO (First In First Out)", "LRU (Least Recently Used)", 
                               "Optimal"};
        memoryCombo = new JComboBox<>(memoryAlgos);
        memoryCombo.setSelectedIndex(0);
        gbc.gridx = 1;
        panel.add(memoryCombo, gbc);
        
        // Frames de memoria
        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(new JLabel("Frames de memoria:"), gbc);
        
        framesField = new JTextField("7", 10);
        gbc.gridx = 1;
        panel.add(framesField, gbc);
        
        // Descripción de algoritmos
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        
        JTextArea descriptionArea = new JTextArea(5, 30);
        descriptionArea.setEditable(false);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setBackground(new Color(240, 240, 240));
        descriptionArea.setText("Round Robin (RR): Cada proceso ejecuta por un quantum fijo.\n" +
                              "FCFS: Primer llegado, primer servido.\n" +
                              "SJF: El trabajo más corto primero.\n" +
                              "Priority: Procesos con mayor prioridad primero.\n" +
                              "FIFO: Reemplaza la página más antigua.\n" +
                              "LRU: Reemplaza la página menos usada recientemente.\n" +
                              "Optimal: Reemplaza la página que no se usará por más tiempo.");
        
        JScrollPane scrollPane = new JScrollPane(descriptionArea);
        panel.add(scrollPane, gbc);
        
        return panel;
    }
    
    private JPanel createFilesPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Panel superior: Explorador de archivos
        JPanel topPanel = new JPanel(new BorderLayout());
        
        JLabel title = new JLabel("Seleccione archivos de procesos (.txt):");
        title.setFont(new Font("Arial", Font.BOLD, 14));
        topPanel.add(title, BorderLayout.NORTH);
        
        // Lista de archivos
        fileListModel = new DefaultListModel<>();
        fileList = new JList<>(fileListModel);
        fileList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        
        JScrollPane listScroll = new JScrollPane(fileList);
        topPanel.add(listScroll, BorderLayout.CENTER);
        
        // Panel de botones para archivos
        JPanel fileButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        JButton refreshButton = new JButton("Actualizar lista");
        refreshButton.addActionListener(e -> loadFilesFromResources());
        
        JButton selectAllButton = new JButton("Seleccionar todos");
        selectAllButton.addActionListener(e -> fileList.setSelectionInterval(0, fileListModel.getSize() - 1));
        
        JButton clearButton = new JButton("Limpiar selección");
        clearButton.addActionListener(e -> fileList.clearSelection());
        
        fileButtonPanel.add(refreshButton);
        fileButtonPanel.add(selectAllButton);
        fileButtonPanel.add(clearButton);
        
        topPanel.add(fileButtonPanel, BorderLayout.SOUTH);
        
        panel.add(topPanel, BorderLayout.CENTER);
        
        // Panel inferior: Información
        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.setBorder(BorderFactory.createTitledBorder("Información"));
        
        JTextArea infoArea = new JTextArea(3, 30);
        infoArea.setEditable(false);
        infoArea.setLineWrap(true);
        infoArea.setWrapStyleWord(true);
        infoArea.setText("Los archivos .txt deben estar en la carpeta 'resources'.\n" +
                        "Formato esperado: Cada línea = 'PID tiempo_llegada CPU1 IO CPU2 prioridad paginas'\n" +
                        "Ejemplo: 'P1 0 5 2 3 1 4' = P1 llega en 0, CPU(5), IO(2), CPU(3), prioridad 1, 4 páginas");
        
        infoPanel.add(new JScrollPane(infoArea), BorderLayout.CENTER);
        
        panel.add(infoPanel, BorderLayout.SOUTH);
        
        // Cargar archivos al iniciar
        loadFilesFromResources();
        
        return panel;
    }
    
    private JPanel createSummaryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JTextArea summaryArea = new JTextArea();
        summaryArea.setEditable(false);
        summaryArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        
        // Actualizar resumen cuando cambian las selecciones
        ActionListener updateSummary = e -> updateSummaryText(summaryArea);
        
        schedulerCombo.addActionListener(updateSummary);
        memoryCombo.addActionListener(updateSummary);
        quantumField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { updateSummaryText(summaryArea); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { updateSummaryText(summaryArea); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { updateSummaryText(summaryArea); }
        });
        framesField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { updateSummaryText(summaryArea); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { updateSummaryText(summaryArea); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { updateSummaryText(summaryArea); }
        });
        
        fileList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateSummaryText(summaryArea);
            }
        });
        
        panel.add(new JScrollPane(summaryArea), BorderLayout.CENTER);
        
        // Actualizar resumen inicial
        updateSummaryText(summaryArea);
        
        return panel;
    }
    private File getResourcesDirectory() {
        File[] posiblesRutas = {
            new File("../resources"),
            new File("src/main/resources"),
            new File("simulador/src/main/resources"),
            new File("resources")
        };

        for (File ruta : posiblesRutas) {
            if (ruta.exists() && ruta.isDirectory()) {
                return ruta;
            }
        }
        return null;
    }

    private void loadFilesFromResources() {
        fileListModel.clear();
        fileList.setEnabled(true);

        File resourcesDir = getResourcesDirectory(); 

        try {
            if (resourcesDir != null) {
                File[] files = resourcesDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".txt"));

                if (files != null && files.length > 0) {
                    Arrays.sort(files);
                    for (File file : files) {
                        fileListModel.addElement(file.getName());
                    }
                } else {
                    fileListModel.addElement("Carpeta vacía");
                    fileList.setEnabled(false);
                }
            } else {
                fileListModel.addElement("No se halló la carpeta resources.");
                fileList.setEnabled(false);
            }
        } catch (Exception e) {
            fileListModel.addElement("Error: " + e.getMessage());
        }
    }
    
    private void updateSummaryText(JTextArea summaryArea) {
        StringBuilder summary = new StringBuilder();
        
        summary.append("═".repeat(50)).append("\n");
        summary.append("           RESUMEN DE CONFIGURACIÓN\n");
        summary.append("═".repeat(50)).append("\n\n");
        
        // Algoritmos seleccionados
        summary.append("ALGORITMOS:\n");
        summary.append("  Planificación: ").append(schedulerCombo.getSelectedItem()).append("\n");
        
        try {
            int q = Integer.parseInt(quantumField.getText());
            summary.append("  Quantum: ").append(q).append("\n");
        } catch (NumberFormatException e) {
            summary.append("  Quantum: Inválido\n");
        }
        
        summary.append("  Memoria: ").append(memoryCombo.getSelectedItem()).append("\n");
        
        try {
            int f = Integer.parseInt(framesField.getText());
            summary.append("  Frames: ").append(f).append("\n");
        } catch (NumberFormatException e) {
            summary.append("  Frames: Inválido\n");
        }
        
        summary.append("\n").append("─".repeat(50)).append("\n\n");
        
        // Archivos seleccionados
        summary.append("ARCHIVOS DE PROCESOS:\n");
        int[] selectedIndices = fileList.getSelectedIndices();
        
        if (selectedIndices.length == 0) {
            summary.append("  Ningún archivo seleccionado\n");
        } else {
            for (int i = 0; i < selectedIndices.length; i++) {
                summary.append("  ").append(i + 1).append(". ")
                      .append(fileListModel.get(selectedIndices[i])).append("\n");
            }
            summary.append("  Total: ").append(selectedIndices.length).append(" archivo(s)\n");
        }
        
        summary.append("\n").append("─".repeat(50)).append("\n\n");
        
        // Validaciones
        summary.append("VALIDACIONES:\n");
        
        boolean valid = true;
        
        // Validar quantum
        try {
            int q = Integer.parseInt(quantumField.getText());
            if (q <= 0) {
                summary.append("  Quantum debe ser mayor a 0\n");
                valid = false;
            } else {
                summary.append("  Quantum válido\n");
            }
        } catch (NumberFormatException e) {
            summary.append("  Quantum inválido\n");
            valid = false;
        }
        
        // Validar frames
        try {
            int f = Integer.parseInt(framesField.getText());
            if (f <= 0) {
                summary.append("  Frames debe ser mayor a 0\n");
                valid = false;
            } else if (f > 100) {
                summary.append("  Muchos frames (").append(f).append("), puede afectar rendimiento\n");
            } else {
                summary.append("  Frames válido\n");
            }
        } catch (NumberFormatException e) {
            summary.append("  Frames inválido\n");
            valid = false;
        }
        
        // Validar archivos
        if (selectedIndices.length == 0) {
            summary.append("  No hay archivos seleccionados\n");
        } else {
            summary.append("   ").append(selectedIndices.length).append(" archivo(s) seleccionado(s)\n");
        }
        
        summary.append("\n");
        summary.append("═".repeat(50)).append("\n");
        summary.append(valid ? "CONFIGURACIÓN VÁLIDA" : "REVISE LOS ERRORES");
        summary.append("\n").append("═".repeat(50));
        
        summaryArea.setText(summary.toString());
    }
    
    private void startSimulation() {
        // Validar entradas
        try {
            quantum = Integer.parseInt(quantumField.getText());
            frames = Integer.parseInt(framesField.getText());
            
            if (quantum <= 0 || frames <= 0) {
                JOptionPane.showMessageDialog(frame,
                    "Quantum y frames deben ser números positivos.",
                    "Error de Validación",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(frame,
                "Por favor ingrese valores numéricos válidos para quantum y frames.",
                "Error de Entrada",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Obtener selecciones
        selectedScheduler = (String) schedulerCombo.getSelectedItem();
        selectedMemory = (String) memoryCombo.getSelectedItem();
        
        // Cargar procesos desde archivos seleccionados
        int[] selectedIndices = fileList.getSelectedIndices();
        if (selectedIndices.length == 0) {
            int response = JOptionPane.showConfirmDialog(frame,
                "No se seleccionaron archivos de procesos.\n" +
                "¿Desea usar procesos de ejemplo?",
                "Sin Archivos",
                JOptionPane.YES_NO_OPTION);
            
            if (response == JOptionPane.YES_OPTION) {
                createExampleProcesses();
            } else {
                return;
            }
        } else {
            loadProcessesFromFiles(selectedIndices);
        }
        
        if (selectedProcesses.isEmpty()) {
            JOptionPane.showMessageDialog(frame,
                "No se pudieron cargar procesos.\n" +
                "Verifique el formato de los archivos.",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Cerrar ventana de configuración
        frame.dispose();
        
        // Iniciar simulación principal
        iniciarSimulacionPrincipal();
    }
    
    private void loadProcessesFromFiles(int[] selectedIndices) {
        selectedProcesses.clear();
        
        // 1. Obtenemos la carpeta correcta
        File resourcesDir = getResourcesDirectory();
        
        if (resourcesDir == null) {
            JOptionPane.showMessageDialog(frame, "Error crítico: No se encuentra la carpeta de recursos.");
            return;
        }

        for (int index : selectedIndices) {
            String fileName = fileListModel.get(index);
            
            try {
                // 2. Construimos la referencia al archivo específico
                File targetFile = new File(resourcesDir, fileName);
                
                // Verificamos que exista antes de intentar leerlo
                if (!targetFile.exists()) {
                    throw new Exception("El archivo físico no existe en: " + targetFile.getAbsolutePath());
                }

                // 3. Pasamos la RUTA ABSOLUTA al parser
                // NOTA: Asumo que tu InputParser acepta un String con la ruta completa.
                InputParser parser = new InputParser(targetFile.getAbsolutePath());
                
                parser.obtenerProcesos();
                parser.crearProcesos();
                ArrayList<Process> procesos = parser.get_process();
                
                if (procesos != null) {
                    selectedProcesses.addAll(procesos);
                    System.out.println("Cargados " + procesos.size() + " procesos desde " + fileName);
                } else {
                    throw new Exception("El parser devolvió una lista nula de procesos.");
                }
                
            } catch (Exception e) {
                e.printStackTrace(); // Esto imprimirá el error real en la consola de tu IDE
                JOptionPane.showMessageDialog(frame,
                    "Error cargando " + fileName + ":\n" + e.getMessage(),
                    "Error de Archivo",
                    JOptionPane.WARNING_MESSAGE);
            }
        }
    }
    
    private void createExampleProcesses() {
        selectedProcesses.clear();
        
        // Procesos de ejemplo
        selectedProcesses.add(createProcess("P1", 0, new int[]{6, 2, 2}, 1, 2));
        selectedProcesses.add(createProcess("P2", 0, new int[]{5, 1, 3}, 2, 2));
        selectedProcesses.add(createProcess("P3", 0, new int[]{4, 2, 1}, 3, 2));
        
        JOptionPane.showMessageDialog(frame,
            "Se crearon 3 procesos de ejemplo.",
            "Procesos de Ejemplo",
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    private Process createProcess(String pid, int arrival, int[] bursts, int priority, int pages) {
        ArrayList<process.Burst> burstList = new ArrayList<>();
        
        if (bursts.length >= 1 && bursts[0] > 0) {
            burstList.add(new process.Burst(process.BurstResource.CPU, bursts[0]));
        }
        if (bursts.length >= 2 && bursts[1] > 0) {
            burstList.add(new process.Burst(process.BurstResource.IO, bursts[1]));
        }
        if (bursts.length >= 3 && bursts[2] > 0) {
            burstList.add(new process.Burst(process.BurstResource.CPU, bursts[2]));
        }
        
        return new Process(pid, arrival, burstList, priority, pages);
    }
    
    private void iniciarSimulacionPrincipal() {
        // Crear scheduler
        Scheduler scheduler = new Scheduler();
        
        // Configurar algoritmo de scheduler
        if (selectedScheduler.contains("Round Robin")) {
            scheduler.setAlgorithm(Scheduler.Algorithm.RR);
            scheduler.setQuantum(quantum);
        } else if (selectedScheduler.contains("FCFS")) {
            scheduler.setAlgorithm(Scheduler.Algorithm.FCFS);
        } else if (selectedScheduler.contains("SJF")) {
            scheduler.setAlgorithm(Scheduler.Algorithm.SJF);
        } else if (selectedScheduler.contains("Priority")) {
            scheduler.setAlgorithm(Scheduler.Algorithm.PRIORITY);
        }
        
        // Crear memory manager según algoritmo seleccionado
        MemoryManager memory;
        if (selectedMemory.contains("FIFO")) {
            memory = new MemoryManager(frames, new FIFO());
        } else if (selectedMemory.contains("LRU")) {
            memory = new MemoryManager(frames, new LRU());
        } else if (selectedMemory.contains("Óptimo")) {
            memory = new MemoryManager(frames, new Optimo());
        } else {
            memory = new MemoryManager(frames, new FIFO()); // Por defecto
        }
        
        scheduler.setMemoryManager(memory);
        
        // Crear y mostrar la interfaz principal
        SimulatorGUI mainGUI = new SimulatorGUI(scheduler, memory, selectedProcesses);
        mainGUI.show();
        
        // Mostrar resumen de configuración
        JOptionPane.showMessageDialog(null,
            "Simulación configurada exitosamente!\n\n" +
            "Configuración aplicada:\n" +
            "• Algoritmo: " + selectedScheduler + 
            (selectedScheduler.contains("RR") ? " (Q=" + quantum + ")" : "") + "\n" +
            "• Memoria: " + selectedMemory + " (" + frames + " frames)\n" +
            "• Procesos: " + selectedProcesses.size() + " cargados\n\n" +
            "Presione 'Iniciar Simulación' para comenzar.",
            "Configuración Completada",
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    public void show() {
        frame.setVisible(true);
    }
    
    // Método principal para iniciar la aplicación
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ConfiguracionInicialGUI configGUI = new ConfiguracionInicialGUI();
            configGUI.show();
        });
    }
}