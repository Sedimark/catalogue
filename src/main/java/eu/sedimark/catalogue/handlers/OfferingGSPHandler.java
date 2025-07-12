/*
 * SEDIMARK Catalogue Server
 * 
 * Copyright (C) 2025 Tarek Elsaleh
 * 
 * This program is licensed under the European Union Public License (EUPL) v1.2.
 * You may obtain a copy of the License at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.sedimark.catalogue.handlers;

// import org.apache.jena.fuseki.servlets.ActionService;
import org.apache.jena.fuseki.servlets.HttpAction;
import org.apache.jena.fuseki.servlets.ServletOps;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.atlas.web.ContentType;
import org.apache.jena.riot.web.HttpNames;

import org.apache.jena.fuseki.servlets.ActionProcessor; // Note: Use this import, not ActionService
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletResponse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Custom handler for Graph Store Protocol that extracts SEDIMARK Offerings
 * from client payloads and stores them as named graphs.
 */
public class OfferingGSPHandler implements ActionProcessor { // Implement, don't extend
    private static final Logger logger = LoggerFactory.getLogger(OfferingGSPHandler.class);
    private final Dataset dataset;

    // SEDIMARK ontology constants
    private static final String SEDIMARK_NS = "https://w3id.org/sedimark/ontology#";
    private static final String OFFERING_CLASS = SEDIMARK_NS + "Offering";


    /**
     * Constructor
     */
    public OfferingGSPHandler(Dataset dataset) {
        this.dataset = dataset;
        logger.info("SEDIMARK OfferingGSPHandler initialized - CUSTOM HANDLER ACTIVE");
    }

    /**
     * Process method from ActionProcessor - this handles all requests
     */
    @Override
    public void process(HttpAction action) {
        // Set a header that clearly shows this handler was used
        action.getResponse().setHeader("X-Handler", "OfferingGSPHandler");

        String method = action.getRequest().getMethod();
        String path = action.getRequest().getPathInfo();
        String query = action.getRequest().getQueryString();

        // Add detailed request logging
        logger.info("=============================================");
        logger.info("REQUEST: {} {}{}", method, path, query != null ? "?" + query : "");
        logger.info("Content-Type: {}", action.getRequest().getContentType());
        logger.info("Accept: {}", action.getRequest().getHeader("Accept"));
        logger.info("=============================================");

        try {
            // Dispatch based on HTTP method
            if ("GET".equals(method)) {
                if (action.getRequest().getParameter("graph") != null) {
                    handleGetGraphRequest(action);
                } else {
                    confirmCustomHandler(action);
                }
            } else if ("POST".equals(method) || "PUT".equals(method)) {
                handlePostPutRequest(action);
            } else if ("DELETE".equals(method)) {
                handleDeleteRequest(action);
            } else {
                // Error for unsupported methods
                action.getResponse().setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                action.getResponse().setContentType("application/json");
                String response = String.format(
                        "{ \"status\": \"error\", \"message\": \"Method not allowed: %s\" }",
                        method);
                action.getResponseOutputStream().write(response.getBytes());
            }
        } catch (Exception e) {
            handleError(action, e, "Error processing request");
        }
    }

    /**
     * Confirm that the custom handler is being used
     */
    private void confirmCustomHandler(HttpAction action) throws IOException {
        StringBuilder response = new StringBuilder();
        response.append("{\n");
        response.append("  \"status\": \"success\",\n");
        response.append("  \"handler\": \"SEDIMARK OfferingGSPHandler\",\n");
        response.append("  \"message\": \"Custom SEDIMARK handler is active and working\",\n");

        // Include information about stored offerings
        response.append("  \"storedOfferings\": {\n");

        dataset.begin(ReadWrite.READ);
        try {
            List<String> graphNames = new ArrayList<>();
            dataset.listNames().forEachRemaining(graphNames::add);
            response.append("    \"count\": ").append(graphNames.size()).append(",\n");

            if (!graphNames.isEmpty()) {
                response.append("    \"graphs\": [\n");
                for (int i = 0; i < graphNames.size(); i++) {
                    String graphName = graphNames.get(i);
                    response.append("      \"").append(graphName).append("\"");
                    if (i < graphNames.size() - 1) {
                        response.append(",");
                    }
                    response.append("\n");
                }
                response.append("    ]\n");
            } else {
                response.append("    \"graphs\": []\n");
            }
        } finally {
            dataset.end();
        }

        response.append("  }\n");
        response.append("}");

        action.getResponse().setContentType("application/json");
        action.getResponseOutputStream().write(response.toString().getBytes());
    }

    /**
     * Handle GET request to retrieve graph content
     */
    /**
     * Handle GET request to retrieve graph content
     */
    private void handleGetGraphRequest(HttpAction action) {
        try {
            String graphParam = action.getRequest().getParameter("graph");

            if (graphParam == null) {
                // Return 400 Bad Request if graph parameter is missing
                action.getResponse().setStatus(HttpServletResponse.SC_BAD_REQUEST);
                action.getResponse().setContentType("application/json");
                String response = "{ \"status\": \"error\", \"message\": \"Missing required 'graph' parameter\" }";
                action.getResponseOutputStream().write(response.getBytes());
                return;
            }

            String acceptHeader = action.getRequest().getHeader(HttpNames.hAccept);
            Lang outputLang = getOutputLang(acceptHeader);

            // Check if the client specifically wants metadata instead of the actual
            // offering
            String metadataParam = action.getRequest().getParameter("metadata");
            boolean wantMetadata = "true".equalsIgnoreCase(metadataParam);

            dataset.begin(ReadWrite.READ);
            try {
                if (dataset.containsNamedModel(graphParam)) {
                    // Graph exists, get its content
                    Model model = dataset.getNamedModel(graphParam);

                    // Log the prefixes available in the model
                    Map<String, String> prefixMap = model.getNsPrefixMap();
                    logger.debug("Retrieved model has {} prefixes", prefixMap.size());
                    if (logger.isDebugEnabled() && !prefixMap.isEmpty()) {
                        prefixMap.forEach((prefix, uri) -> logger.debug("  Prefix in model: {} -> {}", prefix, uri));
                    }

                    if (wantMetadata) {
                        // Return metadata as JSON if specifically requested
                        action.getResponse().setStatus(HttpServletResponse.SC_OK);
                        action.getResponse().setContentType("application/json");

                        StringBuilder responseBuilder = new StringBuilder();
                        responseBuilder.append("{\n");
                        responseBuilder.append("  \"status\": \"success\",\n");
                        responseBuilder.append("  \"graphUri\": \"").append(graphParam).append("\",\n");
                        responseBuilder.append("  \"statements\": ").append(model.size()).append(",\n");
                        responseBuilder.append("  \"prefixes\": ").append(prefixMap.size()).append(",\n");

                        // Add prefix details if there are any
                        if (!prefixMap.isEmpty()) {
                            responseBuilder.append("  \"prefixMap\": {\n");
                            int count = 0;
                            for (Map.Entry<String, String> entry : prefixMap.entrySet()) {
                                responseBuilder.append("    \"").append(entry.getKey()).append("\": \"")
                                        .append(entry.getValue()).append("\"");
                                if (count < prefixMap.size() - 1) {
                                    responseBuilder.append(",");
                                }
                                responseBuilder.append("\n");
                                count++;
                            }
                            responseBuilder.append("  }\n");
                        }

                        responseBuilder.append("}");
                        action.getResponseOutputStream().write(responseBuilder.toString().getBytes());
                    } else {
                        // Return the actual RDF data as requested by content negotiation

                        // For JSON-LD, set appropriate content type
                        if (outputLang == Lang.JSONLD) {
                            action.getResponse().setContentType("application/ld+json");
                        } else {
                            action.getResponse().setContentType(outputLang.getContentType().getContentTypeStr());
                        }

                        action.getResponse().setStatus(HttpServletResponse.SC_OK);

                        // Write the model in the requested RDF format
                        RDFDataMgr.write(action.getResponseOutputStream(), model, outputLang);
                    }
                } else {
                    // Graph doesn't exist - return 404 with JSON error response
                    action.getResponse().setStatus(HttpServletResponse.SC_NOT_FOUND);
                    action.getResponse().setContentType("application/json");
                    String response = String.format(
                            "{ \"status\": \"error\", \"message\": \"Graph not found: %s\" }",
                            graphParam);
                    action.getResponseOutputStream().write(response.getBytes());
                }
            } finally {
                if (dataset.isInTransaction()) {
                    dataset.end();
                }
            }
        } catch (Exception e) {
            handleError(action, e, "Error in handleGetGraphRequest");
        }
    }

    /**
     * Handle DELETE requests to remove a graph
     */
    private void handleDeleteRequest(HttpAction action) {
        try {
            String graphParam = action.getRequest().getParameter("graph");

            if (graphParam == null) {
                ServletOps.error(HttpServletResponse.SC_BAD_REQUEST, "Missing graph parameter");
                return;
            }

            dataset.begin(ReadWrite.WRITE);
            try {
                if (dataset.containsNamedModel(graphParam)) {
                    dataset.removeNamedModel(graphParam);
                    dataset.commit();

                    // Send success response
                    action.getResponse().setStatus(HttpServletResponse.SC_OK);
                    action.getResponse().setContentType("application/json");
                    String response = String.format(
                            "{ \"status\": \"success\", \"message\": \"Offering graph deleted: %s\" }",
                            graphParam);
                    action.getResponseOutputStream().write(response.getBytes());
                } else {
                    dataset.abort();
                    action.getResponse().setStatus(HttpServletResponse.SC_NOT_FOUND);
                    action.getResponse().setContentType("application/json");
                    String response = String.format(
                            "{ \"status\": \"error\", \"message\": \"Graph not found: %s\" }",
                            graphParam);
                    action.getResponseOutputStream().write(response.getBytes());
                }
            } catch (Exception e) {
                dataset.abort();
                throw e;
            } finally {
                dataset.end();
            }
        } catch (Exception e) {
            handleError(action, e, "Error processing DELETE request");
        }
    }

    /**
     * Handle POST/PUT requests to store offerings in named graphs
     */
    private void handlePostPutRequest(HttpAction action) {
        try {
            logger.info("Processing {} request", action.getRequest().getMethod());
            logger.info("=============================================");
            logger.info("STARTING POST/PUT PROCESSING");
            logger.info("Request Content-Type: {}", action.getRequest().getContentType());

            // Read the request body
            byte[] requestBody = readRequestBody(action);
            if (requestBody.length == 0) {
                action.getResponse().setStatus(HttpServletResponse.SC_BAD_REQUEST);
                action.getResponse().setContentType("application/json");
                action.getResponseOutputStream().write(
                        "{ \"status\": \"error\", \"message\": \"Empty request body\" }".getBytes());
                return;
            }

            // Parse content type
            String contentType = action.getRequest().getContentType();
            Lang lang = RDFLanguages.contentTypeToLang(contentType);
            if (lang == null) {
                action.getResponse().setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
                action.getResponse().setContentType("application/json");
                action.getResponseOutputStream().write(
                        String.format("{ \"status\": \"error\", \"message\": \"Unsupported content type: %s\" }",
                                contentType).getBytes());
                return;
            }

            logger.info("Content-Type: {}, format: {}", contentType, lang.getName());

            // Parse into a model
            Model inputModel = ModelFactory.createDefaultModel();
            try (InputStream is = new ByteArrayInputStream(requestBody)) {
                RDFDataMgr.read(inputModel, is, lang);
            }
            logger.info("Parsed model with {} statements", inputModel.size());

            // Store a copy of the original input string for potential prefix debugging
            String originalInput = new String(requestBody, "UTF-8");
            logger.debug("Original input string: {}", originalInput);

            // Extract prefixes from the input model FIRST
            Map<String, String> originalPrefixes = inputModel.getNsPrefixMap();
            logger.info("Input data contains {} namespace prefixes", originalPrefixes.size());
            if (!originalPrefixes.isEmpty() && logger.isDebugEnabled()) {
                logger.debug("Namespace prefixes in input:");
                originalPrefixes.forEach((prefix, uri) -> logger.debug("  {}: {}", prefix, uri));
            }

            // Extract offerings and their subgraphs - passing the original prefixes
            List<NamedSubgraph> namedGraphs = extractOfferingGraphs(inputModel, originalPrefixes);

            if (namedGraphs.isEmpty()) {
                logger.warn("No offerings found in input");
                action.getResponse().setStatus(HttpServletResponse.SC_BAD_REQUEST);
                action.getResponse().setContentType("application/json");
                action.getResponseOutputStream().write(
                        String.format(
                                "{ \"status\": \"error\", \"message\": \"No offerings found. Input contains %d triples, but no resources of type %s\" }",
                                inputModel.size(), OFFERING_CLASS).getBytes());
                return;
            }

            // Store each named graph in the dataset
            storeOfferingGraphs(namedGraphs);

            // Prepare success response with details about stored offerings
            StringBuilder responseBuilder = new StringBuilder();
            responseBuilder.append("{\n");
            responseBuilder.append("  \"status\": \"success\",\n");
            responseBuilder.append("  \"message\": \"Offerings stored successfully\",\n");
            responseBuilder.append("  \"storedOfferings\": [\n");

            for (int i = 0; i < namedGraphs.size(); i++) {
                NamedSubgraph graph = namedGraphs.get(i);
                responseBuilder.append("    {\n");
                responseBuilder.append("      \"uri\": \"").append(graph.getGraphName()).append("\",\n");
                responseBuilder.append("      \"statements\": ").append(graph.getModel().size()).append(",\n");
                responseBuilder.append("      \"prefixes\": ").append(graph.getModel().getNsPrefixMap().size())
                        .append("\n");
                responseBuilder.append("    }");
                if (i < namedGraphs.size() - 1) {
                    responseBuilder.append(",");
                }
                responseBuilder.append("\n");
            }

            responseBuilder.append("  ],\n");
            responseBuilder.append("  \"totalOfferings\": ").append(namedGraphs.size()).append("\n");
            responseBuilder.append("}");

            // Send the success response
            action.getResponse().setStatus(HttpServletResponse.SC_OK);
            action.getResponse().setContentType("application/json");
            action.getResponseOutputStream().write(responseBuilder.toString().getBytes());

        } catch (Exception e) {
            handleError(action, e, "Error processing POST/PUT request");
        }
    }

    /**
     * Extract named graphs based on SEDIMARK offerings
     */
    private List<NamedSubgraph> extractOfferingGraphs(Model inputModel, Map<String, String> originalPrefixes) {
        List<NamedSubgraph> result = new ArrayList<>();

        // Find all offering instances in the model
        Set<Resource> offerings = findOfferingResources(inputModel);
        logger.info("Found {} offering resources", offerings.size());

        // Extract subgraph for each offering
        for (Resource offering : offerings) {
            if (!offering.isURIResource()) {
                logger.warn("Skipping offering without URI: {}", offering);
                continue;
            }

            String graphName = offering.getURI();
            Model subgraph = extractOfferingSubgraph(inputModel, offering, originalPrefixes);

            logger.info("Created subgraph for {} with {} statements and {} prefixes",
                    graphName, subgraph.size(), subgraph.getNsPrefixMap().size());
            result.add(new NamedSubgraph(graphName, subgraph));
        }

        return result;
    }

    /**
     * Find all resources that are SEDIMARK Offerings in the model
     * 
     * @param model The RDF model to search
     * @return Set of resources that are of type SEDIMARK Offering
     */
    private Set<Resource> findOfferingResources(Model model) {
        Set<Resource> offerings = new HashSet<>();

        // Find all resources that have RDF type of SEDIMARK Offering
        Property rdfType = RDF.type;
        Resource offeringClass = ResourceFactory.createResource(OFFERING_CLASS);

        StmtIterator stmts = model.listStatements(null, rdfType, offeringClass);
        while (stmts.hasNext()) {
            Statement stmt = stmts.next();
            Resource subject = stmt.getSubject();
            offerings.add(subject);
            logger.debug("Found offering: {}", subject.getURI());
        }

        // If no direct offerings were found, try to infer from subclasses
        if (offerings.isEmpty()) {
            logger.info("No direct offerings found with type {}. Looking for potential subclasses.", OFFERING_CLASS);

            // Find all resources that have some type that might be a subclass or related
            // class
            NodeIterator subjects = model.listObjectsOfProperty(rdfType);
            while (subjects.hasNext()) {
                RDFNode node = subjects.next();
                if (node.isURIResource()) {
                    String uri = node.asResource().getURI();
                    if (uri.contains("Offering") || uri.endsWith("Offering")) {
                        logger.debug("Found potential offering class: {}", uri);

                        // Find instances of this class
                        StmtIterator instances = model.listStatements(null, rdfType, node);
                        while (instances.hasNext()) {
                            Resource instance = instances.next().getSubject();
                            logger.debug("Adding potential offering instance: {}", instance.getURI());
                            offerings.add(instance);
                        }
                    }
                }
            }
        }

        logger.info("Found {} offering resources in total", offerings.size());
        return offerings;
    }

    /**
     * Extract the complete subgraph for an offering with special attention to
     * prefixes
     */
    private Model extractOfferingSubgraph(Model sourceModel, Resource offering, Map<String, String> originalPrefixes) {
        // Create a fresh model for the subgraph
        Model result = ModelFactory.createDefaultModel();

        // Copy ONLY the prefixes from original model - don't add any manual ones
        // This ensures we preserve exactly the client's prefix naming conventions
        result.setNsPrefixes(originalPrefixes);

        // Log the prefixes we're copying
        if (logger.isDebugEnabled()) {
            logger.debug("Copying {} prefixes from original model to subgraph for {}",
                    originalPrefixes.size(), offering.getURI());
            originalPrefixes.forEach((prefix, uri) -> logger.debug("  Copying prefix: {} -> {}", prefix, uri));
        }

        // Use a recursive algorithm to extract all related statements
        Set<Resource> visited = new HashSet<>();
        extractRelatedStatements(sourceModel, result, offering, visited);

        // Ensure the offering has the correct type
        if (!result.contains(offering, RDF.type, result.createResource(OFFERING_CLASS))) {
            result.add(offering, RDF.type, result.createResource(OFFERING_CLASS));
        }

        // Log all prefixes that were copied to the subgraph
        logger.info("Subgraph for {} has {} prefixes:", offering.getURI(), result.getNsPrefixMap().size());
        if (logger.isDebugEnabled()) {
            result.getNsPrefixMap()
                    .forEach((prefix, uri) -> logger.debug("  Prefix in subgraph: {} -> {}", prefix, uri));
        }

        return result;
    }

    /**
     * Recursively extract all statements related to a resource.
     */
    private void extractRelatedStatements(Model source, Model target, Resource resource, Set<Resource> visited) {
        if (visited.contains(resource)) {
            return;
        }
        visited.add(resource);

        StmtIterator stmts = source.listStatements(resource, null, (RDFNode) null);
        while (stmts.hasNext()) {
            Statement stmt = stmts.next();
            target.add(stmt);

            RDFNode obj = stmt.getObject();
            if (obj.isResource() && !obj.isLiteral()) {
                Resource objRes = obj.asResource();
                if (!objRes.isAnon()) {
                    extractRelatedStatements(source, target, objRes, visited);
                }
            }
        }
    }

    /**
     * Store the extracted offering graphs in the dataset with enhanced prefix
     * handling
     */
    private void storeOfferingGraphs(List<NamedSubgraph> namedGraphs) throws IOException {
        dataset.begin(ReadWrite.WRITE);
        try {
            for (NamedSubgraph graph : namedGraphs) {
                String graphName = graph.getGraphName();
                Model modelToStore = graph.getModel();

                // Save prefixes for debugging
                Map<String, String> prefixesBeforeStorage = new HashMap<>(modelToStore.getNsPrefixMap());

                // Check if the graph already exists and remove it
                if (dataset.containsNamedModel(graphName)) {
                    logger.info("Replacing existing named graph: {}", graphName);
                    dataset.removeNamedModel(graphName);
                }

                // TDB2 sometimes discards prefixes, so let's work around that

                // 1. First, add the model to the dataset
                dataset.addNamedModel(graphName, modelToStore);

                // 2. Then, retrieve the model back and check if prefixes are preserved
                Model storedModel = dataset.getNamedModel(graphName);
                Map<String, String> storedPrefixes = storedModel.getNsPrefixMap();

                logger.info("Graph {} stored with {} statements. Prefixes before: {}, after: {}",
                        graphName, storedModel.size(), prefixesBeforeStorage.size(), storedPrefixes.size());

                // 3. If prefixes were lost, try to add them back explicitly
                if (storedPrefixes.size() < prefixesBeforeStorage.size()) {
                    logger.warn("Some prefixes were lost during storage. Attempting to restore them.");
                    storedModel.setNsPrefixes(prefixesBeforeStorage);

                    // This forces TDB2 to update the stored prefixes
                    dataset.replaceNamedModel(graphName, storedModel);

                    // Verify again
                    Model verifiedModel = dataset.getNamedModel(graphName);
                    logger.info("After prefix restoration attempt: {} prefixes",
                            verifiedModel.getNsPrefixMap().size());
                }
            }
            dataset.commit();
        } catch (Exception e) {
            dataset.abort();
            throw new IOException("Error storing named graphs", e);
        } finally {
            dataset.end();
        }
    }

    /**
     * Determine the output format based on Accept header
     */
    private Lang getOutputLang(String acceptHeader) {
        // If no Accept header is provided, default to JSON-LD
        if (acceptHeader == null || acceptHeader.isEmpty() || acceptHeader.contains("*/*")) {
            return Lang.JSONLD;
        }

        try {
            // Parse the Accept header
            ContentType ct = ContentType.create(acceptHeader);

            // Check specific content types
            if (ct.getContentTypeStr().contains("text/turtle")) {
                return Lang.TURTLE;
            } else if (ct.getContentTypeStr().contains("application/rdf+xml")) {
                return Lang.RDFXML;
            } else if (ct.getContentTypeStr().contains("application/n-triples")) {
                return Lang.NTRIPLES;
            } else if (ct.getContentTypeStr().contains("text/n3")) {
                return Lang.N3;
            } else if (ct.getContentTypeStr().contains("application/trig")) {
                return Lang.TRIG;
            }

            // Let Jena determine the language
            Lang lang = RDFLanguages.contentTypeToLang(ct.getContentTypeStr());
            if (lang != null) {
                return lang;
            }
        } catch (Exception e) {
            logger.warn("Error parsing Accept header '{}': {}", acceptHeader, e.getMessage());
        }

        // Default to JSON-LD if no valid format was specified or if there was an error
        return Lang.JSONLD;
    }

    /**
     * Read the entire request body as a byte array
     */
    private byte[] readRequestBody(HttpAction action) throws IOException {
        try (InputStream inputStream = action.getRequest().getInputStream()) {
            // Use IOUtils instead of readAllBytes which is Java 9+
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }
            return result.toByteArray();
        }
    }

    /**
     * Handle errors consistently
     */
    private void handleError(HttpAction action, Exception e, String message) {
        logger.error("{}: {}", message, e.getMessage(), e);
        try {
            // Always use a standard HTTP status code
            action.getResponse().setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); // This is 500
            action.getResponse().setContentType("application/json");
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Unknown error";
            String response = String.format(
                    "{ \"status\": \"error\", \"message\": \"%s: %s\" }",
                    message, errorMessage.replace("\"", "\\\"")); // Escape quotes
            action.getResponseOutputStream().write(response.getBytes());
        } catch (IOException ioe) {
            logger.error("Failed to send error response", ioe);
        }
    }

    /**
     * Class to represent a named graph with its model
     */
    private static class NamedSubgraph {
        private final String graphName;
        private final Model model;

        public NamedSubgraph(String graphName, Model model) {
            this.graphName = graphName;
            this.model = model;
        }

        public String getGraphName() {
            return graphName;
        }

        public Model getModel() {
            return model;
        }
    }
}