package simulator;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author chalo
 */


import simulator.io.SimulationConfig;
import simulator.io.ConfigManager;
import simulator.ui.SimulationGUI;
import simulator.core.Scheduler;
import javax.swing.*;
import java.awt.*;
import java.util.Map;
import simulator.core.SchedulingAlgorithm;

/**
 * Pantalla de bienvenida para configurar la simulación.
 */
public class WelcomeGUI extends JFrame {

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
        setLocationRelativeTo(null); 
        setResizable(false);
        
        initComponents();
        initEventHandlers();

        loadDefaultSettings();
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel configPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        
        // Memoria
        configPanel.add(new JLabel("Memoria Total de Simulación (MB):"));
        totalMemorySpinner = new JSpinner(new SpinnerNumberModel(1024, MIN_MEMORY, MAX_MEMORY, 512));
        configPanel.add(totalMemorySpinner);

        // Algoritmo
        configPanel.add(new JLabel("Algoritmo de Planificación Inicial:"));
        algorithmComboBox = new JComboBox<>(SchedulingAlgorithm.values());
        configPanel.add(algorithmComboBox);

        // Duración de Ciclo
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

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        
        // Guardar
        saveButton = new JButton("Guardar Config.");
        buttonPanel.add(saveButton);
        
        // Cargar
        loadButton = new JButton("Cargar Config.");
        buttonPanel.add(loadButton);
        
        // Iniciar
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
        startButton.addActionListener(e -> startSimulation());

        saveButton.addActionListener(e -> saveConfiguration());
        
        loadButton.addActionListener(e -> loadConfiguration());
        
        javax.swing.event.ChangeListener spinnerListener = (e) -> {
        };
        cycleDurationSpinner.addChangeListener(spinnerListener); 

        timeUnitComboBox.addActionListener((e) -> {
            convertTimeUnits(spinnerListener);
        });
    }

    private void startSimulation() {
        int memory = (Integer) totalMemorySpinner.getValue();
        if (memory < MIN_MEMORY || memory > MAX_MEMORY) {
            JOptionPane.showMessageDialog(this, 
                "Error: La memoria debe estar entre " + MIN_MEMORY + " y " + MAX_MEMORY + " MB.",
                "Error de Configuración", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        SchedulingAlgorithm algorithm = (SchedulingAlgorithm) algorithmComboBox.getSelectedItem();

        int duration = (Integer) cycleDurationSpinner.getValue();
        if ("s".equals(timeUnitComboBox.getSelectedItem())) {
            duration *= 1000;
        }

        SimulationConfig config = new SimulationConfig(memory, algorithm, duration);

        Scheduler scheduler = new Scheduler(config);
        SimulationGUI simulationGUI = new SimulationGUI(scheduler, config);

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
                
                int memory = (Integer) totalMemorySpinner.getValue();
                SchedulingAlgorithm algorithm = (SchedulingAlgorithm) algorithmComboBox.getSelectedItem();
                int duration = (Integer) cycleDurationSpinner.getValue();
                if ("s".equals(timeUnitComboBox.getSelectedItem())) {
                    duration *= 1000; 
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
                
                SpinnerNumberModel model = (SpinnerNumberModel) cycleDurationSpinner.getModel();

                if (durationMs >= 1000 && durationMs % 1000 == 0) {
                    int sValue = durationMs / 1000;
                    timeUnitComboBox.setSelectedItem("s"); 
                    model = (SpinnerNumberModel) cycleDurationSpinner.getModel();
                    model.setValue(Math.min(sValue, 10)); 

                } else {
                    timeUnitComboBox.setSelectedItem("ms"); 
                    
                    model = (SpinnerNumberModel) cycleDurationSpinner.getModel();
                    model.setValue(Math.min(durationMs, 10000));
                }
                
                JOptionPane.showMessageDialog(this, "Configuración cargada exitosamente.");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, 
                    "Error al cargar: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * conversión de tiempo.
     */
    private void convertTimeUnits(javax.swing.event.ChangeListener spinnerListener) {
        cycleDurationSpinner.removeChangeListener(spinnerListener);

        int currentValue = (Integer) cycleDurationSpinner.getValue();
        String selectedUnit = (String) timeUnitComboBox.getSelectedItem();
        SpinnerNumberModel model = (SpinnerNumberModel) cycleDurationSpinner.getModel();
        int newValue;

        if ("s".equals(selectedUnit)) {
            // ms a seg
            newValue = Math.max(1, currentValue / 1000);
            
            model.setMinimum(1);
            model.setMaximum(10); 
            model.setStepSize(1);
            
            if (newValue > 10) {
                newValue = 10;
            }

        } else {
            // seg a ms
            newValue = currentValue * 1000;

            model.setMinimum(100);
            model.setMaximum(10000); 
            model.setStepSize(100);
            
            if (newValue > 10000) {
                newValue = 10000;
            }
            if (newValue < 100) {
                newValue = 100;
            }
        }
        
        model.setValue(newValue); 

        cycleDurationSpinner.addChangeListener(spinnerListener);
    }
}
