/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author chalo
 */

/**
 * Hilo para manejar excepciones del sistema de manera global
 * Monitorea procesos bloqueados y gestiona su reactivación
 */
public class ExceptionHandlerThread extends Thread {
    private final Scheduler scheduler;
    private volatile boolean running;
    
    public ExceptionHandlerThread(Scheduler scheduler) {
        this.scheduler = scheduler;
        this.running = true;
        this.setName("ExceptionHandlerThread");
        this.setDaemon(true); // Es un hilo de fondo
    }
    
    @Override
    public void run() {
        System.out.println("Hilo de manejo de excepciones iniciado");
        
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                // Monitorear procesos bloqueados periódicamente
                monitorBlockedProcesses();
                
                // Dormir por un tiempo antes de la siguiente verificación
                Thread.sleep(100); // Aumentamos el sleep, 10ms es muy agresivo
            
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
    
    /**
     * Monitorea procesos bloqueados y los reactiva cuando sea apropiado.
     * ¡VERSIÓN SEGURA!
     */
    private void monitorBlockedProcesses() {
        if (scheduler != null) {
            
            // --- INICIO DE CAMBIOS ---
            
            // Obtenemos el tamaño de la cola de forma segura usando el snapshot
            int blockedCount = scheduler.getBlockedQueueSnapshot().size();
            
            // --- FIN DE CAMBIOS ---
            
            if (blockedCount > 5) {
                // Este log es seguro, solo es una salida a consola
                System.out.println("Advertencia: " + blockedCount + " procesos en cola de bloqueados");
            }
        }
    }
    
    public void stopHandler() {
        this.running = false;
        this.interrupt();
    }
}