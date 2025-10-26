package simulator.ui;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author chalo
 */
import simulator.io.SimulationConfig;
import simulator.structures.CustomList;
import simulator.core.SchedulingAlgorithm;
import simulator.core.ProcessType;
import simulator.core.PCB;
import simulator.core.Scheduler;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Map; 


/**
 * Interfaz gráfica del simulador de planificación de procesos
 */
public class SimulationGUI extends JFrame {
    private Scheduler scheduler;
    private Timer simulationTimer;
    private int cycleDuration = 1000;

    private JLabel currentCycleLabel;
    private JLabel currentProcessLabel;
    private JLabel cpuStateLabel;
    private JLabel algorithmLabel;
    private JTable newQueueTable;
    private JTable readyQueueTable;
    private JTable blockedQueueTable;
    private JTable blockedSuspendedQueueTable; 
    private JTable readySuspendedQueueTable;   
    private JTable terminatedTable;
    private JTextArea logArea;
    private CustomList<MetricsDisplayGUI> activeMetricsWindows;
    private JComboBox<SchedulingAlgorithm> algorithmComboBox;
    private JSpinner cycleDurationSpinner;
    private JComboBox<String> timeUnitComboBox;
    private JButton startButton;
    private JButton stopButton;
    private JButton addProcessButton;
    private JButton openGraphsButton;
    private JButton openExtendedQueuesButton;
    private int processCounter = 1;
    
    private JLabel throughputLabel;
    private JLabel cpuUtilizationLabel;
    private JLabel avgWaitTimeLabel;
    private JLabel avgResponseTimeLabel;


    public SimulationGUI(Scheduler scheduler, SimulationConfig config) {
        this.scheduler = scheduler;
        this.activeMetricsWindows = new CustomList<>();

        initializeGUI();
        setupEventHandlers();

        int durationMs = config.getInitialCycleDuration();
        if (durationMs >= 1000 && durationMs % 1000 == 0) {
            timeUnitComboBox.setSelectedItem("s");
            cycleDurationSpinner.setValue(durationMs / 1000);
        } else {
            timeUnitComboBox.setSelectedItem("ms");
            cycleDurationSpinner.setValue(durationMs);
        }

        // Aplicar algoritmo
        algorithmComboBox.setSelectedItem(config.getStartAlgorithm());
    }
    
    private void initializeGUI() {
        setTitle("Simulador de Planificación de Procesos - Sistemas Operativos");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel topPanel = createTopPanel();
        add(topPanel, BorderLayout.NORTH);
        
        JPanel centerPanel = createCenterPanel();
        add(centerPanel, BorderLayout.CENTER);
        
        JPanel bottomPanel = createBottomPanel();
        add(bottomPanel, BorderLayout.SOUTH);
        
        pack();
        setSize(1200, 800);
        setLocationRelativeTo(null);
        throughputLabel = new JLabel("0.00 proc/s");
        cpuUtilizationLabel = new JLabel("0.0 %");
        avgWaitTimeLabel = new JLabel("0.00 ciclos");
        avgResponseTimeLabel = new JLabel("0.00 ciclos");
        
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                scheduler.shutdown();
            }
        });
    }

    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new GridLayout(4, 2, 5, 5));
        
        currentCycleLabel = new JLabel("0");
        currentProcessLabel = new JLabel("Ninguno");
        cpuStateLabel = new JLabel("Modo Kernel");
        algorithmLabel = new JLabel("FCFS");
        
        panel.add(new JLabel("Ciclo Global:"));
        panel.add(currentCycleLabel);
        panel.add(new JLabel("Proceso Actual:"));
        panel.add(currentProcessLabel);
        panel.add(new JLabel("Estado CPU:"));
        panel.add(cpuStateLabel);
        panel.add(new JLabel("Algoritmo:"));
        panel.add(algorithmLabel);
        
        panel.setBorder(BorderFactory.createTitledBorder("Estado del Sistema"));
        panel.setPreferredSize(new Dimension(800, 100));
        return panel;
    }
    
    private JPanel createCenterPanel() {
        JPanel panel = new JPanel(new GridLayout(3, 2, 5, 5)); // 3 filas, 2 columnas
        
        newQueueTable = createProcessTable();
        JScrollPane newScroll = new JScrollPane(newQueueTable);
        newScroll.setBorder(BorderFactory.createTitledBorder("Cola de Nuevos (NEW)"));
        panel.add(newScroll); // Fila 1, Col 1

        readyQueueTable = createProcessTable();
        JScrollPane readyScroll = new JScrollPane(readyQueueTable);
        readyScroll.setBorder(BorderFactory.createTitledBorder("Cola de Listos (READY)"));
        panel.add(readyScroll); // Fila 1, Col 2
        
        blockedQueueTable = createProcessTable();
        JScrollPane blockedScroll = new JScrollPane(blockedQueueTable);
        blockedScroll.setBorder(BorderFactory.createTitledBorder("Cola de Bloqueados"));
        panel.add(blockedScroll); // Fila 2, Col 1
        
        // --- REFACTOR ---
        blockedSuspendedQueueTable = createProcessTable();
        JScrollPane blockedSuspScroll = new JScrollPane(blockedSuspendedQueueTable);
        blockedSuspScroll.setBorder(BorderFactory.createTitledBorder("Bloqueados, Suspendidos"));
        panel.add(blockedSuspScroll); // Fila 2, Col 2
        
        readySuspendedQueueTable = createProcessTable();
        JScrollPane readySuspScroll = new JScrollPane(readySuspendedQueueTable);
        readySuspScroll.setBorder(BorderFactory.createTitledBorder("Listos, Suspendidos"));
        panel.add(readySuspScroll); // Fila 3, Col 1
        // ---
        
        terminatedTable = createProcessTable();
        JScrollPane terminatedScroll = new JScrollPane(terminatedTable);
        terminatedScroll.setBorder(BorderFactory.createTitledBorder("Procesos Terminados"));
        panel.add(terminatedScroll); // Fila 3, Col 2
        
        return panel;
    }
    
    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        JPanel controlPanel = new JPanel(new FlowLayout());
        
        algorithmComboBox = new JComboBox<>(SchedulingAlgorithm.values());
        controlPanel.add(new JLabel("Algoritmo:"));
        controlPanel.add(algorithmComboBox);
        
        cycleDurationSpinner = new JSpinner(new SpinnerNumberModel(1000, 100, 5000, 100));
        controlPanel.add(new JLabel("Duración Ciclo:"));
        controlPanel.add(cycleDurationSpinner);
        
        timeUnitComboBox = new JComboBox<>(new String[]{"ms", "s"});
        controlPanel.add(timeUnitComboBox);
        
        startButton = new JButton("Iniciar");
        stopButton = new JButton("Detener");
        addProcessButton = new JButton("Agregar Proceso");
        
        controlPanel.add(startButton);
        controlPanel.add(stopButton);
        controlPanel.add(addProcessButton);
        openGraphsButton = new JButton("Abrir Gráficos");
        controlPanel.add(openGraphsButton);

        openExtendedQueuesButton = new JButton("Abrir Colas Ext.");
        controlPanel.add(openExtendedQueuesButton);
        
        logArea = new JTextArea(10, 80);
        logArea.setEditable(false);
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Log de Eventos"));
        
        panel.add(controlPanel, BorderLayout.NORTH);
        panel.add(logScroll, BorderLayout.CENTER);

        JTextArea algorithmArea = new JTextArea(5, 80);
        algorithmArea.setEditable(false);
        algorithmArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        StringBuilder algoText = new StringBuilder("Tipos de Algoritmos de Planificación:\n");
        for (SchedulingAlgorithm alg : SchedulingAlgorithm.values()) { //
            switch (alg) {
                case FCFS:
                    algoText.append("FCFS:     First Come First Served\n");
                    break;
                case SJF:
                    algoText.append("SJF:      Shortest Job First\n");
                    break;
                case RR:
                    algoText.append("RR:       Round Robin\n");
                    break;
                case PRIORITY:
                    algoText.append("PRIORITY: Planificación por Prioridad (Estática)\n");
                    break;
                case SRTF:
                    algoText.append("SRTF:     Shortest Remaining Time First\n");
                    break;
                case MLFQ:
                    algoText.append("MLFQ:     Multi-Level Feedback Queue\n");
                    break;
                case HRRN:
                    algoText.append("HRRN:     Highest Response Ratio Next\n");
                    break;
            }
        }
        algorithmArea.setText(algoText.toString());
        
        JScrollPane algorithmScroll = new JScrollPane(algorithmArea);
        algorithmScroll.setBorder(BorderFactory.createTitledBorder("Leyenda de Algoritmos"));
        
        panel.add(algorithmScroll, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JTable createProcessTable() {
        String[] columnNames = {"ID", "Nombre", "Estado", "PC", "MAR", "Instrucciones Restantes", "Tipo", "Prioridad",
                                "Memoria (MB)"};
        DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(model);
        table.setAutoCreateRowSorter(true);
        return table;
    }

    private void setupEventHandlers() {
        int guiRefreshRate = 33; 
        simulationTimer = new Timer(guiRefreshRate, (ActionEvent e) -> {
            updateGUI(); 
        });

        startButton.addActionListener((e) -> {
            scheduler.start(); 
            simulationTimer.start(); 

            startButton.setEnabled(false); // Deshabilitar botón
            stopButton.setEnabled(true);   // Habilitar botón
            log("Simulación iniciada.");
        });

        stopButton.addActionListener((e) -> {
            simulationTimer.stop(); 
            scheduler.shutdown(); 
            for (int i = 0; i < activeMetricsWindows.size(); i++) {
                MetricsDisplayGUI metricsWindow = activeMetricsWindows.get(i);
                metricsWindow.dispatchEvent(new WindowEvent(metricsWindow, WindowEvent.WINDOW_CLOSING));
            }
            activeMetricsWindows.clear();
            startButton.setEnabled(true);   // Habilitar botón
            stopButton.setEnabled(false); // Deshabilitar botón
            log("Simulación detenida por el usuario.");
        });

        addProcessButton.addActionListener(e -> addProcessDialog());

        algorithmComboBox.addActionListener((e) -> {
            SchedulingAlgorithm selected = (SchedulingAlgorithm) algorithmComboBox.getSelectedItem();
            if (selected != null) {
                scheduler.setSchedulingAlgorithm(selected); 
                log("Algoritmo cambiado a: " + selected);
            }
        });

        javax.swing.event.ChangeListener spinnerListener = (e) -> {
                int newDurationValue = (Integer) cycleDurationSpinner.getValue();
                String selectedUnit = (String) timeUnitComboBox.getSelectedItem();
                int newDurationMs;

                if ("s".equals(selectedUnit)) {
                    newDurationMs = newDurationValue * 1000;
                } else {
                    newDurationMs = newDurationValue;
                }

                if (newDurationMs > 0) {
                    scheduler.setCycleDuration(newDurationMs); 
                    log("Duración del ciclo cambiada a: " + newDurationValue + " " + selectedUnit);
                }
            };
            cycleDurationSpinner.addChangeListener(spinnerListener); 
            timeUnitComboBox.addActionListener((e) -> {
                convertTimeUnits(spinnerListener);
            });

        openGraphsButton.addActionListener(e -> {
            MetricsDisplayGUI graphsWindow = new MetricsDisplayGUI("Gráficos de Rendimiento", "Gráficos", this);
            activeMetricsWindows.add(graphsWindow);
            graphsWindow.setVisible(true);
        });

        openExtendedQueuesButton.addActionListener(e -> {
            MetricsDisplayGUI queuesWindow = new MetricsDisplayGUI("Vistas Extendidas de Colas", "Colas Extendidas", this);
            activeMetricsWindows.add(queuesWindow);
            queuesWindow.setVisible(true);
        });

        stopButton.setEnabled(false);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (simulationTimer != null) {
                    simulationTimer.stop();
                }
                scheduler.shutdown();
                System.exit(0); 
            }
        });
    }
    
    private void addProcessDialog() {
        // --- Componentes del diálogo ---
        JTextField nameField = new JTextField("Process_" + processCounter);
        JComboBox<ProcessType> typeCombo = new JComboBox<>(ProcessType.values());
        JSpinner instructionsSpinner = new JSpinner(new SpinnerNumberModel(15, 5, 50, 5));
        
        // --- Componentes de Excepción (ahora variables locales) ---
        JLabel exceptionLabel = new JLabel("Ciclos para Excepción:");
        JSpinner exceptionSpinner = new JSpinner(new SpinnerNumberModel(4, 2, 10, 1));
        JLabel completionLabel = new JLabel("Ciclos para Completar Excepción:");
        JSpinner completionSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 999, 1)); // Límite corregido
        // ---
        
        JSpinner prioritySpinner = new JSpinner(new SpinnerNumberModel(2, 1, 3, 1));
        JSpinner memorySpinner = new JSpinner(new SpinnerNumberModel(64, 16, 256, 16));

        // --- Panel del diálogo ---
        JPanel panel = new JPanel(new GridLayout(7, 2, 5, 5)); // GridLayout con espacio vertical
        panel.add(new JLabel("Nombre:"));
        panel.add(nameField);
        panel.add(new JLabel("Tipo:"));
        panel.add(typeCombo);
        panel.add(new JLabel("Instrucciones:"));
        panel.add(instructionsSpinner);
        
        // --- Añadir componentes de excepción al panel ---
        panel.add(exceptionLabel);
        panel.add(exceptionSpinner);
        panel.add(completionLabel);
        panel.add(completionSpinner);
        // ---
        
        panel.add(new JLabel("Prioridad (1=Alta, 3=Baja):"));
        panel.add(prioritySpinner);
        panel.add(new JLabel("Tamaño de Memoria (MB):"));
        panel.add(memorySpinner);

        // --- LÓGICA PARA OCULTAR/MOSTRAR ---
        // Función auxiliar para actualizar la visibilidad
        Runnable updateVisibility = () -> {
            boolean isIOBound = (typeCombo.getSelectedItem() == ProcessType.IO_BOUND);
            exceptionLabel.setVisible(isIOBound);
            exceptionSpinner.setVisible(isIOBound);
            completionLabel.setVisible(isIOBound);
            completionSpinner.setVisible(isIOBound);
            
            // Reajustar el tamaño del diálogo si los componentes se ocultan/muestran
            Window window = SwingUtilities.getWindowAncestor(panel);
            if (window != null) {
                window.pack(); // Ajusta el tamaño al contenido
            }
        };

        // Añadir el ActionListener al ComboBox
        typeCombo.addActionListener(e -> updateVisibility.run());
        
        // Ejecutar una vez al inicio para establecer el estado inicial correcto
        updateVisibility.run(); 
        // --- FIN DE LÓGICA ---

        // --- Mostrar el diálogo ---
        int result = JOptionPane.showConfirmDialog(this, panel, "Agregar Proceso", 
                                                  JOptionPane.OK_CANCEL_OPTION, 
                                                  JOptionPane.PLAIN_MESSAGE); // Usar PLAIN_MESSAGE sin icono

        // --- Procesar resultado ---
        if (result == JOptionPane.OK_OPTION) {
            String name = nameField.getText();
            ProcessType type = (ProcessType) typeCombo.getSelectedItem();
            int instructions = (Integer) instructionsSpinner.getValue();
            int priority = (Integer) prioritySpinner.getValue();
            int memorySize = (Integer) memorySpinner.getValue();
            
            // --- Obtener valores de excepción SÓLO si es IO_BOUND ---
            int exceptionCycles = 0;
            int completionCycles = 0;
            if (type == ProcessType.IO_BOUND) {
                exceptionCycles = (Integer) exceptionSpinner.getValue();
                completionCycles = (Integer) completionSpinner.getValue();
            }
            // ---

            PCB process = new PCB(name, type, instructions, exceptionCycles, completionCycles, priority , memorySize, scheduler);
            scheduler.addProcess(process);
            log("Nuevo proceso creado: " + name + " (" + type + ", " + instructions + " instrucciones, Prioridad: "+ priority + ")" );
            log("Nuevo proceso " + name + " agregado a la cola NEW.");
            updateGUI();
            processCounter++;
        }
    }
    
    private void updateGUI() {
        SwingUtilities.invokeLater(() -> {
            
            long cycle = scheduler.getGlobalCycleSnapshot();
            PCB p = scheduler.getCurrentProcessSnapshot();
            boolean osRunning = scheduler.getIsOperatingSystemRunningSnapshot();
            SchedulingAlgorithm alg = scheduler.getAlgorithmSnapshot();
            Map<String, Double> metrics = scheduler.getPerformanceMetricsSnapshot();
            
            boolean cpuIdle = scheduler.getIsCpuIdleSnapshot();
            
            currentCycleLabel.setText(String.valueOf(cycle));
            currentProcessLabel.setText(p != null ? p.getName() + " (ID: " + p.getId() + ")" : "Ninguno");
            cpuStateLabel.setText(cpuIdle ? "Modo Kernel" : "Modo Usuario");
            algorithmLabel.setText(alg.toString());
          
            updateTableFromCustomList(newQueueTable, scheduler.getNewQueueSnapshot());
            updateTable(readyQueueTable, scheduler.getReadyQueueSnapshot());
            updateTableFromCustomList(blockedQueueTable, scheduler.getBlockedQueueSnapshot());
            updateTableFromCustomList(blockedSuspendedQueueTable, scheduler.getBlockedSuspendedQueueSnapshot());
            updateTableFromCustomList(readySuspendedQueueTable, scheduler.getReadySuspendedQueueSnapshot());
            updateTableFromCustomList(terminatedTable, scheduler.getTerminatedQueueSnapshot());
            
            if (osRunning) {
                for (int i = 0; i < activeMetricsWindows.size(); i++) {
                    MetricsDisplayGUI metricsWindow = activeMetricsWindows.get(i);
                    if (metricsWindow != null) {
                        metricsWindow.updateDisplay(scheduler);
                    }
                }
            }
            
            throughputLabel.setText(String.format("%.2f proc/s", metrics.get("Throughput")));
            cpuUtilizationLabel.setText(String.format("%.1f %%", metrics.get("CPU_Utilization") * 100));
            avgWaitTimeLabel.setText(String.format("%.2f ciclos", metrics.get("Avg_Wait_Time")));
            avgResponseTimeLabel.setText(String.format("%.2f ciclos", metrics.get("Avg_Response_Time")));
        });
    }
    
    private void updateTableFromCustomList(JTable table, CustomList<PCB> processList) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);
        
        for (int i = 0; i < processList.size(); i++) {
        PCB p = processList.get(i);
        model.addRow(new Object[]{
            p.getId(), p.getName(), p.getState(), p.getProgramCounter(),
            p.getMAR(), p.getRemainingInstructions(), p.getType(),
            p.getPriority(), p.getMemorySize()
        });
    }
    }
    
    private void updateTable(JTable table, Object[] processes) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);

        for (Object obj : processes) {
            if (obj instanceof PCB) {
                PCB p = (PCB) obj;
                model.addRow(new Object[]{
                    p.getId(), p.getName(), p.getState(), p.getProgramCounter(),
                    p.getMAR(), p.getRemainingInstructions(), p.getType(),
                    p.getPriority(), p.getMemorySize()
                });
            }
        }
    }
    
    private void convertTimeUnits(javax.swing.event.ChangeListener spinnerListener) {
        cycleDurationSpinner.removeChangeListener(spinnerListener);

        int currentValue = (Integer) cycleDurationSpinner.getValue();
        String selectedUnit = (String) timeUnitComboBox.getSelectedItem();
        SpinnerNumberModel model = (SpinnerNumberModel) cycleDurationSpinner.getModel();
        int newValue;

        if ("s".equals(selectedUnit)) {
            newValue = Math.max(1, currentValue / 1000);
            model.setMinimum(1);
            model.setMaximum(10);
            model.setStepSize(1);
            if (newValue > 10) {
                newValue = 10; 
            }
        } else {
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

        int newDurationMs = (int) model.getValue();
        if ("s".equals(selectedUnit)) {
            newDurationMs *= 1000;
        }
        scheduler.setCycleDuration(newDurationMs);
        log("Unidad de ciclo cambiada a: " + model.getValue() + " " + selectedUnit);
    }
    
    private void log(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append("Ciclo " + scheduler.getGlobalCycleSnapshot() + ": " + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
    
    public void removeMetricsWindow(MetricsDisplayGUI window) {
        activeMetricsWindows.remove(window);
    }
}