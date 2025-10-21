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
 * Actualizada para usar CustomList en lugar de ArrayList
 */
public class SimulationGUI extends JFrame {
    private Scheduler scheduler;
    private Timer simulationTimer;
    private int cycleDuration = 1000; // 1 segundo por defecto
    
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
        
        // Panel superior - Información del sistema
        JPanel topPanel = createTopPanel();
        add(topPanel, BorderLayout.NORTH);
        
        // Panel central - Colas de procesos
        JPanel centerPanel = createCenterPanel();
        add(centerPanel, BorderLayout.CENTER);
        
        // Panel inferior - Controles y log
        JPanel bottomPanel = createBottomPanel();
        add(bottomPanel, BorderLayout.SOUTH);
        
        pack();
        setSize(1200, 800);
        setLocationRelativeTo(null);
        
        // Manejar el cierre de la ventana para detener los hilos
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                scheduler.shutdown();
            }
        });
    }
    
    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 4));
        
        currentCycleLabel = new JLabel("Ciclo Global: 0");
        currentProcessLabel = new JLabel("Proceso Actual: Ninguno");
        cpuStateLabel = new JLabel("Estado CPU: Sistema Operativo");
        algorithmLabel = new JLabel("Algoritmo: FCFS");
        
        panel.add(new JLabel("Ciclo Global:"));
        panel.add(currentCycleLabel);
        panel.add(new JLabel("Proceso Actual:"));
        panel.add(currentProcessLabel);
        panel.add(new JLabel("Estado CPU:"));
        panel.add(cpuStateLabel);
        panel.add(new JLabel("Algoritmo:"));
        panel.add(algorithmLabel);
        
        panel.setBorder(BorderFactory.createTitledBorder("Estado del Sistema"));
        return panel;
    }
    
    private JPanel createCenterPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 2));
        
        // Cola de listos
        readyQueueTable = createProcessTable();
        JScrollPane readyScroll = new JScrollPane(readyQueueTable);
        readyScroll.setBorder(BorderFactory.createTitledBorder("Cola de Listos"));
        
        // Cola de bloqueados
        blockedQueueTable = createProcessTable();
        JScrollPane blockedScroll = new JScrollPane(blockedQueueTable);
        blockedScroll.setBorder(BorderFactory.createTitledBorder("Cola de Bloqueados"));
        
        // Cola de suspendidos
        suspendedQueueTable = createProcessTable();
        JScrollPane suspendedScroll = new JScrollPane(suspendedQueueTable);
        suspendedScroll.setBorder(BorderFactory.createTitledBorder("Cola de Suspendidos"));
        
        // Procesos terminados
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
        
        // Panel de controles
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
        
        // Área de log
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
        return new JTable(model);
    }
    
    private void setupEventHandlers() {
        startButton.addActionListener(this::startSimulation);
        stopButton.addActionListener(this::stopSimulation);
        addProcessButton.addActionListener(this::addProcessDialog);
        algorithmComboBox.addActionListener(this::changeAlgorithm);
        cycleDurationSpinner.addChangeListener(e -> updateCycleDuration());
        
        // Timer para la simulación
        simulationTimer = new Timer(cycleDuration, e -> executeSimulationCycle());
        
        // Inicializar botones
        stopButton.setEnabled(false);
    }
    
    private void startSimulation(ActionEvent e) {
        simulationTimer.start();
        startButton.setEnabled(false);
        stopButton.setEnabled(true);
        log("Simulación iniciada");
    }
    
    private void stopSimulation(ActionEvent e) {
        simulationTimer.stop();
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        log("Simulación detenida");
    }
    
    private void addProcessDialog(ActionEvent e) {
        JTextField nameField = new JTextField("Proceso_" + System.currentTimeMillis());
        JComboBox<ProcessType> typeCombo = new JComboBox<>(ProcessType.values());
        JSpinner instructionsSpinner = new JSpinner(new SpinnerNumberModel(100, 10, 1000, 10));
        JSpinner exceptionSpinner = new JSpinner(new SpinnerNumberModel(5, 1, 50, 1));
        JSpinner completionSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 20, 1));
        
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
            log("Nuevo proceso creado: " + name);
        }
    }
    
    private void changeAlgorithm(ActionEvent e) {
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
    
    private void executeSimulationCycle() {
        scheduler.executeCycle();
        updateGUI();
    }
    
    private void updateGUI() {
        // Actualizar información del sistema
        currentCycleLabel.setText("Ciclo Global: " + scheduler.getGlobalCycle());
        
        PCB currentProcess = scheduler.getCurrentProcess();
        if (currentProcess != null) {
            currentProcessLabel.setText("Proceso Actual: " + currentProcess.getName() + 
                                      " (PC: " + currentProcess.getProgramCounter() + ")");
        } else {
            currentProcessLabel.setText("Proceso Actual: Ninguno");
        }
        
        cpuStateLabel.setText("Estado CPU: " + 
                            (scheduler.isOperatingSystemRunning() ? "Sistema Operativo" : "Programa Usuario"));
        
        // Actualizar tablas usando CustomList
        updateTable(readyQueueTable, scheduler.getReadyQueue().toArray());
        updateTableFromCustomList(blockedQueueTable, scheduler.getBlockedQueue());
        updateTableFromCustomList(suspendedQueueTable, scheduler.getSuspendedQueue());
        updateTableFromCustomList(terminatedTable, scheduler.getTerminatedProcesses());
        
        // Actualizar métricas periódicamente
        if (scheduler.getGlobalCycle() % 10 == 0) {
            log("Métricas - Throughput: " + String.format("%.2f", scheduler.getThroughput()) +
                ", Utilización CPU: " + String.format("%.2f", scheduler.getCpuUtilization() * 100) + "%" +
                ", Tiempo Espera Promedio: " + String.format("%.2f", scheduler.getAverageWaitTime()) +
                ", Tiempo Respuesta Promedio: " + String.format("%.2f", scheduler.getAverageResponseTime()));
        }
    }
    
    /**
     * Actualiza tabla desde un CustomList
     */
    private void updateTableFromCustomList(JTable table, CustomList<PCB> processList) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);
        
        for (int i = 0; i < processList.size(); i++) {
            PCB p = processList.get(i);
            model.addRow(new Object[]{
                p.getId(),
                p.getName(),
                p.getState(),
                p.getProgramCounter(),
                p.getMAR(),
                p.getRemainingInstructions(),
                p.getType()
            });
        }
    }
    
    /**
     * Actualiza tabla desde un array
     */
    private void updateTable(JTable table, Object[] processes) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);
        
        for (Object obj : processes) {
            if (obj instanceof PCB) {
                PCB p = (PCB) obj;
                model.addRow(new Object[]{
                    p.getId(),
                    p.getName(),
                    p.getState(),
                    p.getProgramCounter(),
                    p.getMAR(),
                    p.getRemainingInstructions(),
                    p.getType()
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
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new SimulationGUI().setVisible(true);
        });
    }
}
