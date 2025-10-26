package simulator.utils;


import simulator.core.PCB;
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
 * Hilo para manejar excepciones de E/S 
 */
public class IOExceptionThread extends Thread {
    private final PCB process;
    private final int completionCycles;
    private final Scheduler scheduler;
    private volatile boolean running;
    
    public IOExceptionThread(PCB process, int completionCycles, Scheduler scheduler) {
        this.process = process;
        this.completionCycles = completionCycles;
        this.scheduler = scheduler;
        this.running = true;
        this.setName("IOThread-" + process.getName());
        this.setDaemon(true);
    }

    @Override
    public void run() {
        System.out.println("Hilo de E/S iniciado para proceso: " + process.getName());

        try {
            for (int i = 0; i < completionCycles && running; i++) {
                Thread.sleep(100);

                process.setMAR(process.getMAR() + 1);

                System.out.println("Operación E/S en progreso para " + process.getName()
                        + " (" + (i + 1) + "/" + completionCycles + ")");
            }
           
            if (running) {
                if (scheduler != null) {
                    scheduler.unblockProcess(process);
                    System.out.println("Operación E/S completada para: " + process.getName());
                }
            }
        }

        catch (InterruptedException e) {
            System.out.println("Hilo de E/S interrumpido para: " + process.getName());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("Error en hilo de E/S para " + process.getName() + ": " + e.getMessage());
        } finally {
            System.out.println("Hilo de E/S terminado para: " + process.getName());
        }
    }
    
    public void cancelIO() {
        this.running = false;
        this.interrupt();
    }
}