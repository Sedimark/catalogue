package eu.sedimark.catalogue.utils;

import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.fuseki.servlets.ActionProcessor;
import org.apache.jena.query.Dataset;

import eu.sedimark.catalogue.handlers.OfferingGSPHandler;

/**
 * Helper class for debugging Fuseki server configurations.
 * Provides low-level access to Fuseki internals for advanced debugging.
 */
public class FusekiDebugHelper {

    /**
     * Register handlers with the low-level Fuseki dispatcher
     * This is used for debugging purposes when standard registration doesn't work
     * 
     * @param dataset The dataset to create handlers for
     */
    public static void registerHandlersWithDispatcher(Dataset dataset) {
        try {
            System.out.println("Attempting to register handler using low-level API...");

            // Try to access dispatcher using reflection
            Class<?> dispatcherClass = Class.forName("org.apache.jena.fuseki.server.Dispatcher");

            // Get the instance
            java.lang.reflect.Method getInstance = dispatcherClass.getMethod("getInstance");
            Object dispatcher = getInstance.invoke(null);

            // Create handler
            OfferingGSPHandler handler = new OfferingGSPHandler(dataset);

            // Register handler
            Class<?> operationClass = Class.forName("org.apache.jena.fuseki.server.Operation");
            java.lang.reflect.Field gspRField = operationClass.getField("GSP_R");
            Object gspROperation = gspRField.get(null);

            java.lang.reflect.Field gspRWField = operationClass.getField("GSP_RW");
            Object gspRWOperation = gspRWField.get(null);

            // Invoke register
            java.lang.reflect.Method registerMethod = dispatcherClass.getMethod("register", operationClass,
                    ActionProcessor.class);
            registerMethod.invoke(dispatcher, gspROperation, handler);
            registerMethod.invoke(dispatcher, gspRWOperation, handler);

            System.out.println("Successfully registered handler with low-level API");
        } catch (Exception e) {
            System.err.println("Failed to register handler with low-level API: " + e.getMessage());
        }
    }
    
    /**
     * Print detailed information about the registered processors
     * 
     * @param server The FusekiServer instance
     */
    public static void printProcessorInfo(FusekiServer server) {
        System.out.println("\n=== DEBUG INFO ===");
        System.out.println("Registered processor classes:");
        try {
            // Use reflection to extract registered processors
            java.lang.reflect.Field processorField = FusekiServer.class.getDeclaredField("processors");
            processorField.setAccessible(true);
            Object processors = processorField.get(server);
            System.out.println(processors);
        } catch (Exception e) {
            System.out.println("Could not extract processor info: " + e.getMessage());
        }
        System.out.println("=================\n");
    }
}