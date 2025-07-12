package eu.sedimark.catalogue.utils;

import org.apache.jena.fuseki.main.FusekiServer;
import java.io.File;
import java.nio.file.Paths;

/**
 * Helper class for handling command line arguments and server output
 */
public class ArgumentsHelper {

    /**
     * Parse command line arguments
     */
    public static Arguments parseArguments(String[] args) {
        Arguments arguments = new Arguments();
        
        if (args.length > 0) {
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                switch (arg) {
                    case "--tdb":
                        arguments.storageType = StorageType.TDB;
                        if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                            arguments.tdbLocation = args[++i];
                        }
                        break;
                    case "--memory":
                        arguments.storageType = StorageType.MEMORY;
                        break;
                    case "--port":
                        if (i + 1 < args.length) {
                            try {
                                arguments.port = Integer.parseInt(args[++i]);
                            } catch (NumberFormatException e) {
                                System.err.println("Invalid port number: " + args[i]);
                            }
                        }
                        break;
                    case "--load-examples":
                        arguments.loadExampleData = true;
                        break;
                    case "--debug":
                        arguments.debug = true;
                        break;
                    case "--help":
                        printHelp();
                        System.exit(0);
                        break;
                    default:
                        System.out.println("Unknown argument: " + arg);
                        printHelp();
                }
            }
        }
        
        // Use default TDB location if not specified
        if (arguments.storageType == StorageType.TDB && arguments.tdbLocation == null) {
            // Default to the application's directory
            String jarLocation = getJarDirectory();
            arguments.tdbLocation = Paths.get(jarLocation, "sedimark-tdb").toString();
        }
        
        return arguments;
    }

    /**
     * Get the directory where the JAR file is located
     */
    private static String getJarDirectory() {
        try {
            // Try to find the current JAR file location
            String jarPath = ArgumentsHelper.class.getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .getPath();
            
            File jarFile = new File(jarPath);
            if (jarFile.isFile()) { // It's a JAR file
                return jarFile.getParent();
            } else { // It's a directory (running from IDE)
                return new File(".").getAbsolutePath();
            }
        } catch (Exception e) {
            // Fallback to current working directory
            return new File(".").getAbsolutePath();
        }
    }

    /**
     * Print command line help
     */
    public static void printHelp() {
        System.out.println("SEDIMARK Catalogue Server - Command Line Options");
        System.out.println("-----------------------------------------------");
        System.out.println("--memory         Use in-memory storage (default)");
        System.out.println("--tdb [path]     Use TDB2 persistent storage at the specified path");
        System.out.println("                 Default: ./sedimark-tdb (relative to JAR location)");
        System.out.println("--port <number>  Specify the server port (default: 3030)");
        System.out.println("--load-examples  Load example offerings (disabled by default)");
        System.out.println("--debug          Enable debug mode with additional logging");
        System.out.println("--help           Show this help message");
        System.out.println("");
        System.out.println("Examples:");
        System.out.println("  java -jar sedimark-catalogue.jar --memory");
        System.out.println("  java -jar sedimark-catalogue.jar --tdb /data/sedimark-tdb");
        System.out.println("  java -jar sedimark-catalogue.jar --tdb --load-examples");
    }

    /**
     * Print information about the server endpoints
     */
    public static void printServerInformation(FusekiServer server, Arguments arguments) {
        System.out.println("\n========================================================");
        System.out.println("SEDIMARK Catalogue Server");
        System.out.println("Storage: " + arguments.storageType + 
                          (arguments.storageType == StorageType.TDB ? " at " + arguments.tdbLocation : ""));
        System.out.println("Port: " + server.getPort());
        System.out.println("========================================================");
        System.out.println("Server URLs:");
        System.out.println("  Base URL:          http://localhost:" + server.getPort() + "/");
        System.out.println("  Dataset:           http://localhost:" + server.getPort() + "/catalogue");
        System.out.println("  SPARQL endpoint:   http://localhost:" + server.getPort() + "/catalogue/sparql");
        System.out.println("  Update endpoint:   http://localhost:" + server.getPort() + "/catalogue/update");
        System.out.println("  GSP endpoint:      http://localhost:" + server.getPort() + "/catalogue/data");
        System.out.println("  Offering publish:  http://localhost:" + server.getPort() + "/catalogue/manager");
        System.out.println("  Graph listing:     http://localhost:" + server.getPort() + "/catalogue/graphs");
        System.out.println("  Test endpoint:     http://localhost:" + server.getPort() + "/catalogue/test");
        
        System.out.println("\nJSON-LD is enabled for Graph Store Protocol operations");
        System.out.println("Each sedimark:Offering will be stored in its own named graph");
        
        System.out.println("\nUsage examples:");
        System.out.println("  Publish offering:  curl -H \"Content-Type: application/ld+json\" -X POST --data @file.jsonld http://localhost:"
                          + server.getPort() + "/catalogue/manager");
        System.out.println("  Get offering:      curl -H \"Accept: application/ld+json\" http://localhost:" 
                          + server.getPort() + "/catalogue/manager?graph=<offering-uri>");
        System.out.println("  List offerings:    curl http://localhost:" + server.getPort() + "/catalogue/graphs");
        System.out.println("========================================================\n");
    }

    /**
     * Ensure that the TDB directory exists
     */
    public static void ensureDirectoryExists(String directoryPath) {
        File directory = new File(directoryPath);
        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            if (created) {
                System.out.println("Created TDB directory: " + directoryPath);
            } else {
                System.err.println("Failed to create TDB directory: " + directoryPath);
            }
        }
    }

    /**
     * Enum for storage type
     */
    public enum StorageType {
        MEMORY("In-Memory"),
        TDB("TDB2");
        
        private final String displayName;
        
        StorageType(String displayName) {
            this.displayName = displayName;
        }
        
        @Override
        public String toString() {
            return displayName;
        }
    }

    /**
     * Class to hold command line arguments
     */
    public static class Arguments {
        public StorageType storageType = StorageType.MEMORY;
        public String tdbLocation = null;
        public int port = 3030;
        public boolean loadExampleData = false; // Changed to false by default
        public boolean debug = false;
    }
}