/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author chalo
 */

// Archivo: CpuUsageGraphPanel.java
// (Sin 'package' al inicio)

import javax.swing.JPanel;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Font;
import java.awt.RenderingHints;
import java.awt.BasicStroke;

/**
 * Dibuja un gráfico de LÍNEA (como el de la imagen) del uso de CPU.
 * Agrupa los ciclos en "baldes" (buckets) para crear un gráfico más suave.
 */
public class CpuUsageGraphPanel extends JPanel {

    private CustomList<Integer> cpuHistory;
    private double currentUtilization = 0.0;
    private int totalBusyCycles = 0;
    private int totalCycles = 0;
    
    // Almacena los puntos del gráfico (X, Y)
    private CustomList<Point> graphPoints; 
    
    // (Clase interna simple para un punto)
    private static class Point {
        int x, y;
        Point(int x, int y) { this.x = x; this.y = y; }
    }

    public CpuUsageGraphPanel() {
        this.cpuHistory = new CustomList<>();
        this.graphPoints = new CustomList<>();
        setBackground(Color.DARK_GRAY);
    }

    public void updateData(CustomList<Integer> data) {
        this.cpuHistory = data;
        totalCycles = cpuHistory.size();
        
        if (totalCycles > 0) {
            totalBusyCycles = 0;
            for (int i = 0; i < totalCycles; i++) {
                if (cpuHistory.get(i) == 1) {
                    totalBusyCycles++;
                }
            }
            this.currentUtilization = (double) totalBusyCycles / totalCycles;
        } else {
            this.currentUtilization = 0.0;
        }
        
        // Recalcular los puntos del gráfico
        calculateGraphPoints();
        
        this.repaint();
    }

    /**
     * Esta es la lógica clave para agrupar ciclos y crear un gráfico de línea.
     */
    private void calculateGraphPoints() {
        graphPoints.clear();
        if (totalCycles == 0) return;

        int width = getWidth();
        int height = getHeight();
        int p = 60; // Padding
        int graphWidth = width - p*2;
        int graphHeight = height - p*2;
        if (graphWidth <= 0 || graphHeight <= 0) return;

        // Definimos cuántos puntos queremos en el gráfico (ej. uno por cada 5 píxeles)
        int numBuckets = Math.min(totalCycles, graphWidth / 5);
        if (numBuckets == 0) numBuckets = 1;
        
        int bucketSize = totalCycles / numBuckets;
        if (bucketSize == 0) bucketSize = 1;

        for (int i = 0; i < numBuckets; i++) {
            int start = i * bucketSize;
            int end = Math.min((i + 1) * bucketSize, totalCycles);
            if (i == numBuckets - 1) end = totalCycles; // Asegurar que el último bucket llegue al final

            int busyInBucket = 0;
            int totalInBucket = end - start;
            
            for (int j = start; j < end; j++) {
                if (cpuHistory.get(j) == 1) {
                    busyInBucket++;
                }
            }
            
            double bucketUtil = (totalInBucket > 0) ? (double) busyInBucket / totalInBucket : 0.0;
            
            // Mapear al gráfico
            int x = p + (int) (i * (double) graphWidth / (numBuckets - 1));
            int y = (height - p) - (int) (bucketUtil * graphHeight);
            
            graphPoints.add(new Point(x, y));
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        // 1. Fondo
        g2d.setColor(Color.DARK_GRAY);
        g2d.fillRect(0, 0, width, height);

        // 2. Texto
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Monospaced", Font.BOLD, 18));
        g2d.drawString(String.format("Utilización Total de CPU: %.2f %%", currentUtilization * 100), 20, 30);
        g2d.setFont(new Font("Monospaced", Font.PLAIN, 14));
        g2d.drawString(String.format("Ciclos Ocupados: %d", totalBusyCycles), 20, 55);
        g2d.drawString(String.format("Ciclos Totales: %d", totalCycles), 20, 75);

        // 3. Ejes
        int p = 60; // Padding
        g2d.setColor(Color.GRAY);
        g2d.drawLine(p, p, p, height - p); // Y-axis
        g2d.drawLine(p, height - p, width - p, height - p); // X-axis
        // Etiquetas de ejes
        g2d.setFont(new Font("Monospaced", Font.PLAIN, 12));
        g2d.drawString("100%", p - 40, p + 5);
        g2d.drawString("0%", p - 30, height - p + 5);
        g2d.drawString("Tiempo (Ciclos)", (width / 2) - 40, height - (p/2) + 10);


        // 4. Dibujar la LÍNEA (estilo imagen)
        if (graphPoints.size() > 1) {
            g2d.setColor(new Color(60, 180, 255)); // Azul claro
            g2d.setStroke(new BasicStroke(3)); // Línea gruesa

            for (int i = 0; i < graphPoints.size() - 1; i++) {
                Point p1 = graphPoints.get(i);
                Point p2 = graphPoints.get(i + 1);
                g2d.drawLine(p1.x, p1.y, p2.x, p2.y);
            }
            
            // Dibujar el último punto (círculo)
            Point lastPoint = graphPoints.get(graphPoints.size() - 1);
            g2d.fillOval(lastPoint.x - 5, lastPoint.y - 5, 10, 10);
        }
    }
}