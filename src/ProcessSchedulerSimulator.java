/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */

/**
 *
 * @author Chalopa
 */
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 * Clase principal del simulador de planificación de procesos
 * Punto de entrada de la aplicación
 */
public class ProcessSchedulerSimulator {
    
    public static void main(String[] args) {
        // Configura el Look and Feel del sistema
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | UnsupportedLookAndFeelException e) {
            System.err.println("Error al configurar el Look and Feel: " + e.getMessage());
        }
        
        // Inicia la interfaz gráfica
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                createAndShowGUI();
            }
        });
    }
    
    private static void createAndShowGUI() {
        // Crea y muestra la ventana principal
        SimulationGUI gui = new SimulationGUI();
        gui.setVisible(true);
        
        // Mensaje de bienvenida
        System.out.println("=========================================");
        System.out.println("Simulador de Planificación de Procesos");
        System.out.println("Sistemas Operativos - Proyecto Universitario");
        System.out.println("Desarrollado en Java con Montículos (Heap)");
        System.out.println("=========================================");
    }
}