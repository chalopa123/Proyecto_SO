/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author chalo
 */
// Archivo: WelcomeGUI.java
// (Sin 'package' al inicio)

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.Map;

/**
 * Pantalla de bienvenida para configurar la simulación.
 */
public class WelcomeGUI extends JFrame {

    // Rango de memoria realista (en MB)
    private static final int MIN_MEMORY = 512;
    private static final int MAX_MEMORY = 8192;

    private JSpinner totalMemorySpinner;
    private JComboBox<SchedulingAlgorithm> algorithmComboBox;
    private JSpinner cycleDurationSpinner;
    private JComboBox<String> timeUnitComboBox;
    
    private JButton saveButton;
    private JButton loadButton;
    private JButton startButton;

    private ConfigManager configManager;

    public WelcomeGUI() {
        this.configManager = new ConfigManager();
        
        setTitle("Configuración de la Simulación");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(450, 250);
        setLocationRelativeTo(null); // Centrar
        setResizable(false);
        
        initComponents();
        initEventHandlers();
        
        // Cargar una configuración por defecto
        loadDefaultSettings();
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- Panel de Configuración ---
        JPanel configPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        
        // 3.1. Memoria
        configPanel.add(new JLabel("Memoria Total de Simulación (MB):"));
        totalMemorySpinner = new JSpinner(new SpinnerNumberModel(1024, MIN_MEMORY, MAX_MEMORY, 512));
        configPanel.add(totalMemorySpinner);

        // 3.2. Algoritmo
        configPanel.add(new JLabel("Algoritmo de Planificación Inicial:"));
        algorithmComboBox = new JComboBox<>(SchedulingAlgorithm.values());
        configPanel.add(algorithmComboBox);

        // 3.3. Duración de Ciclo
        configPanel.add(new JLabel("Duración de Ciclo Inicial:"));
        JPanel durationPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        cycleDurationSpinner = new JSpinner(new SpinnerNumberModel(500, 100, 5000, 100));
        JSpinner.NumberEditor durationEditor = new JSpinner.NumberEditor(cycleDurationSpinner, "#");
        cycleDurationSpinner.setEditor(durationEditor);
        timeUnitComboBox = new JComboBox<>(new String[]{"ms", "s"});
        durationPanel.add(cycleDurationSpinner);
        durationPanel.add(timeUnitComboBox);
        configPanel.add(durationPanel);

        mainPanel.add(configPanel, BorderLayout.CENTER);

        // --- Panel de Botones ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        
        // 3.4.1. Guardar
        saveButton = new JButton("Guardar Config.");
        buttonPanel.add(saveButton);
        
        // 3.4.2. Cargar
        loadButton = new JButton("Cargar Config.");
        buttonPanel.add(loadButton);
        
        // 3.4.3. Iniciar
        startButton = new JButton("Iniciar Simulación");
        buttonPanel.add(startButton);
        
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }
    
    private void loadDefaultSettings() {
        totalMemorySpinner.setValue(1024);
        algorithmComboBox.setSelectedItem(SchedulingAlgorithm.FCFS);
        timeUnitComboBox.setSelectedItem("ms");        
        cycleDurationSpinner.setValue(1000); 
    }

    private void initEventHandlers() {
        // --- 3.4.3. Iniciar Simulación ---
        startButton.addActionListener(e -> startSimulation());

        // --- 3.4.1. Guardar Configuración ---
        saveButton.addActionListener(e -> saveConfiguration());
        
        // --- 3.4.2. Cargar Configuración ---
        loadButton.addActionListener(e -> loadConfiguration());
        
        javax.swing.event.ChangeListener spinnerListener = (e) -> {
            // El valor se lee al Iniciar/Guardar
        };
        cycleDurationSpinner.addChangeListener(spinnerListener); // Asignarlo

        // 2. Listener para el COMBOBOX
        timeUnitComboBox.addActionListener((e) -> {
            // Llamar a convertTimeUnits, pasándole el listener del spinner
            convertTimeUnits(spinnerListener);
        });
    }

    private void startSimulation() {
        // 3.1. Validar Memoria
        int memory = (Integer) totalMemorySpinner.getValue();
        if (memory < MIN_MEMORY || memory > MAX_MEMORY) {
            JOptionPane.showMessageDialog(this, 
                "Error: La memoria debe estar entre " + MIN_MEMORY + " y " + MAX_MEMORY + " MB.",
                "Error de Configuración", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // 3.2. Obtener Algoritmo
        SchedulingAlgorithm algorithm = (SchedulingAlgorithm) algorithmComboBox.getSelectedItem();

        // 3.3. Obtener Duración del Ciclo (siempre en ms)
        int duration = (Integer) cycleDurationSpinner.getValue();
        if ("s".equals(timeUnitComboBox.getSelectedItem())) {
            duration *= 1000;
        }

        // Crear el objeto de configuración
        SimulationConfig config = new SimulationConfig(memory, algorithm, duration);

        // Crear el Scheduler y la GUI principal
        Scheduler scheduler = new Scheduler(config);
        SimulationGUI simulationGUI = new SimulationGUI(scheduler, config);

        // Mostrar la GUI de simulación y cerrar esta
        simulationGUI.setVisible(true);
        this.dispose();
    }
    
    private void saveConfiguration() {
        JFileChooser fileChooser = new JFileChooser("src/archivos/");
        fileChooser.setDialogTitle("Guardar Configuración");
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                String filePath = fileChooser.getSelectedFile().getPath();
                if (!filePath.endsWith(".csv")) {
                    filePath += ".csv";
                }
                
                // Recolectar datos de la GUI
                int memory = (Integer) totalMemorySpinner.getValue();
                SchedulingAlgorithm algorithm = (SchedulingAlgorithm) algorithmComboBox.getSelectedItem();
                int duration = (Integer) cycleDurationSpinner.getValue();
                if ("s".equals(timeUnitComboBox.getSelectedItem())) {
                    duration *= 1000; // Siempre guardar en ms
                }

                SimulationConfig config = new SimulationConfig(memory, algorithm, duration);
                configManager.saveConfig(filePath, config);

                JOptionPane.showMessageDialog(this, "Configuración guardada exitosamente.");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, 
                    "Error al guardar: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void loadConfiguration() {
        JFileChooser fileChooser = new JFileChooser("src/archivos/");
        fileChooser.setDialogTitle("Cargar Configuración");
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                String filePath = fileChooser.getSelectedFile().getPath();
                Map<String, String> configMap = configManager.loadConfig(filePath);
                
                int memory = Integer.parseInt(configMap.get("totalMemory"));
                SchedulingAlgorithm algorithm = SchedulingAlgorithm.valueOf(configMap.get("startAlgorithm"));
                int durationMs = Integer.parseInt(configMap.get("cycleDuration"));
                
                totalMemorySpinner.setValue(memory);
                algorithmComboBox.setSelectedItem(algorithm);
                
                // --- INICIO DE LA LÓGICA CORREGIDA ---
                SpinnerNumberModel model = (SpinnerNumberModel) cycleDurationSpinner.getModel();

                if (durationMs >= 1000 && durationMs % 1000 == 0) {
                    // Cargar como SEGUNDOS
                    int sValue = durationMs / 1000;
                    timeUnitComboBox.setSelectedItem("s"); // Esto dispara convertTimeUnits
                    
                    // Re-obtener el modelo por si convertTimeUnits lo cambió
                    model = (SpinnerNumberModel) cycleDurationSpinner.getModel();
                    // Forzar el valor a estar DENTRO del rango (max 10s)
                    model.setValue(Math.min(sValue, 10)); 

                } else {
                    // Cargar como MILISEGUNDOS
                    timeUnitComboBox.setSelectedItem("ms"); // Esto dispara convertTimeUnits
                    
                    // Re-obtener el modelo por si convertTimeUnits lo cambió
                    model = (SpinnerNumberModel) cycleDurationSpinner.getModel();
                    // Forzar el valor a estar DENTRO del rango (max 10000ms)
                    model.setValue(Math.min(durationMs, 10000));
                }
                // --- FIN DE LA LÓGICA CORREGIDA ---
                
                JOptionPane.showMessageDialog(this, "Configuración cargada exitosamente.");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, 
                    "Error al cargar: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Lógica de conversión de tiempo.
     */
    private void convertTimeUnits(javax.swing.event.ChangeListener spinnerListener) {
        cycleDurationSpinner.removeChangeListener(spinnerListener);

        int currentValue = (Integer) cycleDurationSpinner.getValue();
        String selectedUnit = (String) timeUnitComboBox.getSelectedItem();
        SpinnerNumberModel model = (SpinnerNumberModel) cycleDurationSpinner.getModel();
        int newValue;

        if ("s".equals(selectedUnit)) {
            // --- Se cambió A SEGUNDOS ---
            newValue = Math.max(1, currentValue / 1000);
            
            model.setMinimum(1);
            model.setMaximum(10); // Límite de 10s
            model.setStepSize(1);
            
            // Forzar el valor a estar DENTRO del nuevo rango
            if (newValue > 10) {
                newValue = 10;
            }

        } else {
            // --- Se cambió A MILISEGUNDOS ---
            newValue = currentValue * 1000;

            model.setMinimum(100);
            model.setMaximum(10000); // Límite de 10000ms
            model.setStepSize(100);
            
            // Forzar el valor a estar DENTRO del nuevo rango
            if (newValue > 10000) {
                newValue = 10000;
            }
            if (newValue < 100) {
                newValue = 100;
            }
        }
        
        model.setValue(newValue); // Asignar el valor ya verificado

        cycleDurationSpinner.addChangeListener(spinnerListener);
    }
}
