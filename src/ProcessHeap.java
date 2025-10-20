/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author chalo
 */
import java.util.Arrays;

/**
 * Implementación de un Montículo (Heap) para la gestión de procesos
 * Esta estructura reemplaza el uso de ArrayList/LinkedList según los requerimientos
 */
public class ProcessHeap {
    private PCB[] heap;
    private int size;
    private final boolean isMinHeap;
    private SchedulingAlgorithm algorithm;
    
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
     * Inserta un proceso en el montículo
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
     * Extrae el proceso con mayor/menor prioridad según el tipo de montículo
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
     */
    public PCB peek() {
        if (isEmpty()) {
            return null;
        }
        return heap[0];
    }
    
    /**
     * Elimina un proceso específico del montículo
     */
    public boolean remove(PCB process) {
        for (int i = 0; i < size; i++) {
            if (heap[i].getId() == process.getId()) {
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
        int result = 0;
        
        switch (algorithm) {
            case FCFS:
                result = Long.compare(p1.getCreationTime(), p2.getCreationTime());
                break;
            case SJF:
            case SRTF:
                result = Integer.compare(p1.getRemainingInstructions(), p2.getRemainingInstructions());
                break;
            case RR:
                // Para RR, usamos FCFS como base
                result = Long.compare(p1.getCreationTime(), p2.getCreationTime());
                break;
            case PRIORITY:
                // Prioridad basada en el tipo de proceso
                int p1Priority = (p1.getType() == ProcessType.IO_BOUND) ? 1 : 2;
                int p2Priority = (p2.getType() == ProcessType.IO_BOUND) ? 1 : 2;
                result = Integer.compare(p1Priority, p2Priority);
                break;
            case MLFQ:
                // Implementación simplificada de MLFQ
                result = Long.compare(p1.getCreationTime(), p2.getCreationTime());
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
    
    public PCB[] toArray() {
        return Arrays.copyOf(heap, size);
    }
    
    public void clear() {
        Arrays.fill(heap, null);
        size = 0;
    }
}