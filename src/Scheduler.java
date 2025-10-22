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
 * Planificador principal con mejoras en gestión de estados
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
        this.timeQuantum = 4;
        this.currentQuantum = 0;
        this.globalCycle = 0;
        this.isOperatingSystemRunning = true;
        this.mutex = new Semaphore(1);
        
        this.completedProcesses = 0;
        this.totalCpuBusyTime = 0;
        this.startTime = System.currentTimeMillis();
    }
    
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
     * CORRECCIÓN: Ciclo de ejecución más robusto
     */
    public void executeCycle() {
        try {
            mutex.acquire();
            globalCycle++;
            
            // Estado del sistema operativo
            isOperatingSystemRunning = (currentProcess == null);
            
            // 1. Actualizar procesos suspendidos (menos agresivo)
            updateSuspendedProcesses();
            
            // 2. Seleccionar próximo proceso si es necesario
            if (currentProcess == null || 
                currentProcess.getState() != ProcessState.RUNNING ||
                (currentAlgorithm == SchedulingAlgorithm.RR && currentQuantum >= timeQuantum)) {
                
                scheduleNextProcess();
            }
            
            // 3. Ejecutar proceso actual si hay uno y está RUNNING
            if (currentProcess != null && currentProcess.getState() == ProcessState.RUNNING) {
                executeCurrentProcess();
            }
            
            // 4. Actualizar métricas
            updateMetrics();
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            mutex.release();
        }
    }
    
    private void scheduleNextProcess() {
        // Si hay un proceso actual en RUNNING, devolverlo a ready
        if (currentProcess != null && currentProcess.getState() == ProcessState.RUNNING) {
            currentProcess.setState(ProcessState.READY);
            readyQueue.insert(currentProcess);
            System.out.println("Proceso " + currentProcess.getName() + " vuelve a ready");
        }
        
        currentProcess = readyQueue.extract();
        if (currentProcess != null) {
            currentProcess.setState(ProcessState.RUNNING);
            currentQuantum = 0;
            System.out.println("Planificador selecciona Proceso: " + currentProcess.getName());
        } else {
            currentProcess = null;
            System.out.println("No hay procesos disponibles en ready queue");
        }
    }
    
    private void executeCurrentProcess() {
        boolean processFinished = currentProcess.executeInstruction();
        
        if (processFinished) {
            // Proceso terminado
            currentProcess.setState(ProcessState.TERMINATED);
            terminatedProcesses.add(currentProcess);
            completedProcesses++;
            System.out.println("Proceso terminado: " + currentProcess.getName());
            currentProcess = null;
        } else if (currentProcess.getState() == ProcessState.BLOCKED) {
            // CORRECCIÓN: El proceso se bloqueó durante executeInstruction
            System.out.println("Proceso bloqueado durante ejecución: " + currentProcess.getName());
            // No hacer nada - ya está en la cola de bloqueados
            currentProcess = null;
        } else {
            currentQuantum++;
        }
    }
    
    /**
     * CORRECCIÓN: Suspensión menos agresiva
     */
    private void updateSuspendedProcesses() {
        // Solo suspender si hay muchos procesos listos (más de 5)
        if (readyQueue.size() > 5) {
            PCB processToSuspend = readyQueue.extract();
            if (processToSuspend != null) {
                processToSuspend.setState(ProcessState.SUSPENDED);
                suspendedQueue.add(processToSuspend);
                System.out.println("Proceso suspendido: " + processToSuspend.getName());
            }
        }
        
        // Reactivar procesos suspendidos cuando hay pocos procesos listos
        if (readyQueue.size() < 3 && suspendedQueue.size() > 0) {
            PCB processToResume = suspendedQueue.removeAt(0);
            processToResume.setState(ProcessState.READY);
            readyQueue.insert(processToResume);
            System.out.println("Proceso reanudado: " + processToResume.getName());
        }
    }
    
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
    
    public void addToBlockedQueue(PCB process) {
        try {
            mutex.acquire();
            // Verificar si ya está bloqueado
            boolean alreadyBlocked = false;
            for (int i = 0; i < blockedQueue.size(); i++) {
                if (blockedQueue.get(i).getId() == process.getId()) {
                    alreadyBlocked = true;
                    break;
                }
            }
            
            if (!alreadyBlocked) {
                process.setState(ProcessState.BLOCKED);
                blockedQueue.add(process);
                System.out.println("Proceso bloqueado: " + process.getName());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            mutex.release();
        }
    }
    
    private void updateMetrics() {
        if (currentProcess != null && currentProcess.getState() == ProcessState.RUNNING) {
            totalCpuBusyTime++;
        }
        
        // Actualizar tiempos de espera
        PCB[] readyProcesses = readyQueue.toArray();
        for (PCB process : readyProcesses) {
            if (process != null) {
                process.setWaitingTime(process.getWaitingTime() + 1);
            }
        }
        
        for (int i = 0; i < blockedQueue.size(); i++) {
            PCB process = blockedQueue.get(i);
            process.setWaitingTime(process.getWaitingTime() + 1);
        }
    }
    
    public void setCurrentAlgorithm(SchedulingAlgorithm algorithm) {
        try {
            mutex.acquire();
            this.currentAlgorithm = algorithm;
            
            ProcessHeap newReadyQueue = new ProcessHeap(100, algorithm);
            
            PCB[] oldProcesses = this.readyQueue.toArray();
            for (PCB process : oldProcesses) {
                if (process != null && process.getState() == ProcessState.READY) {
                    newReadyQueue.insert(process);
                }
            }
            
            this.readyQueue = newReadyQueue;
            System.out.println("Algoritmo cambiado a: " + algorithm);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            mutex.release();
        }
    }
    
    // Getters y métodos de métricas...
    public PCB getCurrentProcess() { return currentProcess; }
    public ProcessHeap getReadyQueue() { return readyQueue; }
    public CustomList<PCB> getBlockedQueue() { return blockedQueue; }
    public CustomList<PCB> getSuspendedQueue() { return suspendedQueue; }
    public CustomList<PCB> getTerminatedProcesses() { return terminatedProcesses; }
    public long getGlobalCycle() { return globalCycle; }
    public boolean isOperatingSystemRunning() { return isOperatingSystemRunning; }
    public SchedulingAlgorithm getCurrentAlgorithm() { return currentAlgorithm; }
    
    public void setTimeQuantum(int quantum) { this.timeQuantum = quantum; }
    
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
    
    public void shutdown() {
        // No hay hilos que detener en esta versión simplificada
    }
}