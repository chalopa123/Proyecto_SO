/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author chalo
 */
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Map; // Asegúrate de importar Map

// ... (resto de importaciones si las hay) ...

/**
 * Interfaz gráfica del simulador de planificación de procesos
 * Con correcciones de diseño y parámetros no utilizados
 */
public class SimulationGUI extends JFrame {
    private Scheduler scheduler;
    private Timer simulationTimer;
    private int cycleDuration = 1000;
    
    // ... (declaraciones de componentes de la GUI: JLabels, JTables, etc.) ...
    private JLabel currentCycleLabel;
    private JLabel currentProcessLabel;
    private JLabel cpuStateLabel;
    private JLabel algorithmLabel;
    private JTable newQueueTable;
    private JTable readyQueueTable;
    private JTable blockedQueueTable;
    private JTable suspendedQueueTable;
    private JTable terminatedTable;
    private JTextArea logArea;
    private CustomList<MetricsDisplayGUI> activeMetricsWindows;
    private JComboBox<SchedulingAlgorithm> algorithmComboBox;
    private JSpinner cycleDurationSpinner;
    private JComboBox<String> timeUnitComboBox;
    private JButton startButton;
    private JButton stopButton;
    private JButton addProcessButton;
    private JButton saveCycleButton;
    private JButton loadCycleButton;
    private JButton openGraphsButton;
    private JButton openExtendedQueuesButton;
    private int processCounter = 1;
    
    // Componentes de métricas (asegúrate de que estén declarados)
    private JLabel throughputLabel;
    private JLabel cpuUtilizationLabel;
    private JLabel avgWaitTimeLabel;
    private JLabel avgResponseTimeLabel;


    public SimulationGUI(Scheduler scheduler, SimulationConfig config) {
        this.scheduler = scheduler; // Se recibe
        this.activeMetricsWindows = new CustomList<>();

        initializeGUI();
        setupEventHandlers();

        // --- APLICAR CONFIGURACIÓN INICIAL ---
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
        
        // CORRECCIÓN: Panel superior sin duplicados
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
    
    /**
     * CORRECCIÓN: Panel superior sin etiquetas duplicadas
     */
    private JPanel createTopPanel() {
        // Usar GridLayout de 4 filas x 2 columnas para evitar duplicados
        JPanel panel = new JPanel(new GridLayout(4, 2, 5, 5));
        
        currentCycleLabel = new JLabel("0");
        currentProcessLabel = new JLabel("Ninguno");
        cpuStateLabel = new JLabel("Modo Kernel");
        algorithmLabel = new JLabel("FCFS");
        
        // Agregar etiquetas y valores en pares
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
        // Cambiamos el GridLayout de 2x2 a 3x2
        JPanel panel = new JPanel(new GridLayout(3, 2, 5, 5));
        
        newQueueTable = createProcessTable();
        JScrollPane newScroll = new JScrollPane(newQueueTable);
        newScroll.setBorder(BorderFactory.createTitledBorder("Cola de Nuevos (NEW)"));

        readyQueueTable = createProcessTable();
        JScrollPane readyScroll = new JScrollPane(readyQueueTable);
        readyScroll.setBorder(BorderFactory.createTitledBorder("Cola de Listos (READY)"));
        
        blockedQueueTable = createProcessTable();
        JScrollPane blockedScroll = new JScrollPane(blockedQueueTable);
        blockedScroll.setBorder(BorderFactory.createTitledBorder("Cola de Bloqueados"));
        
        suspendedQueueTable = createProcessTable();
        JScrollPane suspendedScroll = new JScrollPane(suspendedQueueTable);
        suspendedScroll.setBorder(BorderFactory.createTitledBorder("Cola de Suspendidos"));
        
        terminatedTable = createProcessTable();
        JScrollPane terminatedScroll = new JScrollPane(terminatedTable);
        terminatedScroll.setBorder(BorderFactory.createTitledBorder("Procesos Terminados"));
        
        // Añadimos las tablas en el nuevo orden
        panel.add(newScroll);       // (Fila 1, Col 1)
        panel.add(readyScroll);     // (Fila 1, Col 2)
        panel.add(blockedScroll);   // (Fila 2, Col 1)
        panel.add(suspendedScroll); // (Fila 2, Col 2)
        panel.add(terminatedScroll);// (Fila 3, Col 1)
        // (El sexto slot, Fila 3 Col 2, quedará vacío)
        
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

        // --- AÑADIR ESTAS LÍNEAS ---
        JTextArea algorithmArea = new JTextArea(5, 80);
        algorithmArea.setEditable(false);
        algorithmArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        
        // Obtenemos los algoritmos del Enum
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
    
    // En SimulationGUI.java

private void setupEventHandlers() {
    
    // 1. Definir la tasa de refresco de la GUI (p.ej., 30 FPS)
    // Esto es independiente de la duración del ciclo de simulación.
    int guiRefreshRate = 33; // Aprox. 30 veces por segundo (1000ms / 30fps = 33.3ms)

    // 2. ÚNICA inicialización del Timer.
    // Este timer SOLO se encarga de actualizar la GUI.
    // NO debe llamar a scheduler.executeCycle().
    simulationTimer = new Timer(guiRefreshRate, (ActionEvent e) -> {
        updateGUI(); 
    });

    // 3. Configurar Listeners
    startButton.addActionListener((e) -> {
        scheduler.start(); // Inicia el hilo de simulación (Scheduler.run())
        simulationTimer.start(); // Inicia el hilo de refresco de la GUI (este Timer)
        
        startButton.setEnabled(false); // Deshabilitar botón
        stopButton.setEnabled(true);   // Habilitar botón
        log("Simulación iniciada.");
    });
    
    stopButton.addActionListener((e) -> {
        simulationTimer.stop(); // Detiene el refresco de la GUI
        scheduler.shutdown(); // Detiene el hilo de simulación
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
    
    cycleDurationSpinner.addChangeListener((e) -> {
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
    });

    // 2. Crear un ActionListener simple para el COMBOBOX
    timeUnitComboBox.addActionListener((e) -> {
        // Simplemente re-ejecuta la misma lógica que el spinner
        // para actualizar la duración
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
    
    // 4. Estado inicial de los botones
    stopButton.setEnabled(false);
    
    // 5. Manejador de cierre de ventana
    addWindowListener(new WindowAdapter() {
        @Override
        public void windowClosing(WindowEvent e) {
            if (simulationTimer != null) {
                simulationTimer.stop();
            }
            scheduler.shutdown();
            System.exit(0); // Asegurarse de que la app se cierra
        }
    });
}
    
    /**
     * CORRECCIÓN: Métodos sin parámetros ActionEvent no utilizados
     */
    private void startSimulation() {
        simulationTimer.start();
        startButton.setEnabled(false);
        stopButton.setEnabled(true);
        log("Simulación iniciada");
    }
    
    private void addProcessDialog() {
        JTextField nameField = new JTextField("Process_" + processCounter);
        JComboBox<ProcessType> typeCombo = new JComboBox<>(ProcessType.values());

        // VALORES MEJORADOS PARA TESTING:
        JSpinner instructionsSpinner = new JSpinner(new SpinnerNumberModel(15, 5, 50, 5));
        JSpinner exceptionSpinner = new JSpinner(new SpinnerNumberModel(4, 2, 10, 1));
        JSpinner completionSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 5, 1));
        JSpinner prioritySpinner = new JSpinner(new SpinnerNumberModel(2, 1, 3, 1));
        JSpinner memorySpinner = new JSpinner(new SpinnerNumberModel(64, 16, 256, 16));

        JPanel panel = new JPanel(new GridLayout(7, 2));
        panel.add(new JLabel("Nombre:"));
        panel.add(nameField);
        panel.add(new JLabel("Tipo:"));
        panel.add(typeCombo);
        panel.add(new JLabel("Instrucciones:"));
        panel.add(instructionsSpinner);
        panel.add(new JLabel("Ciclos para Excepción:"));
        panel.add(exceptionSpinner);
        panel.add(new JLabel("Ciclos para Completar Excepción:"));
        panel.add(completionSpinner);
        panel.add(new JLabel("Prioridad (1=Alta, 3=Baja):"));
        panel.add(prioritySpinner);
        panel.add(new JLabel("Tamaño de Memoria (MB):"));
        panel.add(memorySpinner);

        int result = JOptionPane.showConfirmDialog(this, panel, "Agregar Proceso", 
                                                  JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            String name = nameField.getText();
            ProcessType type = (ProcessType) typeCombo.getSelectedItem();
            int instructions = (Integer) instructionsSpinner.getValue();
            int exceptionCycles = (Integer) exceptionSpinner.getValue();
            int completionCycles = (Integer) completionSpinner.getValue();
            int priority = (Integer) prioritySpinner.getValue();
            int memorySize = (Integer) memorySpinner.getValue();

            PCB process = new PCB(name, type, instructions, exceptionCycles, completionCycles, priority , memorySize, scheduler);
            scheduler.addProcess(process);
            log("Nuevo proceso creado: " + name + " (" + type + ", " + instructions + " instrucciones, Prioridad: "+ priority + ")" );
            log("Nuevo proceso " + name + " agregado a la cola NEW.");
            updateGUI();
            processCounter++;
        }
    }

    
    private void executeSimulationCycle() {
        scheduler.executeCycle();
        updateGUI();
    }
    
    private void updateGUI() {
        // SwingUtilities.invokeLater es una buena práctica, aunque el Timer ya usa el EDT
        SwingUtilities.invokeLater(() -> {
            
            // Todos estos métodos get...Snapshot() ahora leen el caché
            // o usan locks muy rápidos. No bloquearán la GUI.
            
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
            
            // Actualizar tablas usando los snapshots (que vienen del caché)
            updateTableFromCustomList(newQueueTable, scheduler.getNewQueueSnapshot());
            updateTable(readyQueueTable, scheduler.getReadyQueueSnapshot());
            updateTableFromCustomList(blockedQueueTable, scheduler.getBlockedQueueSnapshot());
            updateTableFromCustomList(suspendedQueueTable, scheduler.getSuspendedQueueSnapshot());
            updateTableFromCustomList(terminatedTable, scheduler.getTerminatedQueueSnapshot());
            
            if (osRunning) {
                // Usar un bucle for estándar para CustomList
                for (int i = 0; i < activeMetricsWindows.size(); i++) {
                    MetricsDisplayGUI metricsWindow = activeMetricsWindows.get(i);
                    if (metricsWindow != null) {
                        metricsWindow.updateDisplay(scheduler);
                    }
                }
            }
            
            // Actualizar métricas
            throughputLabel.setText(String.format("%.2f proc/s", metrics.get("Throughput")));
            cpuUtilizationLabel.setText(String.format("%.1f %%", metrics.get("CPU_Utilization") * 100));
            avgWaitTimeLabel.setText(String.format("%.2f ciclos", metrics.get("Avg_Wait_Time")));
            avgResponseTimeLabel.setText(String.format("%.2f ciclos", metrics.get("Avg_Response_Time")));
        });
    }
    
    private void updateTableFromCustomList(JTable table, CustomList<PCB> processList) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);
        
        // Es seguro iterar 'processList' porque es una copia (snapshot)
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
        
        // Es seguro iterar 'processes' porque es una copia (snapshot)
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
    
    private void log(String message) {
        SwingUtilities.invokeLater(() -> {
            // Usamos el snapshot para el ciclo global
            logArea.append("Ciclo " + scheduler.getGlobalCycleSnapshot() + ": " + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
    
    public void removeMetricsWindow(MetricsDisplayGUI window) {
        activeMetricsWindows.remove(window);
    }
}