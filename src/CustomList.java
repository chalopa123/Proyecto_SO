/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author chalo
 */
/**
 * Implementación personalizada de una lista para reemplazar ArrayList
 * Cumple con los requerimientos de no usar librerías de estructuras de datos
 */
public class CustomList<T> {
    private Object[] elements;
    private int size;
    private static final int DEFAULT_CAPACITY = 10;
    
    public CustomList() {
        this.elements = new Object[DEFAULT_CAPACITY];
        this.size = 0;
    }
    
    public CustomList(int initialCapacity) {
        this.elements = new Object[initialCapacity];
        this.size = 0;
    }
    
    public void add(T element) {
        ensureCapacity();
        elements[size++] = element;
    }
    
    @SuppressWarnings("unchecked")
    public T get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
        return (T) elements[index];
    }
    
    public boolean remove(T element) {
        for (int i = 0; i < size; i++) {
            if (elements[i].equals(element)) {
                removeAt(i);
                return true;
            }
        }
        return false;
    }
    
    @SuppressWarnings("unchecked")
    public T removeAt(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
        
        T removedElement = (T) elements[index];
        int numMoved = size - index - 1;
        if (numMoved > 0) {
            System.arraycopy(elements, index + 1, elements, index, numMoved);
        }
        elements[--size] = null;
        return removedElement;
    }
    
    public int size() {
        return size;
    }
    
    public boolean isEmpty() {
        return size == 0;
    }
    
    @SuppressWarnings("unchecked")
    public T[] toArray(T[] a) {
        if (a.length < size) {
            return (T[]) java.util.Arrays.copyOf(elements, size, a.getClass());
        }
        System.arraycopy(elements, 0, a, 0, size);
        if (a.length > size) {
            a[size] = null;
        }
        return a;
    }
    
    public Object[] toArray() {
        return java.util.Arrays.copyOf(elements, size);
    }
    
    private void ensureCapacity() {
        if (size == elements.length) {
            int newCapacity = elements.length * 2;
            elements = java.util.Arrays.copyOf(elements, newCapacity);
        }
    }
    
    public void clear() {
        for (int i = 0; i < size; i++) {
            elements[i] = null;
        }
        size = 0;
    }
}
