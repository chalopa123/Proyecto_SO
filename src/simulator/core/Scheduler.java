package simulator.core;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author chalo
 */

// Contenido completo de Scheduler.java (Corregido y Final)

import simulator.utils.ExceptionHandlerThread;
import simulator.io.SimulationConfig;
import simulator.structures.ProcessHeap;
import simulator.structures.CustomList;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Map;
import java.util.HashMap;

/**
 * Planificador principal 
 */
public class Scheduler implements Runnable { // 
    
    // Colas de procesos 
    private ProcessHeap readyQueue;
    private final CustomList<PCB> blockedQueue;
    private final CustomList<PCB> suspendedQueue;
    private final CustomList<PCB> terminatedProcesses;
    private final CustomList<PCB> newQueue;
    private int maxMultiprogrammingLevel = 5; 
    
    // estado de la sim
    private PCB currentProcess;
    private SchedulingAlgorithm currentAlgorithm;
    private int timeQuantum;
    private int currentQuantum;
    private long globalCycle;
    private volatile boolean isOperatingSystemRunning; 
    private final ReentrantLock mutex;
    private volatile boolean isCpuIdle; 
    private volatile int cycleDuration = 1000;
    private final int totalMemory;
    private int usedMemory;
    
    private Thread simulationThread;
    private ExceptionHandlerThread exceptionHandlerThread;

    private volatile Object[] readyQueueCache = new Object[0];
    private volatile CustomList<PCB> blockedQueueCache = new CustomList<>();
    private volatile CustomList<PCB> suspendedQueueCache = new CustomList<>();
    private volatile CustomList<PCB> terminatedQueueCache = new CustomList<>();
    private volatile CustomList<PCB> newQueueCache = new CustomList<>();
    
    // Métricas 
    private int completedProcesses;
    private long totalCpuBusyTime;
    private long totalWaitTime;
    private long totalResponseTime;
    private final long startTime;
    private CustomList<Integer> cpuUsageHistory = new CustomList<>(); 
    private CustomList<Integer> globalCycleHistory = new CustomList<>();
    
    
    private volatile CustomList<Integer> cpuUsageHistoryCache = new CustomList<>();
    private volatile CustomList<Integer> globalCycleHistoryCache = new CustomList<>();
    private CustomList<Integer> terminatedHistory = new CustomList<>();
    private volatile CustomList<Integer> terminatedHistoryCache;

    public Scheduler(SimulationConfig config) {
        this.newQueue = new CustomList<>();
        this.usedMemory = 0;
        this.readyQueue = new ProcessHeap(100, config.getStartAlgorithm()); 
        this.blockedQueue = new CustomList<>();
        this.suspendedQueue = new CustomList<>();
        this.terminatedProcesses = new CustomList<>();
        this.currentAlgorithm = config.getStartAlgorithm(); 
        this.timeQuantum = 4;
        this.mutex = new ReentrantLock();
        this.isOperatingSystemRunning = false;
        this.isCpuIdle = true;
        this.startTime = System.currentTimeMillis();

        this.totalMemory = config.getTotalMemory(); 

        this.cycleDuration = config.getInitialCycleDuration(); 

        this.cpuUsageHistoryCache = new CustomList<>();
        this.globalCycleHistoryCache = new CustomList<>();
        this.terminatedHistoryCache = new CustomList<>();
    }
    
    /**
     * Inicia el hilo de simulación y el hilo de excepciones.
     */
    public void start() {
        mutex.lock();
        try {
            this.isOperatingSystemRunning = true;
            
            cpuUsageHistory.clear();
            globalCycleHistory.clear();
            cpuUsageHistoryCache.clear();
            globalCycleHistoryCache.clear();
            terminatedHistory.clear(); 
            terminatedHistoryCache.clear(); 
            
            if (this.exceptionHandlerThread == null || !this.exceptionHandlerThread.isAlive()) {
                this.exceptionHandlerThread = new ExceptionHandlerThread(this);
                this.exceptionHandlerThread.start();
            }
            
            if (this.simulationThread == null || !this.simulationThread.isAlive()) {
                this.simulationThread = new Thread(this);
                this.simulationThread.setName("SchedulerThread");
                this.simulationThread.start();
            }
        } finally {
            mutex.unlock();
        }
    }

    /**
     * Bucle principal del hilo de simulación.
     */
    @Override
    public void run() {
        while (isOperatingSystemRunning) {
            try {
                executeCycle(); 
                
                Thread.sleep(this.cycleDuration); 
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); 
                break; 
            } catch (Exception e) {
                System.err.println("Error en el bucle de simulación: " + e.getMessage());
            }
        }
        System.out.println("Hilo del Scheduler detenido.");
    }

    /**
     * Detiene el planificador y sus hilos asociados.
     */
    public void shutdown() {
        this.isOperatingSystemRunning = false; 
        cpuUsageHistory.clear();
        globalCycleHistory.clear();
        cpuUsageHistoryCache.clear();
        globalCycleHistoryCache.clear();
        terminatedHistory.clear(); 
        terminatedHistoryCache.clear(); 
        
        if (this.exceptionHandlerThread != null) {
            this.exceptionHandlerThread.stopHandler();
        }
        
        if (this.simulationThread != null) {
            this.simulationThread.interrupt(); 
        }
    }

    /**
     * Ciclo principal de ejecución.
     * sección crítica.
     */
    public void executeCycle() {
        mutex.lock();
        try {
            if (!isOperatingSystemRunning) {
                return;
            }
            globalCycle++;
            
            globalCycleHistory.add((int)globalCycle);
            cpuUsageHistory.add(this.isCpuIdle ? 0 : 1);
            terminatedHistory.add(terminatedProcesses.size());
            
            longTermScheduler();
            mediumTermScheduler();
            
            this.isCpuIdle = (currentProcess == null);
            updateSuspendedProcesses();
            
            if (currentProcess == null || currentProcess.getState() != ProcessState.RUNNING || 
                (currentAlgorithm == SchedulingAlgorithm.RR && currentQuantum >= timeQuantum)) {
                scheduleNextProcess();
            }
            
            if (currentProcess != null && currentProcess.getState() == ProcessState.RUNNING) {
                executeCurrentProcess();
            }
            
            updateMetrics();
            updateGUICache();
            
        } finally {
            mutex.unlock();
        }
    }

    /**
     * Agrega un nuevo proceso a la cola de listos 
     */
    public void addProcess(PCB process) {
        mutex.lock();
        try {
            newQueue.add(process);

            this.newQueueCache = createSnapshot(newQueue);
            
        } finally {
            mutex.unlock();
        }
    }

    /**
     * Desbloquea un proceso 
     */
    public void unblockProcess(PCB process) {
        mutex.lock();
        try {
            boolean removed = blockedQueue.remove(process);
            if (removed) {
                process.setState(ProcessState.READY);
                readyQueue.insert(process);
            }

            this.readyQueueCache = readyQueue.toArray();
            this.blockedQueueCache = createSnapshot(blockedQueue);
            
        } finally {
            mutex.unlock();
        }
    }

    /**
     * Cambia el algoritmo de planificación
     */
    public void setSchedulingAlgorithm(SchedulingAlgorithm algorithm) {
        mutex.lock();
        try {
            this.currentAlgorithm = algorithm;
            ProcessHeap newReadyQueue = new ProcessHeap(100, this.currentAlgorithm);
            
            while (!this.readyQueue.isEmpty()) {
                newReadyQueue.insert(this.readyQueue.extract()); 
            }
            this.readyQueue = newReadyQueue;
            
            this.readyQueueCache = readyQueue.toArray();
            
        } finally {
            mutex.unlock();
        }
    }

    
    /**
     * Planificador de Largo Plazo (Long-Term Scheduler).
     * proceso de NEW a READY.
     */
    private void longTermScheduler() {
        if (!newQueue.isEmpty()) {
            PCB processToAdmit = newQueue.get(0);

            if (usedMemory + processToAdmit.getMemorySize() <= totalMemory) {
                PCB process = newQueue.removeAt(0);
                process.setState(ProcessState.READY);
                readyQueue.insert(process);

                usedMemory += process.getMemorySize();

                System.out.println("LTS: Proceso " + process.getName() + " admitido a READY. (Memoria: " + usedMemory + "/" + totalMemory + ")");
            }
        }
    }
    
    /**
    * Planificador de Mediano Plazo (Medium-Term Scheduler).
    * suspender un proceso para liberar memoria.
    */
   private void mediumTermScheduler() {
       if (newQueue.isEmpty() || blockedQueue.isEmpty()) {
           return; 
       }

       PCB nextNewProcess = newQueue.get(0);
       int availableMemory = totalMemory - usedMemory;

       if (nextNewProcess.getMemorySize() > availableMemory) {

           PCB processToSuspend = blockedQueue.removeAt(0);
           processToSuspend.setState(ProcessState.SUSPENDED);
           suspendedQueue.add(processToSuspend);

           usedMemory -= processToSuspend.getMemorySize();

           System.out.println("MTS: Proceso " + processToSuspend.getName() + " SUSPENDIDO. (Memoria: " + usedMemory + "/" + totalMemory + ")");

           this.blockedQueueCache = createSnapshot(blockedQueue);
           this.suspendedQueueCache = createSnapshot(suspendedQueue);
       }
   }

    private void scheduleNextProcess() {
        if (currentProcess != null && currentProcess.getState() == ProcessState.RUNNING) {
            currentProcess.setState(ProcessState.READY);
            readyQueue.insert(currentProcess);
        }
        
        if (!readyQueue.isEmpty()) {
            currentProcess = readyQueue.extract();
            currentProcess.setState(ProcessState.RUNNING);
            currentQuantum = 0;
            if (currentProcess.getResponseTime() == -1) {
                currentProcess.setResponseTime(globalCycle);
            }
        } else {
            currentProcess = null;
        }
    }
    
    private void executeCurrentProcess() {
        totalCpuBusyTime++;
        this.isCpuIdle = false;
        currentProcess.executeInstruction(); 
        currentQuantum++;
        
        if (currentProcess.getState() == ProcessState.TERMINATED) {
            currentProcess.setTurnaroundTime(globalCycle);
            terminatedProcesses.add(currentProcess);
            completedProcesses++;
            usedMemory -= currentProcess.getMemorySize(); 
            System.out.println("Kernel: Proceso " + currentProcess.getName() + " TERMINADO. (Memoria: " + usedMemory + "/" + totalMemory + ")");
            currentProcess = null;
        } else if (currentProcess.getState() == ProcessState.BLOCKED) {
            blockedQueue.add(currentProcess);
            currentProcess = null;
        }
    }
    
    private void updateMetrics() {
        Object[] readyProcesses = readyQueue.toArray(); 
        for (Object obj : readyProcesses) {
            if (obj instanceof PCB) {
                ((PCB) obj).incrementWaitingTime();
                totalWaitTime++;
            }
        }
    }
    
    private void updateSuspendedProcesses() {
        if (suspendedQueue.isEmpty()) {
            return;
        }

        PCB processToResume = suspendedQueue.get(0);

        if (usedMemory + processToResume.getMemorySize() <= totalMemory) {
            suspendedQueue.removeAt(0); 
            processToResume.setState(ProcessState.READY);
            readyQueue.insert(processToResume);

            usedMemory += processToResume.getMemorySize();

            System.out.println("MTS: Proceso " + processToResume.getName() + " REANUDADO a READY. (Memoria: " + usedMemory + "/" + totalMemory + ")");

            this.suspendedQueueCache = createSnapshot(suspendedQueue);
        }
    }

    private void updateGUICache() {
        this.newQueueCache = createSnapshot(newQueue);
        this.readyQueueCache = readyQueue.toArray();
        this.blockedQueueCache = createSnapshot(blockedQueue);
        this.suspendedQueueCache = createSnapshot(suspendedQueue);
        this.terminatedQueueCache = createSnapshot(terminatedProcesses);
        this.cpuUsageHistoryCache = createSnapshot(cpuUsageHistory);
        this.globalCycleHistoryCache = createSnapshot(globalCycleHistory);
        this.terminatedHistoryCache = createSnapshot(terminatedHistory);
    }


    public CustomList<PCB> getNewQueueSnapshot() {
        return newQueueCache; 
    }
    
    public Object[] getReadyQueueSnapshot() {
        return readyQueueCache; 
    }

    public CustomList<PCB> getBlockedQueueSnapshot() {
        return blockedQueueCache; 
    }

    public CustomList<PCB> getSuspendedQueueSnapshot() {
        return suspendedQueueCache; 
    }

    public CustomList<PCB> getTerminatedQueueSnapshot() {
        return terminatedQueueCache; 
    }

    public PCB getCurrentProcessSnapshot() {
        mutex.lock();
        try { return currentProcess; }
        finally { mutex.unlock(); }
    }

    public long getGlobalCycleSnapshot() {
        mutex.lock();
        try { return globalCycle; }
        finally { mutex.unlock(); }
    }
    
    public boolean getIsOperatingSystemRunningSnapshot() {
        return isOperatingSystemRunning;
    }
    
    public boolean getIsCpuIdleSnapshot() {
        return isCpuIdle; 
    }
    
    public SchedulingAlgorithm getAlgorithmSnapshot() {
        mutex.lock();
        try { return currentAlgorithm; }
        finally { mutex.unlock(); }
    }
    
    public Map<String, Double> getPerformanceMetricsSnapshot() {
        mutex.lock();
        try {
            Map<String, Double> metrics = new HashMap<>();
            long elapsedTime = (System.currentTimeMillis() - startTime) / 1000;
            double throughput = elapsedTime > 0 ? (double) completedProcesses / elapsedTime : 0;
            double cpuUtilization = globalCycle > 0 ? (double) totalCpuBusyTime / globalCycle : 0;
            int totalProcesses = completedProcesses + readyQueue.size() + blockedQueue.size() + suspendedQueue.size();
            if (currentProcess != null) totalProcesses++;
            double avgWaitTime = totalProcesses > 0 ? (double) totalWaitTime / totalProcesses : 0;
            double avgResponseTime = completedProcesses > 0 ? (double) totalResponseTime / completedProcesses : 0;
            metrics.put("Throughput", throughput);
            metrics.put("CPU_Utilization", cpuUtilization);
            metrics.put("Avg_Wait_Time", avgWaitTime);
            metrics.put("Avg_Response_Time", avgResponseTime);
            return metrics;
        } finally {
            mutex.unlock();
        }
    }
    
    public CustomList<Integer> getCpuUsageHistory() {
        return cpuUsageHistoryCache; 
    }
    
    public CustomList<Integer> getGlobalCycleHistory() {
        return globalCycleHistoryCache; 
    }
    
    private <T> CustomList<T> createSnapshot(CustomList<T> original) {
        CustomList<T> snapshot = new CustomList<>();
        for (int i = 0; i < original.size(); i++) {
            snapshot.add(original.get(i));
        }
        return snapshot;
    }
    
    public CustomList<Integer> getTerminatedHistory() {
        return terminatedHistoryCache; 
    }
    
    
    public void setCycleDuration(int duration) {
        this.cycleDuration = duration; 
    }
    
    public int getCycleDuration() {
        return cycleDuration;
    }
}