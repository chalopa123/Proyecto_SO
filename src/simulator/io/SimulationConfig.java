package simulator.io;


import simulator.core.SchedulingAlgorithm;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author chalo
 */

/**
 * Almacena la configuración de la simulación.
 */
public class SimulationConfig {

    private final int totalMemory;
    private final SchedulingAlgorithm startAlgorithm;
    private final int initialCycleDuration;

    public SimulationConfig(int totalMemory, SchedulingAlgorithm startAlgorithm, int initialCycleDuration) {
        this.totalMemory = totalMemory;
        this.startAlgorithm = startAlgorithm;
        this.initialCycleDuration = initialCycleDuration;
    }

    public int getTotalMemory() {
        return totalMemory;
    }

    public SchedulingAlgorithm getStartAlgorithm() {
        return startAlgorithm;
    }

    public int getInitialCycleDuration() {
        return initialCycleDuration;
    }
}