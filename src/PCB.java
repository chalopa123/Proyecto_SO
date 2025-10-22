/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author Chalopa
 */

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Clase que representa el Bloque de Control de Proceso (PCB)
 * Con correcciones para evitar bloqueos en excepciones I/O
 */
public class PCB implements Comparable<PCB> {
    private static final AtomicInteger ID_GENERATOR = new AtomicInteger(1);
    
    private final int id;
    private final String name;
    private ProcessState state;
    private final ProcessType type;
    private final int totalInstructions;
    private int programCounter;
    private int mar;
    private final int cyclesToException;
    private final int cyclesToCompleteException;
    private int remainingInstructions;
    private int waitingTime;
    private int turnaroundTime;
    private int responseTime;
    private final long creationTime;
    private final Scheduler scheduler;
    
    // Bandera para evitar múltiples excepciones simultáneas
    private boolean exceptionInProgress;
    
    public PCB(String name, ProcessType type, int totalInstructions, 
               int cyclesToException, int cyclesToCompleteException, Scheduler scheduler) {
        this.id = ID_GENERATOR.getAndIncrement();
        this.name = name;
        this.type = type;
        this.totalInstructions = totalInstructions;
        this.remainingInstructions = totalInstructions;
        this.cyclesToException = cyclesToException;
        this.cyclesToCompleteException = cyclesToCompleteException;
        this.state = ProcessState.NEW;
        this.programCounter = 0;
        this.mar = 0;
        this.creationTime = System.currentTimeMillis();
        this.scheduler = scheduler;
        this.exceptionInProgress = false;
    }
    
    // Getters y Setters...
    public int getId() { return id; }
    public String getName() { return name; }
    public ProcessState getState() { return state; }
    public void setState(ProcessState state) { this.state = state; }
    public ProcessType getType() { return type; }
    public int getTotalInstructions() { return totalInstructions; }
    public int getProgramCounter() { return programCounter; }
    public void setProgramCounter(int programCounter) { this.programCounter = programCounter; }
    public int getMAR() { return mar; }
    public void setMAR(int mar) { this.mar = mar; }
    public int getCyclesToException() { return cyclesToException; }
    public int getCyclesToCompleteException() { return cyclesToCompleteException; }
    public int getRemainingInstructions() { return remainingInstructions; }
    public void setRemainingInstructions(int remainingInstructions) { 
        this.remainingInstructions = remainingInstructions; 
    }
    public int getWaitingTime() { return waitingTime; }
    public void setWaitingTime(int waitingTime) { this.waitingTime = waitingTime; }
    public int getTurnaroundTime() { return turnaroundTime; }
    public void setTurnaroundTime(int turnaroundTime) { this.turnaroundTime = turnaroundTime; }
    public int getResponseTime() { return responseTime; }
    public void setResponseTime(int responseTime) { this.responseTime = responseTime; }
    public long getCreationTime() { return creationTime; }
    public boolean isExceptionInProgress() { return exceptionInProgress; }
    public void setExceptionInProgress(boolean exceptionInProgress) { 
        this.exceptionInProgress = exceptionInProgress; 
    }
    
    /**
     * CORRECCIÓN CRÍTICA: Lógica mejorada para evitar bloqueos
     */
    public boolean executeInstruction() {
        if (remainingInstructions <= 0) {
            state = ProcessState.TERMINATED;
            return true;
        }
        
        programCounter++;
        mar = programCounter;
        remainingInstructions--;
        
        // CORRECCIÓN: Solo generar excepción si no hay una en progreso y es I/O Bound
        if (type == ProcessType.IO_BOUND && 
            cyclesToException > 0 && 
            programCounter > 0 && 
            programCounter % cyclesToException == 0 &&
            !exceptionInProgress &&
            state == ProcessState.RUNNING) {
            
            System.out.println("Generando excepción I/O para: " + name + " en PC: " + programCounter);
            generateIOException();
            return false; // El proceso se bloquea
        }
        
        if (remainingInstructions == 0) {
            state = ProcessState.TERMINATED;
            return true;
        }
        
        return false;
    }
    
    /**
     * CORRECCIÓN: Generar excepción de E/S de forma segura
     */
    private void generateIOException() {
        // Marcar que hay una excepción en progreso
        exceptionInProgress = true;
        state = ProcessState.BLOCKED;
        
        if (scheduler != null) {
            scheduler.addToBlockedQueue(this);
        }
        
        // Iniciar hilo para manejar la excepción de E/S
        Thread ioThread = new Thread(() -> {
            try {
                System.out.println("Hilo E/S iniciado para: " + name);
                
                // Simular el tiempo de la operación I/O
                for (int i = 0; i < cyclesToCompleteException; i++) {
                    Thread.sleep(300); // Más rápido para testing
                    System.out.println("E/S progreso " + name + ": " + (i+1) + "/" + cyclesToCompleteException);
                }
                
                // CORRECCIÓN: Usar invokeLater para actualizaciones GUI
                javax.swing.SwingUtilities.invokeLater(() -> {
                    try {
                        // Desbloquear el proceso
                        exceptionInProgress = false;
                        if (scheduler != null) {
                            scheduler.unblockProcess(PCB.this);
                            System.out.println("Operación E/S completada para: " + name);
                        }
                    } catch (Exception e) {
                        System.err.println("Error al desbloquear proceso: " + e.getMessage());
                    }
                });
                
            } catch (InterruptedException e) {
                System.out.println("Hilo E/S interrumpido para: " + name);
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                System.err.println("Error en hilo E/S para " + name + ": " + e.getMessage());
            }
        });
        
        ioThread.setDaemon(true);
        ioThread.setName("IO-Thread-" + name);
        ioThread.start();
        
        System.out.println("Excepción de E/S generada para proceso: " + name);
    }
    
    @Override
    public int compareTo(PCB other) {
        return Long.compare(this.creationTime, other.creationTime);
    }
    
    @Override
    public String toString() {
        return String.format("PCB{id=%d, name='%s', state=%s, PC=%d, MAR=%d, remaining=%d}", 
                           id, name, state, programCounter, mar, remainingInstructions);
    }
}