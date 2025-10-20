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
 * Con manejo de excepciones usando Threads
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
    private final Scheduler scheduler; // Referencia al planificador para notificar excepciones
    
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
    }
    
    // Getters y Setters básicos
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
    
    /**
     * Ejecuta una instrucción del proceso
     * @return true si el proceso ha terminado, false en caso contrario
     */
    public boolean executeInstruction() {
        if (remainingInstructions <= 0) {
            state = ProcessState.TERMINATED;
            return true;
        }
        
        programCounter++;
        mar = programCounter;
        remainingInstructions--;
        
        // Verificar si se genera una excepción de E/S
        if (type == ProcessType.IO_BOUND && programCounter % cyclesToException == 0) {
            generateIOException();
            return false;
        }
        
        if (remainingInstructions == 0) {
            state = ProcessState.TERMINATED;
            return true;
        }
        
        return false;
    }
    
    /**
     * Genera una excepción de E/S usando un Thread separado
     */
    private void generateIOException() {
        state = ProcessState.BLOCKED;
        
        // Notificar al planificador que este proceso se bloqueó
        if (scheduler != null) {
            scheduler.addToBlockedQueue(this);
        }
        
        // Iniciar un hilo para manejar la excepción de E/S
        IOExceptionThread ioThread = new IOExceptionThread(this, cyclesToCompleteException, scheduler);
        ioThread.start();
        
        System.out.println("Excepción de E/S generada para proceso: " + name);
    }
    
    @Override
    public int compareTo(PCB other) {
        // Comparación basada en el tiempo de creación para FCFS
        return Long.compare(this.creationTime, other.creationTime);
    }
    
    @Override
    public String toString() {
        return String.format("PCB{id=%d, name='%s', state=%s, PC=%d, MAR=%d, remaining=%d}", 
                           id, name, state, programCounter, mar, remainingInstructions);
    }
}