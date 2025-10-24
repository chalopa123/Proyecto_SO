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
    private JTable readyQueueTable;
    private JTable blockedQueueTable;
    private JTable suspendedQueueTable;
    private JTable terminatedTable;
    private JTextArea logArea;
    private JComboBox<SchedulingAlgorithm> algorithmComboBox;
    private JSpinner cycleDurationSpinner;
    private JButton startButton;
    private JButton stopButton;
    private JButton addProcessButton;
    
    // Componentes de métricas (asegúrate de que estén declarados)
    private JLabel throughputLabel;
    private JLabel cpuUtilizationLabel;
    private JLabel avgWaitTimeLabel;
    private JLabel avgResponseTimeLabel;


    public SimulationGUI() {
        this.scheduler = new Scheduler();
        initializeGUI(); // Este método debe inicializar los JLabels de métricas
        setupEventHandlers();
        cycleDurationSpinner.setValue(1000);
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
        cpuStateLabel = new JLabel("Sistema Operativo");
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
        JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
        
        readyQueueTable = createProcessTable();
        JScrollPane readyScroll = new JScrollPane(readyQueueTable);
        readyScroll.setBorder(BorderFactory.createTitledBorder("Cola de Listos"));
        
        blockedQueueTable = createProcessTable();
        JScrollPane blockedScroll = new JScrollPane(blockedQueueTable);
        blockedScroll.setBorder(BorderFactory.createTitledBorder("Cola de Bloqueados"));
        
        suspendedQueueTable = createProcessTable();
        JScrollPane suspendedScroll = new JScrollPane(suspendedQueueTable);
        suspendedScroll.setBorder(BorderFactory.createTitledBorder("Cola de Suspendidos"));
        
        terminatedTable = createProcessTable();
        JScrollPane terminatedScroll = new JScrollPane(terminatedTable);
        terminatedScroll.setBorder(BorderFactory.createTitledBorder("Procesos Terminados"));
        
        panel.add(readyScroll);
        panel.add(blockedScroll);
        panel.add(suspendedScroll);
        panel.add(terminatedScroll);
        
        return panel;
    }
    
    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        JPanel controlPanel = new JPanel(new FlowLayout());
        
        algorithmComboBox = new JComboBox<>(SchedulingAlgorithm.values());
        controlPanel.add(new JLabel("Algoritmo:"));
        controlPanel.add(algorithmComboBox);
        
        cycleDurationSpinner = new JSpinner(new SpinnerNumberModel(1000, 100, 5000, 100));
        controlPanel.add(new JLabel("Duración Ciclo (ms):"));
        controlPanel.add(cycleDurationSpinner);
        
        startButton = new JButton("Iniciar");
        stopButton = new JButton("Detener");
        addProcessButton = new JButton("Agregar Proceso");
        
        controlPanel.add(startButton);
        controlPanel.add(stopButton);
        controlPanel.add(addProcessButton);
        
        logArea = new JTextArea(10, 80);
        logArea.setEditable(false);
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Log de Eventos"));
        
        panel.add(controlPanel, BorderLayout.NORTH);
        panel.add(logScroll, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JTable createProcessTable() {
        String[] columnNames = {"ID", "Nombre", "Estado", "PC", "MAR", "Instrucciones Restantes", "Tipo"};
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
        simulationTimer = new Timer(cycleDuration, (ActionEvent e) -> {
            // 1. Ejecutar el ciclo del planificador
            scheduler.executeCycle();
            
            // 2. Actualizar la GUI (de forma segura)
            updateGUI();
        });
        // CORRECCIÓN: Usar lambdas para evitar parámetros no utilizados
        int guiRefreshRate = 33; // Refrescar la GUI ~30 veces por segundo (33ms)
        simulationTimer = new Timer(guiRefreshRate, (ActionEvent e) -> {
            // ¡YA NO LLAMA A executeCycle()!
            updateGUI(); 
        });
        startButton.addActionListener((e) -> {
            scheduler.start(); // <-- AÑADIR ESTA LÍNEA
            simulationTimer.start();
            log("Simulación iniciada.");
        });
        stopButton.addActionListener((e) -> {
            simulationTimer.stop();
            scheduler.shutdown();
            log("Simulación detenida por el usuario.");
        });
        addProcessButton.addActionListener(e -> addProcessDialog());
        algorithmComboBox.addActionListener((e) -> {
            SchedulingAlgorithm selected = (SchedulingAlgorithm) algorithmComboBox.getSelectedItem();
            if (selected != null) {
            // La llamada correcta es al scheduler
                scheduler.setSchedulingAlgorithm(selected); 
                log("Algoritmo cambiado a: " + selected);
            }
        });
        cycleDurationSpinner.addChangeListener((e) -> {
            int newDuration = (Integer) cycleDurationSpinner.getValue();
            if (newDuration > 0) {
                // YA NO CAMBIA EL simulationTimer.setDelay()
                scheduler.setCycleDuration(newDuration); // Envía el valor al scheduler
                log("Duración del ciclo cambiada a: " + newDuration + " ms");
            }
        });
        
        simulationTimer = new Timer(cycleDuration, e -> executeSimulationCycle());
        stopButton.setEnabled(false);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                simulationTimer.stop();
                scheduler.shutdown();
                System.exit(0);
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
        JTextField nameField = new JTextField("Process_" + System.currentTimeMillis());
        JComboBox<ProcessType> typeCombo = new JComboBox<>(ProcessType.values());

        // VALORES MEJORADOS PARA TESTING:
        JSpinner instructionsSpinner = new JSpinner(new SpinnerNumberModel(15, 5, 50, 5));
        JSpinner exceptionSpinner = new JSpinner(new SpinnerNumberModel(4, 2, 10, 1));
        JSpinner completionSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 5, 1));

        JPanel panel = new JPanel(new GridLayout(5, 2));
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

        int result = JOptionPane.showConfirmDialog(this, panel, "Agregar Proceso", 
                                                  JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            String name = nameField.getText();
            ProcessType type = (ProcessType) typeCombo.getSelectedItem();
            int instructions = (Integer) instructionsSpinner.getValue();
            int exceptionCycles = (Integer) exceptionSpinner.getValue();
            int completionCycles = (Integer) completionSpinner.getValue();

            PCB process = new PCB(name, type, instructions, exceptionCycles, completionCycles, scheduler);
            scheduler.addProcess(process);
            log("Nuevo proceso creado: " + name + " (" + type + ", " + instructions + " instrucciones)");
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
            cpuStateLabel.setText(cpuIdle ? "Sistema Operativo" : "Proceso");
            algorithmLabel.setText(alg.toString());
            
            // Actualizar tablas usando los snapshots (que vienen del caché)
            updateTable(readyQueueTable, scheduler.getReadyQueueSnapshot());
            updateTableFromCustomList(blockedQueueTable, scheduler.getBlockedQueueSnapshot());
            updateTableFromCustomList(suspendedQueueTable, scheduler.getSuspendedQueueSnapshot());
            updateTableFromCustomList(terminatedTable, scheduler.getTerminatedQueueSnapshot());
            
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
                p.getMAR(), p.getRemainingInstructions(), p.getType()
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
                    p.getMAR(), p.getRemainingInstructions(), p.getType()
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
}