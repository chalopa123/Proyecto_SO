package simulator.structures;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author chalo
 */
import simulator.core.SchedulingAlgorithm;
import simulator.core.PCB;
import java.util.Arrays;

/**
 * Implementación de Montículo (Heap) 
 */
public class ProcessHeap {
    private PCB[] heap;
    private int size;
    private final boolean isMinHeap;
    private final SchedulingAlgorithm algorithm;
    
    public ProcessHeap(int capacity, boolean isMinHeap, SchedulingAlgorithm algorithm) {
        this.heap = new PCB[capacity];
        this.size = 0;
        this.isMinHeap = isMinHeap;
        this.algorithm = algorithm;
    }
    
    public ProcessHeap(int capacity, SchedulingAlgorithm algorithm) {
        this(capacity, true, algorithm);
    }
    
    public boolean isEmpty() {
        return size == 0;
    }
    
    public int size() {
        return size;
    }
    
    /**
     * Inserta proceso en el montículo
     * @param process proceso a insertar
     */
    public void insert(PCB process) {
        if (size == heap.length) {
            resize();
        }
        
        heap[size] = process;
        heapifyUp(size);
        size++;
    }
    
    /**
     * Extrae proceso con mayor o menor prioridad según el tipo de montículo
     * @return el proceso extraído o null si está vacío
     */
    public PCB extract() {
        if (isEmpty()) {
            return null;
        }
        
        PCB root = heap[0];
        heap[0] = heap[size - 1];
        heap[size - 1] = null;
        size--;
        heapifyDown(0);
        
        return root;
    }
    
    /**
     * Obtiene el proceso en la raíz sin extraerlo
     * @return el proceso en la raíz o null si está vacío
     */
    public PCB peek() {
        if (isEmpty()) {
            return null;
        }
        return heap[0];
    }
    
    /**
     * Elimina proceso específico del montículo
     * @param process el proceso a eliminar
     * @return true si se eliminó, false caso contrario
     */
    public boolean remove(PCB process) {
        for (int i = 0; i < size; i++) {
            if (heap[i] != null && heap[i].getId() == process.getId()) {
                heap[i] = heap[size - 1];
                heap[size - 1] = null;
                size--;
                heapifyDown(i);
                return true;
            }
        }
        return false;
    }
    
    /**
     * Reorganiza el montículo hacia arriba
     */
    private void heapifyUp(int index) {
        int parent = (index - 1) / 2;
        
        while (index > 0 && compare(heap[index], heap[parent]) < 0) {
            swap(index, parent);
            index = parent;
            parent = (index - 1) / 2;
        }
    }
    
    /**
     * Reorganiza el montículo hacia abajo
     */
    private void heapifyDown(int index) {
        int smallest = index;
        int left = 2 * index + 1;
        int right = 2 * index + 2;
        
        if (left < size && compare(heap[left], heap[smallest]) < 0) {
            smallest = left;
        }
        
        if (right < size && compare(heap[right], heap[smallest]) < 0) {
            smallest = right;
        }
        
        if (smallest != index) {
            swap(index, smallest);
            heapifyDown(smallest);
        }
    }
    
    /**
     * Compara dos procesos según el algoritmo de planificación
     */
    private int compare(PCB p1, PCB p2) {
        if (p1 == null || p2 == null) return 0;
        
        int result;
        switch (algorithm) {
            case FCFS:
                result = Long.compare(p1.getCreationTime(), p2.getCreationTime());
                break;
            case SJF:
                result = Integer.compare(p1.getTotalInstructions(), p2.getTotalInstructions());
                break;
            case SRTF:
                result = Integer.compare(p1.getRemainingInstructions(), p2.getRemainingInstructions());
                break;
            case RR:
                result = Long.compare(p1.getLastReadyQueueTime(), p2.getLastReadyQueueTime());
                break;
            case PRIORITY:
                result = Integer.compare(p1.getPriority(), p2.getPriority());
                break;
            case MLFQ:
                result = Long.compare(p1.getCreationTime(), p2.getCreationTime());
                break;
            case HRRN:
                double s1 = Math.max(1.0, p1.getServiceTime());
                double ratioP1 = (p1.getWaitingTime() + s1) / s1;

                double s2 = Math.max(1.0, p2.getServiceTime());
                double ratioP2 = (p2.getWaitingTime() + s2) / s2;
                result = Double.compare(ratioP2, ratioP1); 
                break;
            default:
                result = 0; 
                break;
        }
        
        return isMinHeap ? result : -result;
    }
    
    private void swap(int i, int j) {
        PCB temp = heap[i];
        heap[i] = heap[j];
        heap[j] = temp;
    }
    
    private void resize() {
        heap = Arrays.copyOf(heap, heap.length * 2);
    }
    
    public Object[] toArray() {
        return java.util.Arrays.copyOf(heap, size);
    }
    
    public void clear() {
        Arrays.fill(heap, null);
        size = 0;
    }
}