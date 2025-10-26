/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author chalo
 */
// Archivo: ConfigManager.java
// (Sin 'package' al inicio)

import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
// Usamos java.util.HashMap solo para el manejo interno de la carga de datos.
// No se usa como estructura de datos en la simulación principal.
import java.util.HashMap; 
import java.util.Map;

/**
 * Gestiona el guardado y la carga de la configuración de simulación
 * desde/hacia archivos CSV.
 */
public class ConfigManager {

    private static final String MEMORY_KEY = "totalMemory";
    private static final String ALGORITHM_KEY = "startAlgorithm";
    private static final String DURATION_KEY = "cycleDuration";
    private static final String DELIMITER = ";";

    /**
     * Guarda la configuración actual en un archivo CSV.
     */
    public void saveConfig(String filePath, SimulationConfig config) throws Exception {
        // Usamos try-with-resources para asegurar que el writer se cierre
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write(MEMORY_KEY + DELIMITER + config.getTotalMemory());
            writer.newLine();
            writer.write(ALGORITHM_KEY + DELIMITER + config.getStartAlgorithm().name());
            writer.newLine();
            writer.write(DURATION_KEY + DELIMITER + config.getInitialCycleDuration());
            writer.newLine();
        }
    }

    /**
     * Carga la configuración desde un archivo CSV.
     * Devuelve un mapa con los valores leídos.
     */
    public Map<String, String> loadConfig(String filePath) throws Exception {
        Map<String, String> configMap = new HashMap<>();
        // Usamos try-with-resources para asegurar que el reader se cierre
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(DELIMITER);
                if (parts.length == 2) {
                    configMap.put(parts[0].trim(), parts[1].trim());
                }
            }
        }
        
        // Validar que el archivo cargado tiene las claves necesarias
        if (!configMap.containsKey(MEMORY_KEY) || 
            !configMap.containsKey(ALGORITHM_KEY) || 
            !configMap.containsKey(DURATION_KEY)) {
            
            throw new Exception("El archivo de configuración es inválido o está incompleto.");
        }
        
        return configMap;
    }
}
