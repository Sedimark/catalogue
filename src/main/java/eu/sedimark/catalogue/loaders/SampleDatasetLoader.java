package eu.sedimark.catalogue.loaders;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

/**
 * Utility class for managing SEDIMARK offerings in the dataset
 */
public class SampleDatasetLoader {
    
    private static final String SEDIMARK_NAMESPACE = "https://w3id.org/sedimark/ontology#";
    
    /**
     * Load the example offering from offerings_1.jsonld file
     */
    public static void loadExampleOffering(Dataset dataset) {
        try {
            System.out.println("Loading offering from offerings_100.jsonld...");

            // Path to the file (adjust if needed)
            String filePath = "src/main/resources/examples/offerings_1.jsonld";
            File file = new File(filePath);

            if (!file.exists()) {
                System.out.println("File not found: " + file.getAbsolutePath());
                // Try alternative path
                filePath = "jena-fuseki-server/src/main/resources/examples/offerings_1.jsonld";
                file = new File(filePath);
                if (!file.exists()) {
                    System.out.println("File not found at alternate path: " + file.getAbsolutePath());
                    return;
                }
            }

            // Parse the JSON-LD file into a model
            Model offeringModel = ModelFactory.createDefaultModel();
            try (InputStream is = new FileInputStream(file)) {
                RDFDataMgr.read(offeringModel, is, Lang.JSONLD);
            }

            System.out.println("Parsed offerings_1.jsonld with " + offeringModel.size() + " statements");

            // Find the offering resource in the model
            Resource offeringResource = null;
            for (Resource r : offeringModel.listSubjectsWithProperty(
                    RDF.type,
                    offeringModel.createResource(SEDIMARK_NAMESPACE + "Offering")).toList()) {
                offeringResource = r;
                System.out.println("Found offering: " + offeringResource.getURI());
                break;
            }

            if (offeringResource == null) {
                System.out.println("No offering found in the file. Looking for specific URI pattern...");

                // Try looking for ex:offering_1 specifically
                offeringResource = offeringModel.getResource("http://example.org/offering_1");
                if (offeringResource != null) {
                    System.out.println("Using explicit URI: " + offeringResource.getURI());
                } else {
                    System.out.println("Couldn't find offering resource in the file.");
                    return;
                }
            }

            // Store the offering in a named graph
            String graphName = offeringResource.getURI();
            System.out.println("Adding offering to dataset as named graph: " + graphName);

            dataset.begin(ReadWrite.WRITE);
            try {
                dataset.addNamedModel(graphName, offeringModel);
                dataset.commit();
                System.out.println("Successfully added offering graph with " + offeringModel.size() + " statements");
            } catch (Exception e) {
                dataset.abort();
                System.err.println("Error storing offering: " + e.getMessage());
                e.printStackTrace();
            } finally {
                dataset.end();
            }

        } catch (Exception e) {
            System.err.println("Error loading offering_1.jsonld: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Add a test offering to verify the system is working
     */
    public static void addTestOffering(Dataset dataset) {
        // Create a test named graph to verify functionality
        Model testModel = ModelFactory.createDefaultModel();
        Resource testOffering = testModel.createResource("http://example.org/testOffering");
        testModel.add(testOffering, RDF.type, testModel.createResource(SEDIMARK_NAMESPACE + "Offering"));
        testModel.add(testOffering, RDFS.label, "Test Offering");
        dataset.addNamedModel("http://example.org/testOffering", testModel);
        System.out.println("Added test offering to dataset: http://example.org/testOffering");

        // List all graphs in dataset
        System.out.println("\n==== Named Graphs in Dataset ====");
        dataset.listNames().forEachRemaining(name -> System.out
                .println("Graph: " + name + " with " + dataset.getNamedModel(name).size() + " statements"));
        System.out.println("===============================\n");
    }
    
    /**
     * Load a specific file into the dataset
     * 
     * @param dataset The dataset to add the data to
     * @param filename The filename in the examples directory
     * @return The URI of the offering that was loaded, or null if none was found
     */
    public static String loadFile(Dataset dataset, String filename) {
        try {
            System.out.println("Loading file: " + filename);

            // Path to the file
            String filePath = "src/main/resources/examples/" + filename;
            File file = new File(filePath);

            if (!file.exists()) {
                System.out.println("File not found: " + file.getAbsolutePath());
                // Try alternative path
                filePath = "jena-fuseki-server/src/main/resources/examples/" + filename;
                file = new File(filePath);
                if (!file.exists()) {
                    System.out.println("File not found at alternate path: " + file.getAbsolutePath());
                    return null;
                }
            }

            // Determine format from file extension
            Lang format = null;
            if (filename.endsWith(".jsonld")) {
                format = Lang.JSONLD;
            } else if (filename.endsWith(".ttl")) {
                format = Lang.TURTLE;
            } else if (filename.endsWith(".rdf") || filename.endsWith(".xml")) {
                format = Lang.RDFXML;
            } else {
                System.out.println("Unsupported file format: " + filename);
                return null;
            }

            // Parse the file into a model
            Model fileModel = ModelFactory.createDefaultModel();
            try (InputStream is = new FileInputStream(file)) {
                RDFDataMgr.read(fileModel, is, format);
            }

            System.out.println("Parsed " + filename + " with " + fileModel.size() + " statements");

            // Find the offering resource in the model
            Resource offeringResource = null;
            for (Resource r : fileModel.listSubjectsWithProperty(
                    RDF.type,
                    fileModel.createResource(SEDIMARK_NAMESPACE + "Offering")).toList()) {
                offeringResource = r;
                System.out.println("Found offering: " + offeringResource.getURI());
                break;
            }

            if (offeringResource == null) {
                System.out.println("No offering found in the file. Looking for URI patterns...");

                // Try looking for common patterns
                String[] patterns = {
                    "http://example.org/offering",
                    "https://w3id.org/sedimark/offering"
                };
                
                for (String pattern : patterns) {
                    for (Resource subject : fileModel.listSubjects().toList()) {
                        if (subject.isURIResource() && subject.getURI().contains(pattern)) {
                            offeringResource = subject;
                            System.out.println("Found potential offering by URI pattern: " + offeringResource.getURI());
                            break;
                        }
                    }
                    if (offeringResource != null) break;
                }
                
                if (offeringResource == null) {
                    System.out.println("Couldn't find offering resource in the file.");
                    return null;
                }
            }

            // Store the offering in a named graph
            String graphName = offeringResource.getURI();
            System.out.println("Adding offering to dataset as named graph: " + graphName);

            dataset.begin(ReadWrite.WRITE);
            try {
                dataset.addNamedModel(graphName, fileModel);
                dataset.commit();
                System.out.println("Successfully added offering graph with " + fileModel.size() + " statements");
                return graphName;
            } catch (Exception e) {
                dataset.abort();
                System.err.println("Error storing offering: " + e.getMessage());
                e.printStackTrace();
                return null;
            } finally {
                dataset.end();
            }

        } catch (Exception e) {
            System.err.println("Error loading " + filename + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}