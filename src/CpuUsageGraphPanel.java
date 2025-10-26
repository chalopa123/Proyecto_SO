/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author chalo
 */

import javax.swing.JPanel;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Font;
import java.awt.RenderingHints;

/**
 * Un panel personalizado que dibuja un gráfico del historial de uso de CPU.
 * Cumple con el requisito de no usar librerías de gráficos externas.
 */
public class CpuUsageGraphPanel extends JPanel {

    private CustomList<Integer> cpuHistory;
    private double currentUtilization = 0.0;
    private int totalBusyCycles = 0;
    private int totalCycles = 0;

    public CpuUsageGraphPanel() {
        this.cpuHistory = new CustomList<>();
        setBackground(Color.DARK_GRAY);
    }

    /**
     * Recibe los nuevos datos desde el Scheduler (vía MetricsDisplayGUI)
     * y solicita que el panel se redibuje.
     */
    public void updateData(CustomList<Integer> data) {
        this.cpuHistory = data;

        // Calcular métricas para mostrar
        totalBusyCycles = 0;
        totalCycles = cpuHistory.size();
        
        if (totalCycles > 0) {
            for (int i = 0; i < totalCycles; i++) {
                if (cpuHistory.get(i) == 1) {
                    totalBusyCycles++;
                }
            }
            this.currentUtilization = (double) totalBusyCycles / totalCycles;
        } else {
            this.currentUtilization = 0.0;
        }

        // Importante: Llama a repaint() para forzar la actualización del dibujo
        this.repaint();
    }

    /**
     * Sobrescribimos este método para dibujar nuestro gráfico.
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        // 1. Dibujar fondo
        g2d.setColor(Color.DARK_GRAY);
        g2d.fillRect(0, 0, width, height);

        // 2. Dibujar texto de métricas
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Monospaced", Font.BOLD, 18));
        g2d.drawString(String.format("Utilización Total de CPU: %.2f %%", currentUtilization * 100), 20, 30);
        g2d.setFont(new Font("Monospaced", Font.PLAIN, 14));
        g2d.drawString(String.format("Ciclos Ocupados: %d", totalBusyCycles), 20, 55);
        g2d.drawString(String.format("Ciclos Totales: %d", totalCycles), 20, 75);

        // 3. Dibujar el gráfico (un "sparkline" simple)
        if (totalCycles > 0) {
            int graphX = 20;
            int graphY = 100;
            int graphWidth = width - 40;
            int graphHeight = height - 120;

            // Dibujar borde del gráfico
            g2d.setColor(Color.GRAY);
            g2d.drawRect(graphX, graphY, graphWidth, graphHeight);

            // Dibujar las barras
            // (Esto dibuja cada ciclo como una línea de 1px de ancho)
            for (int i = 0; i < totalCycles; i++) {
                // Mapear el índice 'i' a una posición 'x' en el panel
                int x = graphX + (int) ((double) i / totalCycles * graphWidth);
                
                if (cpuHistory.get(i) == 1) {
                    // Ocupado (Verde)
                    g2d.setColor(Color.GREEN);
                    g2d.drawLine(x, graphY + graphHeight, x, graphY + (graphHeight / 2));
                } else {
                    // Ocioso (Rojo)
                    g2d.setColor(Color.RED);
                    g2d.drawLine(x, graphY + graphHeight, x, graphY + graphHeight - 10);
                }
            }
        }
    }
}
