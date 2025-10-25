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
 * Con correcciones para no bloquear la interfaz y soporte para excepciones CPU Bound
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
    private long waitingTime;
    private long turnaroundTime;
    private long responseTime;
    private final long creationTime;
    private final int priority;
    private final int memorySize;
    private final Scheduler scheduler;
    
    public PCB(String name, ProcessType type, int totalInstructions, 
               int cyclesToException, int cyclesToCompleteException,
               int priority, int memorySize, Scheduler scheduler) {
        this.id = ID_GENERATOR.getAndIncrement();
        this.name = name;
        this.type = type;
        this.totalInstructions = totalInstructions;
        this.remainingInstructions = totalInstructions;
        this.cyclesToException = cyclesToException;
        this.cyclesToCompleteException = cyclesToCompleteException;
        this.priority = priority;
        this.memorySize = memorySize;
        this.state = ProcessState.NEW;
        this.programCounter = 0;
        this.mar = 0;
        this.creationTime = System.currentTimeMillis();
        this.scheduler = scheduler;
    }
    
    // Getters y Setters
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
    public long getWaitingTime() { return waitingTime; }
    public void setWaitingTime(long waitingTime) { this.waitingTime = waitingTime; }
    public long getTurnaroundTime() { return turnaroundTime; }
    public void setTurnaroundTime(long turnaroundTime) { this.turnaroundTime = turnaroundTime; }
    public long getResponseTime() { return responseTime; }
    public void setResponseTime(long responseTime) { this.responseTime = responseTime; }
    public long getCreationTime() { return creationTime; }
    public int getPriority() { return priority; }
    public int getMemorySize() { return memorySize; }
    
    /**
     * Lógica de ejecución con excepciones para ambos tipos de procesos
     * @return 
     */
    public boolean executeInstruction() {
        if (remainingInstructions <= 0) {
            state = ProcessState.TERMINATED;
            return true;
        }
        
        programCounter++;
        mar = programCounter;
        remainingInstructions--;
        
        // CORRECCIÓN: Ambos tipos de procesos pueden generar excepciones
        if (cyclesToException > 0 && 
            programCounter > 0 && 
            programCounter % cyclesToException == 0 &&
            state == ProcessState.RUNNING) {
            
            System.out.println("Generando excepción para: " + name + " (" + type + ") en PC: " + programCounter);
            generateException();
            return false; // El proceso se bloquea
        }
        
        if (remainingInstructions == 0) {
            state = ProcessState.TERMINATED;
            return true;
        }
        
        return false;
    }
    
    /**
     * Generar excepción sin bloquear la interfaz
     */
    private void generateException() {
        // Cambiar estado a BLOQUEADO
        state = ProcessState.BLOCKED;
        
        // Notificar al scheduler
        if (scheduler != null) {
            this.setState(ProcessState.BLOCKED);
        }
        
        // CORRECCIÓN: Usar SwingWorker para no bloquear la interfaz
        javax.swing.SwingWorker<Void, Void> worker = new javax.swing.SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    System.out.println("Iniciando operación de excepción para: " + name);
                    
                    // Simular tiempo de operación (más corto para testing)
                    int totalTime = cyclesToCompleteException * 100;
                    for (int i = 0; i < cyclesToCompleteException; i++) {
                        Thread.sleep(100); // Pequeños intervalos para mantener responsiva la UI

                        // --- AÑADE ESTA LÍNEA ---
                        setMAR(getMAR() + 1); // Simular acceso a memoria durante E/S
                        // -----------------------

                        System.out.println("Progreso excepción " + name + ": " + (i + 1) + "/" + cyclesToCompleteException);
                    }
                    
                    System.out.println("Operación de excepción completada para: " + name);
                    
                } catch (InterruptedException e) {
                    System.out.println("Operación de excepción interrumpida para: " + name);
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    System.err.println("Error en operación de excepción para " + name + ": " + e.getMessage());
                }
                return null;
            }
            
            @Override
            protected void done() {
                // Desbloquear el proceso cuando termina la excepción
                if (scheduler != null) {
                    scheduler.unblockProcess(PCB.this);
                }
            }
        };
        
        worker.execute();
    }
    
    public void incrementWaitingTime() {
        this.waitingTime++;
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