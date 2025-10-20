/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author chalo
 */

import java.util.concurrent.Semaphore;

/**
 * Planificador principal que gestiona los procesos usando montículos
 * Usa CustomList en lugar de ArrayList para cumplir con los requerimientos
 */
public class Scheduler {
    private ProcessHeap readyQueue;
    private final CustomList<PCB> blockedQueue;
    private final CustomList<PCB> suspendedQueue;
    private final CustomList<PCB> terminatedProcesses;
    private PCB currentProcess;
    private SchedulingAlgorithm currentAlgorithm;
    private int timeQuantum;
    private int currentQuantum;
    private long globalCycle;
    private boolean isOperatingSystemRunning;
    private final Semaphore mutex;
    
    // Hilo para manejo de excepciones de E/S
    private final ExceptionHandlerThread exceptionHandler;
    
    // Métricas de rendimiento
    private int completedProcesses;
    private long totalCpuBusyTime;
    private long totalWaitTime;
    private long totalResponseTime;
    private final long startTime;
    
    public Scheduler() {
        this.readyQueue = new ProcessHeap(100, SchedulingAlgorithm.FCFS);
        this.blockedQueue = new CustomList<>();
        this.suspendedQueue = new CustomList<>();
        this.terminatedProcesses = new CustomList<>();
        this.currentAlgorithm = SchedulingAlgorithm.FCFS;
        this.timeQuantum = 4; // Quantum por defecto para RR
        this.currentQuantum = 0;
        this.globalCycle = 0;
        this.isOperatingSystemRunning = true;
        this.mutex = new Semaphore(1);
        
        // Inicializar manejador de excepciones
        this.exceptionHandler = new ExceptionHandlerThread(this);
        
        this.completedProcesses = 0;
        this.totalCpuBusyTime = 0;
        this.startTime = System.currentTimeMillis();
        
        // Iniciar hilo de excepciones después de la construcción completa
        startExceptionHandler();
    }
    
    /**
     * Inicia el hilo de manejo de excepciones después de la construcción
     */
    private void startExceptionHandler() {
        this.exceptionHandler.start();
    }
    
    /**
     * Agrega un nuevo proceso al sistema
     * @param process el proceso a agregar
     */
    public void addProcess(PCB process) {
        try {
            mutex.acquire();
            process.setState(ProcessState.READY);
            readyQueue.insert(process);
            System.out.println("Proceso agregado: " + process.getName());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            mutex.release();
        }
    }
    
    /**
     * Método para que el hilo de excepciones notifique cuando un proceso se desbloquea
     * @param process el proceso a desbloquear
     */
    public void unblockProcess(PCB process) {
        try {
            mutex.acquire();
            if (blockedQueue.remove(process)) {
                process.setState(ProcessState.READY);
                readyQueue.insert(process);
                System.out.println("Proceso desbloqueado: " + process.getName());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            mutex.release();
        }
    }
    
    /**
     * Agrega un proceso a la cola de bloqueados (llamado por PCB cuando ocurre excepción)
     * @param process el proceso a bloquear
     */
    public void addToBlockedQueue(PCB process) {
        try {
            mutex.acquire();
            blockedQueue.add(process);
            System.out.println("Proceso bloqueado: " + process.getName());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            mutex.release();
        }
    }
    
    /**
     * Actualiza el estado de los procesos suspendidos
     */
    private void updateSuspendedProcesses() {
        // Implementación simplificada de gestión de procesos suspendidos
        if (readyQueue.size() > 10) { // Umbral arbitrario para suspensión
            PCB processToSuspend = readyQueue.extract();
            if (processToSuspend != null) {
                processToSuspend.setState(ProcessState.SUSPENDED);
                suspendedQueue.add(processToSuspend);
                System.out.println("Proceso suspendido: " + processToSuspend.getName());
            }
        }
        
        // Reactivar procesos suspendidos cuando hay espacio
        if (readyQueue.size() < 5 && !suspendedQueue.isEmpty()) {
            PCB processToResume = suspendedQueue.removeAt(0);
            processToResume.setState(ProcessState.READY);
            readyQueue.insert(processToResume);
            System.out.println("Proceso reanudado: " + processToResume.getName());
        }
    }
    
    /**
     * Actualiza las métricas de rendimiento
     */
    private void updateMetrics() {
        if (currentProcess != null) {
            totalCpuBusyTime++;
        }
        
        // Actualizar tiempos de espera para procesos en cola de listos
        PCB[] readyProcesses = readyQueue.toArray();
        for (PCB process : readyProcesses) {
            if (process != null) {
                process.setWaitingTime(process.getWaitingTime() + 1);
            }
        }
        
        // Actualizar tiempos de espera para procesos bloqueados
        for (int i = 0; i < blockedQueue.size(); i++) {
            PCB process = blockedQueue.get(i);
            process.setWaitingTime(process.getWaitingTime() + 1);
        }
    }
    
    // Getters para el estado del planificador
    public PCB getCurrentProcess() { return currentProcess; }
    public ProcessHeap getReadyQueue() { return readyQueue; }
    public CustomList<PCB> getBlockedQueue() { return blockedQueue; }
    public CustomList<PCB> getSuspendedQueue() { return suspendedQueue; }
    public CustomList<PCB> getTerminatedProcesses() { return terminatedProcesses; }
    public long getGlobalCycle() { return globalCycle; }
    public boolean isOperatingSystemRunning() { return isOperatingSystemRunning; }
    public SchedulingAlgorithm getCurrentAlgorithm() { return currentAlgorithm; }
    
    public void setCurrentAlgorithm(SchedulingAlgorithm algorithm) {
        this.currentAlgorithm = algorithm;
        this.readyQueue = new ProcessHeap(100, algorithm);
        
        // Reinsertar procesos en la nueva cola
        PCB[] processes = readyQueue.toArray();
        for (PCB process : processes) {
            if (process != null && process.getState() == ProcessState.READY) {
                this.readyQueue.insert(process);
            }
        }
    }
    
    public void setTimeQuantum(int quantum) {
        this.timeQuantum = quantum;
    }
    
    // Métricas de rendimiento
    public double getThroughput() {
        long elapsedTime = (System.currentTimeMillis() - startTime) / 1000;
        return elapsedTime > 0 ? (double) completedProcesses / elapsedTime : 0;
    }
    
    public double getCpuUtilization() {
        return globalCycle > 0 ? (double) totalCpuBusyTime / globalCycle : 0;
    }
    
    public double getAverageWaitTime() {
        int totalProcesses = completedProcesses + readyQueue.size() + blockedQueue.size();
        return totalProcesses > 0 ? (double) totalWaitTime / totalProcesses : 0;
    }
    
    public double getAverageResponseTime() {
        return completedProcesses > 0 ? (double) totalResponseTime / completedProcesses : 0;
    }
    
    /**
     * Detiene el planificador y sus hilos
     */
    public void shutdown() {
        if (exceptionHandler != null) {
            exceptionHandler.interrupt();
        }
    }
}