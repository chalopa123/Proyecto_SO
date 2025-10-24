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
     * Inserta un proceso en el montículo
     * @param process el proceso a insertar
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
     * Elimina un proceso específico del montículo
     * @param process el proceso a eliminar
     * @return true si se eliminó, false en caso contrario
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
            case FCFS -> result = Long.compare(p1.getCreationTime(), p2.getCreationTime());
            case SJF -> result = Integer.compare(p1.getTotalInstructions(), p2.getTotalInstructions());
            case SRTF -> result = Integer.compare(p1.getRemainingInstructions(), p2.getRemainingInstructions());
            case RR -> result = Long.compare(p1.getCreationTime(), p2.getCreationTime());
            case PRIORITY -> {
                int p1Priority = (p1.getType() == ProcessType.IO_BOUND) ? 1 : 2;
                int p2Priority = (p2.getType() == ProcessType.IO_BOUND) ? 1 : 2;
                result = Integer.compare(p1Priority, p2Priority);
            }
            case MLFQ -> result = Long.compare(p1.getCreationTime(), p2.getCreationTime());
            default -> result = 0; // Caso por defecto
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
        // Devuelve una copia del array que contiene SOLO los elementos
        // desde el índice 0 hasta size.
        return java.util.Arrays.copyOf(heap, size);
    }
    
    public void clear() {
        Arrays.fill(heap, null);
        size = 0;
    }
}