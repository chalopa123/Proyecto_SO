/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author chalo
 */

// Contenido completo de Scheduler.java (Corregido y Final)

import java.util.concurrent.locks.ReentrantLock;
import java.util.Map;
import java.util.HashMap;

/**
 * Planificador principal (Thread-Safe) que se ejecuta en su propio hilo.
 * Utiliza un ReentrantLock para la modificación de datos y un sistema
 * de caché volátil (volatile cache) para que la GUI lea los datos
 * sin bloquearse.
 */
public class Scheduler implements Runnable { // <-- CAMBIO: Implementa Runnable
    
    // Colas de procesos (Protegidas por el mutex)
    private ProcessHeap readyQueue;
    private final CustomList<PCB> blockedQueue;
    private final CustomList<PCB> suspendedQueue;
    private final CustomList<PCB> terminatedProcesses;
    private final CustomList<PCB> newQueue;
    private int maxMultiprogrammingLevel = 5; 
    
    // --- ESTADO DE LA SIMULACIÓN ---
    private PCB currentProcess;
    private SchedulingAlgorithm currentAlgorithm;
    private int timeQuantum;
    private int currentQuantum;
    private long globalCycle;
    private volatile boolean isOperatingSystemRunning; // <-- CAMBIO: volatile
    private final ReentrantLock mutex;
    private volatile boolean isCpuIdle; // <-- NUEVA VARIABLE para el estado de la CPU
    private volatile int cycleDuration = 1000;
    private final int totalMemory;
    private int usedMemory;
    
    // --- HILOS ---
    private Thread simulationThread;
    private ExceptionHandlerThread exceptionHandlerThread;

    // --- CACHÉ PARA LA GUI (VOLATILE) ---
    // (Volatile asegura que la GUI siempre vea la copia más reciente)
    private volatile Object[] readyQueueCache = new Object[0];
    private volatile CustomList<PCB> blockedQueueCache = new CustomList<>();
    private volatile CustomList<PCB> suspendedQueueCache = new CustomList<>();
    private volatile CustomList<PCB> terminatedQueueCache = new CustomList<>();
    private volatile CustomList<PCB> newQueueCache = new CustomList<>();
    
    // Métricas (Protegidas por el mutex)
    private int completedProcesses;
    private long totalCpuBusyTime;
    private long totalWaitTime;
    private long totalResponseTime;
    private final long startTime;
    private CustomList<Integer> cpuUsageHistory = new CustomList<>(); // 0:Idle, 1:Busy
    private CustomList<Integer> globalCycleHistory = new CustomList<>();
    
    
    private volatile CustomList<Integer> cpuUsageHistoryCache = new CustomList<>();
    private volatile CustomList<Integer> globalCycleHistoryCache = new CustomList<>();
    private CustomList<Integer> terminatedHistory = new CustomList<>();
    private volatile CustomList<Integer> terminatedHistoryCache;

    // REEMPLAZAR CONSTRUCTOR
    public Scheduler(SimulationConfig config) {
        this.newQueue = new CustomList<>();
        this.usedMemory = 0;
        this.readyQueue = new ProcessHeap(100, config.getStartAlgorithm()); 
        this.blockedQueue = new CustomList<>();
        this.suspendedQueue = new CustomList<>();
        this.terminatedProcesses = new CustomList<>();
        this.currentAlgorithm = config.getStartAlgorithm(); // <-- USAR CONFIG
        this.timeQuantum = 4;
        this.mutex = new ReentrantLock();
        this.isOperatingSystemRunning = false;
        this.isCpuIdle = true;
        this.startTime = System.currentTimeMillis();

        // --- MODIFICACIONES DE CONSTRUCTOR ---
        // Eliminar hardcodeo
        // private final int totalMemory = 512; (eliminar línea)
        this.totalMemory = config.getTotalMemory(); 

        // Establecer duración inicial
        this.cycleDuration = config.getInitialCycleDuration(); 

        // Inicializar cachés de historial (para gráficos)
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
            terminatedHistory.clear(); // <-- AÑADIR ESTA LÍNEA
            terminatedHistoryCache.clear(); // <-- AÑADIR ESTA LÍNEA
            
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
                // 1. Ejecutar un ciclo (ya está protegido por el lock)
                executeCycle(); 
                
                // 2. Dormir durante la duración del ciclo
                Thread.sleep(this.cycleDuration); // Lee 'volatile cycleDuration'
                
            } catch (InterruptedException e) {
                // Ocurre cuando se llama a shutdown()
                Thread.currentThread().interrupt(); // Restablecer el flag
                break; // Salir del bucle
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
        // No necesitamos un lock aquí, solo seteamos un flag volatile
        // y llamamos a interrupt()
        this.isOperatingSystemRunning = false; 
        cpuUsageHistory.clear();
        globalCycleHistory.clear();
        cpuUsageHistoryCache.clear();
        globalCycleHistoryCache.clear();
        terminatedHistory.clear(); // <-- AÑADIR ESTA LÍNEA
        terminatedHistoryCache.clear(); // <-- AÑADIR ESTA LÍNEA
        
        if (this.exceptionHandlerThread != null) {
            this.exceptionHandlerThread.stopHandler();
        }
        
        if (this.simulationThread != null) {
            this.simulationThread.interrupt(); // Despertar de Thread.sleep()
        }
    }

    /**
     * Ciclo principal de ejecución (lógica del planificador).
     * Este método ES la sección crítica.
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
     * Agrega un nuevo proceso a la cola de listos de forma segura.
     */
    public void addProcess(PCB process) {
        mutex.lock();
        try {
            // El PCB se crea con estado NEW por defecto, solo lo añadimos a la cola.
            newQueue.add(process);
            
            // --- IMPORTANTE: Actualizar el caché de NUEVOS ---
            this.newQueueCache = createSnapshot(newQueue);
            
        } finally {
            mutex.unlock();
        }
    }

    /**
     * Desbloquea un proceso (ej. por fin de E/S).
     */
    public void unblockProcess(PCB process) {
        mutex.lock();
        try {
            boolean removed = blockedQueue.remove(process);
            if (removed) {
                process.setState(ProcessState.READY);
                readyQueue.insert(process);
            }
            
            // --- IMPORTANTE: Actualizar el caché ---
            this.readyQueueCache = readyQueue.toArray();
            this.blockedQueueCache = createSnapshot(blockedQueue);
            
        } finally {
            mutex.unlock();
        }
    }

    /**
     * Cambia el algoritmo de planificación en tiempo real.
     */
    public void setSchedulingAlgorithm(SchedulingAlgorithm algorithm) {
        mutex.lock();
        try {
            this.currentAlgorithm = algorithm;
            ProcessHeap newReadyQueue = new ProcessHeap(100, this.currentAlgorithm);
            
            while (!this.readyQueue.isEmpty()) {
                newReadyQueue.insert(this.readyQueue.extract()); // (Asumiendo 'extract()')
            }
            this.readyQueue = newReadyQueue;
            
            // --- IMPORTANTE: Actualizar el caché ---
            this.readyQueueCache = readyQueue.toArray();
            
        } finally {
            mutex.unlock();
        }
    }

    // --- MÉTODOS PRIVADOS (Ayudantes, ya están dentro de un lock) ---
    
    /**
     * Planificador de Largo Plazo (Long-Term Scheduler).
     * Decide cuándo mover un proceso de NEW a READY.
     * DEBE ser llamado desde un bloque SINCROZNIZADO (con lock).
     */
    private void longTermScheduler() {
        if (!newQueue.isEmpty()) {
            PCB processToAdmit = newQueue.get(0);

            // Si el proceso cabe en memoria...
            if (usedMemory + processToAdmit.getMemorySize() <= totalMemory) {
                // Mover el proceso de NEW a READY
                PCB process = newQueue.removeAt(0);
                process.setState(ProcessState.READY);
                readyQueue.insert(process);

                // Ocupar la memoria
                usedMemory += process.getMemorySize();

                System.out.println("LTS: Proceso " + process.getName() + " admitido a READY. (Memoria: " + usedMemory + "/" + totalMemory + ")");
            }
        }
    }
    
    /**
    * Planificador de Mediano Plazo (Medium-Term Scheduler).
    * Decide cuándo suspender un proceso para liberar memoria.
    * DEBE ser llamado desde un bloque SINCROZNIZADO (con lock).
    */
   private void mediumTermScheduler() {
       if (newQueue.isEmpty() || blockedQueue.isEmpty()) {
           return; // No hay necesidad de suspender
       }

       PCB nextNewProcess = newQueue.get(0);
       int availableMemory = totalMemory - usedMemory;

       // Si el siguiente proceso nuevo no cabe, Y hay procesos bloqueados que podemos suspender...
       if (nextNewProcess.getMemorySize() > availableMemory) {

           // Suspendemos el primer proceso de la cola de bloqueados (es una política simple)
           PCB processToSuspend = blockedQueue.removeAt(0);
           processToSuspend.setState(ProcessState.SUSPENDED);
           suspendedQueue.add(processToSuspend);

           // Liberar la memoria
           usedMemory -= processToSuspend.getMemorySize();

           System.out.println("MTS: Proceso " + processToSuspend.getName() + " SUSPENDIDO. (Memoria: " + usedMemory + "/" + totalMemory + ")");

           // Actualizar los cachés para la GUI
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
            currentProcess = readyQueue.extract(); // (Asumiendo 'extract()')
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
            usedMemory -= currentProcess.getMemorySize(); // <-- AÑADIR (LIBERAR MEMORIA)
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
            return; // No hay nada que reanudar
        }

        PCB processToResume = suspendedQueue.get(0);

        // Si ahora cabe en memoria...
        if (usedMemory + processToResume.getMemorySize() <= totalMemory) {
            suspendedQueue.removeAt(0); // Sacarlo de suspendidos

            // --- Lógica importante ---
            // El PDF menciona "listos suspendidos" y "bloqueados suspendidos"
            // Como nuestra lógica simple SÓLO suspende desde "BLOCKED",
            // lo devolvemos a "READY" (asumiendo que su E/S ya terminó mientras estaba suspendido)
            // Una implementación más compleja lo movería a "READY" o "BLOCKED"
            processToResume.setState(ProcessState.READY);
            readyQueue.insert(processToResume);

            // Ocupar la memoria
            usedMemory += processToResume.getMemorySize();

            System.out.println("MTS: Proceso " + processToResume.getName() + " REANUDADO a READY. (Memoria: " + usedMemory + "/" + totalMemory + ")");

            this.suspendedQueueCache = createSnapshot(suspendedQueue);
        }
    }

    /**
     * Actualiza todas las variables de caché.
     * DEBE ser llamado desde un bloque SINCROZNIZADO (con lock).
     */
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

    // --- MÉTODOS "SNAPSHOT" SEGUROS PARA LA GUI (NO BLOQUEANTES) ---
    // (Estos métodos leen el caché o usan un lock rápido)

    public CustomList<PCB> getNewQueueSnapshot() {
        return newQueueCache; // Lee el caché (no bloquea)
    }
    
    public Object[] getReadyQueueSnapshot() {
        return readyQueueCache; // Lee el caché (no bloquea)
    }

    public CustomList<PCB> getBlockedQueueSnapshot() {
        return blockedQueueCache; // Lee el caché (no bloquea)
    }

    public CustomList<PCB> getSuspendedQueueSnapshot() {
        return suspendedQueueCache; // Lee el caché (no bloquea)
    }

    public CustomList<PCB> getTerminatedQueueSnapshot() {
        return terminatedQueueCache; // Lee el caché (no bloquea)
    }

    // Para valores simples, podemos usar el lock, es casi instantáneo
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
        // 'isOperatingSystemRunning' es volatile, no necesita lock para leerse
        return isOperatingSystemRunning;
    }
    
    public boolean getIsCpuIdleSnapshot() {
        return isCpuIdle; // 'isCpuIdle' es volatile
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
        return cpuUsageHistoryCache; // Devuelve el caché volátil
    }
    
    public CustomList<Integer> getGlobalCycleHistory() {
        return globalCycleHistoryCache; // Devuelve el caché volátil
    }
    
    private <T> CustomList<T> createSnapshot(CustomList<T> original) {
        CustomList<T> snapshot = new CustomList<>();
        for (int i = 0; i < original.size(); i++) {
            snapshot.add(original.get(i));
        }
        return snapshot;
    }
    
    public CustomList<Integer> getTerminatedHistory() {
        return terminatedHistoryCache; // Devuelve el caché volátil
    }
    
    // --- CONTROL DE DURACIÓN DEL CICLO ---
    
    public void setCycleDuration(int duration) {
        this.cycleDuration = duration; // 'cycleDuration' es volatile
    }
    
    public int getCycleDuration() {
        return cycleDuration;
    }
}