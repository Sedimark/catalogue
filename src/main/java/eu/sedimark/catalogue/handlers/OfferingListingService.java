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

import org.apache.jena.query.Dataset;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.StmtIterator;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Custom servlet that lists all named graphs in the dataset
 * and specifically identifies those containing sedimark:Offering instances
 */
public class OfferingListingService extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final Dataset dataset;
    private final String offeringType;
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(OfferingListingService.class);

    // Add a formatter for ISO 8601 timestamps
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
            .withZone(ZoneOffset.UTC);

    private String getIsoTimestamp() {
        return ISO_FORMATTER.format(Instant.now());
    }

    public OfferingListingService(Dataset dataset, String offeringType) {
        this.dataset = dataset;
        this.offeringType = offeringType;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            dataset.begin(org.apache.jena.query.ReadWrite.READ);
            try {
                // Log information for debugging
                logger.info("GraphListingService: doGet called");
                logger.info("Dataset contains {} named graphs", getNamedGraphCount());

                // Initialize response
                resp.setContentType("application/json");
                PrintWriter out = resp.getWriter();

                // Get all named graph URIs containing sedimark:Offering instances
                List<String> offeringGraphs = findOfferingGraphs();
                logger.info("Found {} offering graphs", offeringGraphs.size());

                // Build detailed JSON response
                StringBuilder jsonBuilder = new StringBuilder();
                jsonBuilder.append("{\n");
                jsonBuilder.append("  \"status\": \"success\",\n");
                jsonBuilder.append("  \"message\": \"Retrieved offering graphs\",\n");
                jsonBuilder.append("  \"totalCount\": ").append(offeringGraphs.size()).append(",\n");
                jsonBuilder.append("  \"offerings\": [\n");

                for (int i = 0; i < offeringGraphs.size(); i++) {
                    String graphName = offeringGraphs.get(i);

                    // Get self-listing URI from the graph
                    String selfURI = getSelfListingURI(graphName);

                    // Get count of linked assets instead of statements
                    int assetCount = countLinkedAssets(graphName);

                    jsonBuilder.append("    {\n")
                            .append("      \"uri\": \"").append(escapeJsonString(graphName)).append("\",\n")
                            .append("      \"selfListing\": \"").append(escapeJsonString(selfURI)).append("\",\n")
                            .append("      \"assets\": ").append(assetCount).append("\n")
                            .append("    }");
                    if (i < offeringGraphs.size() - 1) {
                        jsonBuilder.append(",");
                    }
                    jsonBuilder.append("\n");
                }

                jsonBuilder.append("  ],\n");
                jsonBuilder.append("  \"timestamp\": \"").append(getIsoTimestamp()).append("\"\n");
                jsonBuilder.append("}");
                out.write(jsonBuilder.toString());
            } finally {
                dataset.end();
            }
        } catch (Exception e) {
            logger.error("Error in OfferingListingService.doGet: {}", e.getMessage(), e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"status\":\"error\",\"message\":\"Internal server error\"}");
        }
    }

    /**
     * Count the number of sedimark:Asset instances linked to the offering in a
     * graph
     * 
     * @param graphName The named graph URI
     * @return The number of linked assets
     */
    private int countLinkedAssets(String graphName) {
        try {
            Model model = dataset.getNamedModel(graphName);

            // First, find the offering subject
            Resource subject = null;
            Property rdfType = ResourceFactory.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
            Resource offeringClass = ResourceFactory.createResource(offeringType);

            StmtIterator stmts = model.listStatements(null, rdfType, offeringClass);
            if (stmts.hasNext()) {
                subject = stmts.next().getSubject();

                // Find assets linked to this offering
                // This counts assets two ways:
                // 1. Assets directly linked via sedimark:offers
                // 2. Any resource of type sedimark:Asset in the graph

                int count = 0;

                // 1. Check for directly linked assets
                Property offersProperty = ResourceFactory.createProperty("https://w3id.org/sedimark/ontology#offers");
                StmtIterator assetStmts = model.listStatements(subject, offersProperty, (RDFNode) null);
                while (assetStmts.hasNext()) {
                    count++;
                    assetStmts.next();
                }

                // If no direct links found, count any asset in the graph
                if (count == 0) {
                    Resource assetClass = ResourceFactory.createProperty("https://w3id.org/sedimark/ontology#Asset");
                    StmtIterator assetInstances = model.listStatements(null, rdfType, assetClass);
                    while (assetInstances.hasNext()) {
                        count++;
                        assetInstances.next();
                    }
                }

                return count;
            }

            return 0; // No offering found
        } catch (Exception e) {
            logger.warn("Error counting assets for graph {}: {}", graphName, e.getMessage());
            return 0;
        }
    }

    /**
     * Get the self-listing URI for an offering from its graph
     * 
     * @param graphName The named graph URI
     * @return The offering's self-listing URI, or the graph URI if not found
     */
    private String getSelfListingURI(String graphName) {
        try {
            Model model = dataset.getNamedModel(graphName);

            // First, find the offering subject
            Resource subject = null;
            Property rdfType = ResourceFactory.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
            Resource offeringClass = ResourceFactory.createResource(offeringType);

            StmtIterator stmts = model.listStatements(null, rdfType, offeringClass);
            if (stmts.hasNext()) {
                subject = stmts.next().getSubject();

                // Look for a linked resource of type sedimark:Self-Listing
                Property hasSelfListing = ResourceFactory
                        .createProperty("https://w3id.org/sedimark/ontology#hasSelfListing");
                StmtIterator selfListingStmts = model.listStatements(subject, hasSelfListing, (RDFNode) null);

                if (selfListingStmts.hasNext()) {
                    RDFNode selfListingNode = selfListingStmts.next().getObject();
                    if (selfListingNode.isURIResource()) {
                        return selfListingNode.asResource().getURI();
                    }
                }

                // If no direct link found, look for any resource of type sedimark:Self-Listing
                // in the graph
                Resource selfListingType = ResourceFactory
                        .createResource("https://w3id.org/sedimark/ontology#Self-Listing");
                StmtIterator selfListingInstanceStmts = model.listStatements(null, rdfType, selfListingType);

                if (selfListingInstanceStmts.hasNext()) {
                    Resource selfListingResource = selfListingInstanceStmts.next().getSubject();
                    return selfListingResource.getURI();
                }

                // If no self-listing found, return the subject URI
                return subject.getURI();
            }

            // If offering not found, return the graph name as fallback
            return graphName;
        } catch (Exception e) {
            logger.warn("Error getting self-listing URI for graph {}: {}", graphName, e.getMessage());
            return graphName;
        }
    }

    /**
     * Get the number of named graphs in the dataset
     */
    private int getNamedGraphCount() {
        List<String> names = new ArrayList<>();
        dataset.listNames().forEachRemaining(names::add);
        return names.size();
    }

    /**
     * Count statements in a specific graph
     */
    private int countStatementsInGraph(String graphName) {
        try {
            return (int) dataset.getNamedModel(graphName).size();
        } catch (Exception e) {
            logger.warn("Error counting statements in graph {}: {}", graphName, e.getMessage());
            return -1;
        }
    }

    /**
     * Find named graphs that contain sedimark:Offering instances
     */
    private List<String> findOfferingGraphs() {
        List<String> result = new ArrayList<>();

        // First approach: use the graph names directly (since we use offering URIs as
        // graph names)
        dataset.listNames().forEachRemaining(result::add);

        // Second approach: query for graphs containing sedimark:Offering instances
        if (result.isEmpty()) {
            String sparqlQuery = "SELECT DISTINCT ?g WHERE {\n" +
                    "  GRAPH ?g {\n" +
                    "    ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <" + offeringType + ">\n" +
                    "  }\n" +
                    "}";

            try (QueryExecution qexec = QueryExecutionFactory.create(sparqlQuery, dataset)) {
                ResultSet rs = qexec.execSelect();
                while (rs.hasNext()) {
                    QuerySolution sol = rs.next();
                    result.add(sol.get("g").toString());
                }
            }
        }

        return result;
    }

    /**
     * Escape special characters in JSON strings
     */
    private String escapeJsonString(String input) {
        if (input == null) {
            return "";
        }

        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}