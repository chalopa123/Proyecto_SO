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
 */
public class PCB implements Comparable<PCB> {
    private static final AtomicInteger ID_GENERATOR = new AtomicInteger(1);
    
    private final int id;
    private String name;
    private ProcessState state;
    private ProcessType type;
    private int totalInstructions;
    private int programCounter;
    private int mar;
    private int cyclesToException;
    private int cyclesToCompleteException;
    private int cyclesInBlocked;
    private int remainingInstructions;
    private int waitingTime;
    private int turnaroundTime;
    private int responseTime;
    private final long creationTime;
    
    public PCB(String name, ProcessType type, int totalInstructions, 
               int cyclesToException, int cyclesToCompleteException) {
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
        this.cyclesInBlocked = 0;
        this.creationTime = System.currentTimeMillis();
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
