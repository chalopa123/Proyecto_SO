package simulator.utils;


import simulator.core.Scheduler;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author chalo
 */

/**
 * Hilo para manejar excepciones del sistema 
 */
public class ExceptionHandlerThread extends Thread {
    private final Scheduler scheduler;
    private volatile boolean running;
    
    public ExceptionHandlerThread(Scheduler scheduler) {
        this.scheduler = scheduler;
        this.running = true;
        this.setName("ExceptionHandlerThread");
        this.setDaemon(true); 
    }
    
    @Override
    public void run() {
        System.out.println("Hilo de manejo de excepciones iniciado");
        
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                monitorBlockedProcesses();

                Thread.sleep(100); 
            
            } catch (InterruptedException e) {
                System.out.println("Hilo de excepciones interrumpido");
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("Error en hilo de excepciones: " + e.getMessage());
            }
        }
        
        System.out.println("Hilo de manejo de excepciones terminado");
    }
    
    private void monitorBlockedProcesses() {
        if (scheduler != null) {
            int blockedCount = scheduler.getBlockedQueueSnapshot().size();
            
            if (blockedCount > 5) {
                System.out.println("Advertencia: " + blockedCount + " procesos en cola de bloqueados");
            }
        }
    }
    
    public void stopHandler() {
        this.running = false;
        this.interrupt();
    }
}