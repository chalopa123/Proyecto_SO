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

/**
 * Interfaz gráfica del simulador de planificación de procesos
 * Con correcciones de diseño y parámetros no utilizados
 */
public class SimulationGUI extends JFrame {
    private Scheduler scheduler;
    private Timer simulationTimer;
    private int cycleDuration = 1000;
    
    // Componentes de la interfaz
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
    
    public SimulationGUI() {
        this.scheduler = new Scheduler();
        initializeGUI();
        setupEventHandlers();
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
        // CORRECCIÓN: Usar lambdas para evitar parámetros no utilizados
        startButton.addActionListener(e -> startSimulation());
        stopButton.addActionListener(e -> stopSimulation());
        addProcessButton.addActionListener(e -> addProcessDialog());
        algorithmComboBox.addActionListener(e -> changeAlgorithm());
        cycleDurationSpinner.addChangeListener(e -> updateCycleDuration());
        
        simulationTimer = new Timer(cycleDuration, e -> executeSimulationCycle());
        stopButton.setEnabled(false);
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
    
    private void stopSimulation() {
        simulationTimer.stop();
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        log("Simulación detenida");
    }
    
    private void changeAlgorithm() {
        SchedulingAlgorithm algorithm = (SchedulingAlgorithm) algorithmComboBox.getSelectedItem();
        scheduler.setCurrentAlgorithm(algorithm);
        algorithmLabel.setText("Algoritmo: " + algorithm);
        log("Algoritmo cambiado a: " + algorithm);
    }
    
    private void updateCycleDuration() {
        cycleDuration = (Integer) cycleDurationSpinner.getValue();
        simulationTimer.setDelay(cycleDuration);
        log("Duración del ciclo actualizada a: " + cycleDuration + "ms");
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
        SwingUtilities.invokeLater(() -> {
            currentCycleLabel.setText(String.valueOf(scheduler.getGlobalCycle()));
            
            PCB currentProcess = scheduler.getCurrentProcess();
            if (currentProcess != null) {
                currentProcessLabel.setText(currentProcess.getName() + " (PC: " + currentProcess.getProgramCounter() + ")");
                cpuStateLabel.setText("Programa Usuario");
            } else {
                currentProcessLabel.setText("Ninguno");
                cpuStateLabel.setText("Sistema Operativo");
            }
            
            // Actualizar todas las tablas
            updateTable(readyQueueTable, scheduler.getReadyQueue().toArray());
            updateTableFromCustomList(blockedQueueTable, scheduler.getBlockedQueue());
            updateTableFromCustomList(suspendedQueueTable, scheduler.getSuspendedQueue());
            updateTableFromCustomList(terminatedTable, scheduler.getTerminatedProcesses());
            
            // Métricas cada 5 ciclos
            if (scheduler.getGlobalCycle() % 5 == 0) {
                log("Métricas - Throughput: " + String.format("%.2f", scheduler.getThroughput()) +
                    ", CPU: " + String.format("%.2f", scheduler.getCpuUtilization() * 100) + "%" +
                    ", Espera: " + String.format("%.2f", scheduler.getAverageWaitTime()));
            }
        });
    }
    
    private void updateTableFromCustomList(JTable table, CustomList<PCB> processList) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);
        
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
            logArea.append("Ciclo " + scheduler.getGlobalCycle() + ": " + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
        System.out.println("Ciclo " + scheduler.getGlobalCycle() + ": " + message);
    }
}