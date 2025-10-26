/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author chalo
 */

// Archivo: MetricsDisplayGUI.java
// (Sin 'package' al inicio)

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
// (No debe haber imports de java.util.List o ArrayList)

public class MetricsDisplayGUI extends JFrame {

    private String windowType;
    private JTabbedPane tabbedPane;
    private SimulationGUI mainGUI;
    
    // --- Variables para los componentes dinámicos ---
    private CpuUsageGraphPanel cpuGraphPanel; // Panel para el gráfico de CPU
    private JTable detailedReadyTable;        // Tabla para la cola de listos
    private DefaultTableModel readyTableModel;  // Modelo de la tabla de listos
    private TerminatedGraphPanel termGraphPanel;

    public MetricsDisplayGUI(String title, String type, SimulationGUI mainGUI) {
        super(title);
        this.windowType = type;
        this.mainGUI = mainGUI;
        // (Ya no necesitamos la lista 'dynamicComponents')

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

        initComponents(); // Este método ahora crea los componentes reales
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        tabbedPane = new JTabbedPane();
        add(tabbedPane, BorderLayout.CENTER);

        if ("Gráficos".equals(windowType)) {
            // --- LÓGICA DE GRÁFICOS IMPLEMENTADA ---
            
            // 1. Pestaña de Uso de CPU
            cpuGraphPanel = new CpuUsageGraphPanel(); // <-- Usamos la nueva clase
            tabbedPane.addTab("Uso CPU", cpuGraphPanel);
            
            // 2. Pestaña de Procesos Terminados (Placeholder)
            // (Puedes implementar esto después si quieres, similar a la tabla de listos)
            termGraphPanel = new TerminatedGraphPanel(); // <-- CREAR EL NUEVO PANEL
            tabbedPane.addTab("Procesos Terminados", termGraphPanel);
            
        } else if ("Colas Extendidas".equals(windowType)) {
            // --- LÓGICA DE TABLA IMPLEMENTADA ---
            
            // 1. Pestaña de Cola de Listos Detallada
            String[] columnNames = {"ID", "Nombre", "Estado", "PC", "Prioridad", "Memoria", "Inst. Restantes"};
            
            readyTableModel = new DefaultTableModel(columnNames, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false; // Hacer la tabla no editable
                }
            };
            detailedReadyTable = new JTable(readyTableModel);
            
            JScrollPane scrollPane = new JScrollPane(detailedReadyTable);
            tabbedPane.addTab("Cola Listos (Detalle)", scrollPane);
        }
    }

    /**
     * Método llamado por la GUI principal para actualizar los componentes.
     */
    public void updateDisplay(Scheduler scheduler) {
        if ("Gráficos".equals(windowType)) {
            // Llama al método del panel de gráfico
            updateCpuUsageGraph(scheduler.getCpuUsageHistory());
        
        } else if ("Colas Extendidas".equals(windowType)) {
            // Llama al método de la tabla de listos
            updateDetailedReadyQueueTable(scheduler.getReadyQueueSnapshot());
            updateTerminatedGraph(scheduler.getTerminatedHistory());
        }
    }

    /**
     * LÓGICA IMPLEMENTADA:
     * Actualiza el panel del gráfico de CPU.
     */
    private void updateCpuUsageGraph(CustomList<Integer> data) {
        if (cpuGraphPanel != null) {
            cpuGraphPanel.updateData(data); // Pasa los datos al panel para que se redibuje
        }
    }
    
    private void updateTerminatedGraph(CustomList<Integer> data) {
        if (termGraphPanel != null) {
            termGraphPanel.updateData(data); // Pasa los datos al panel
        }
    }

    /**
     * LÓGICA IMPLEMENTADA:
     * Actualiza la tabla de la cola de listos detallada.
     */
    private void updateDetailedReadyQueueTable(Object[] data) {
        if (readyTableModel != null) {
            readyTableModel.setRowCount(0); // Limpiar la tabla
            
            for (Object obj : data) {
                if (obj instanceof PCB) {
                    PCB p = (PCB) obj;
                    // Añadir la fila con los datos del PCB
                    // (Usamos los getters de PCB.java)
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
} // Cierre final de la clase MetricsDisplayGUI