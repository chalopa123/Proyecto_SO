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
import javax.swing.JPanel;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Font;
import java.awt.RenderingHints;

public class TerminatedGraphPanel extends JPanel {

    private CustomList<Integer> terminatedHistory;
    private int totalTerminated = 0;
    private int totalCycles = 0;

    public TerminatedGraphPanel() {
        this.terminatedHistory = new CustomList<>();
        setBackground(Color.DARK_GRAY);
    }

    public void updateData(CustomList<Integer> data) {
        this.terminatedHistory = data;
        this.totalCycles = data.size();
        if (totalCycles > 0) {
            this.totalTerminated = data.get(totalCycles - 1);
        } else {
            this.totalTerminated = 0;
        }
        this.repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        // fondo
        g2d.setColor(Color.DARK_GRAY);
        g2d.fillRect(0, 0, width, height);

        // texto
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Monospaced", Font.BOLD, 18));
        g2d.drawString(String.format("Procesos Terminados: %d", totalTerminated), 20, 30);
        g2d.setFont(new Font("Monospaced", Font.PLAIN, 14));
        g2d.drawString(String.format("Ciclos Totales: %d", totalCycles), 20, 55);
        
        // ejes
        int p = 60; 
        g2d.setColor(Color.GRAY);
        g2d.drawLine(p, p, p, height - p); 
        g2d.drawLine(p, height - p, width - p, height - p); 

        // el gráfico
        if (totalCycles > 0) {
            g2d.setColor(Color.CYAN);

            int yMax = Math.max(5, totalTerminated); 

            int lastX = p;
            int lastY = height - p;

            for (int i = 0; i < totalCycles; i++) {
                int x = p + (int) (i * (double) (width - p*2) / totalCycles);
                int y = (height - p) - (int) (terminatedHistory.get(i) * (double) (height - p*2) / yMax);

                g2d.drawLine(lastX, lastY, x, lastY); 
                g2d.drawLine(x, lastY, x, y);

                lastX = x;
                lastY = y;
            }
            g2d.drawLine(lastX, lastY, width - p, lastY);
        }
    }
}
