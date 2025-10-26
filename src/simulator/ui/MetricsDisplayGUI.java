package simulator.ui;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author chalo
 */


import simulator.structures.CustomList;
import simulator.core.PCB;
import simulator.core.Scheduler;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class MetricsDisplayGUI extends JFrame {

    private String windowType;
    private JTabbedPane tabbedPane;
    private SimulationGUI mainGUI;

    private CpuUsageGraphPanel cpuGraphPanel; 
    private JTable detailedReadyTable;        
    private DefaultTableModel readyTableModel;  
    private TerminatedGraphPanel termGraphPanel;

    public MetricsDisplayGUI(String title, String type, SimulationGUI mainGUI) {
        super(title);
        this.windowType = type;
        this.mainGUI = mainGUI;

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(mainGUI);
        
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                mainGUI.removeMetricsWindow(MetricsDisplayGUI.this);
                dispose(); 
            }
        });

        initComponents(); 
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        tabbedPane = new JTabbedPane();
        add(tabbedPane, BorderLayout.CENTER);

        if ("Gráficos".equals(windowType)) {
            cpuGraphPanel = new CpuUsageGraphPanel(); 
            tabbedPane.addTab("Uso CPU", cpuGraphPanel);
 
            termGraphPanel = new TerminatedGraphPanel(); 
            tabbedPane.addTab("Procesos Terminados", termGraphPanel);
            
        } else if ("Colas Extendidas".equals(windowType)) {

            String[] columnNames = {"ID", "Nombre", "Estado", "PC", "Prioridad", "Memoria", "Inst. Restantes"};
            
            readyTableModel = new DefaultTableModel(columnNames, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false; 
                }
            };
            detailedReadyTable = new JTable(readyTableModel);
            
            JScrollPane scrollPane = new JScrollPane(detailedReadyTable);
            tabbedPane.addTab("Cola Listos (Detalle)", scrollPane);
        }
    }

    /**
     * actualizar los componentes.
     */
    public void updateDisplay(Scheduler scheduler) {
        if ("Gráficos".equals(windowType)) {
            updateCpuUsageGraph(scheduler.getCpuUsageHistory());
        
        } else if ("Colas Extendidas".equals(windowType)) {
            updateDetailedReadyQueueTable(scheduler.getReadyQueueSnapshot());
            updateTerminatedGraph(scheduler.getTerminatedHistory());
        }
    }

    private void updateCpuUsageGraph(CustomList<Integer> data) {
        if (cpuGraphPanel != null) {
            cpuGraphPanel.updateData(data); 
        }
    }
    
    private void updateTerminatedGraph(CustomList<Integer> data) {
        if (termGraphPanel != null) {
            termGraphPanel.updateData(data); 
        }
    }

    private void updateDetailedReadyQueueTable(Object[] data) {
        if (readyTableModel != null) {
            readyTableModel.setRowCount(0); 
            
            for (Object obj : data) {
                if (obj instanceof PCB) {
                    PCB p = (PCB) obj;
                    readyTableModel.addRow(new Object[]{
                        p.getId(),
                        p.getName(),
                        p.getState(),
                        p.getProgramCounter(),
                        p.getPriority(),
                        p.getMemorySize() + " MB",
                        p.getRemainingInstructions()
                    });
                }
            }
        }
    }
} 