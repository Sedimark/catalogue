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

package eu.sedimark.catalogue;

import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.fuseki.system.FusekiLogging;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.tdb2.TDB2Factory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;

import eu.sedimark.catalogue.handlers.OfferingListingService;
import eu.sedimark.catalogue.handlers.OfferingGSPHandler;
import eu.sedimark.catalogue.handlers.TestHandler;
import eu.sedimark.catalogue.loaders.SampleDatasetLoader;
import eu.sedimark.catalogue.utils.ArgumentsHelper;
import eu.sedimark.catalogue.utils.ArgumentsHelper.Arguments;
import eu.sedimark.catalogue.utils.FusekiDebugHelper;

public class CatalogueServerLauncher {
    private static final String SEDIMARK_OFFERING = "https://w3id.org/sedimark/ontology#Offering";

    public static void main(String[] args) {
        // Parse command line arguments
        Arguments arguments = ArgumentsHelper.parseArguments(args);
        
        // Initialize Fuseki logging
        FusekiLogging.setLogging();

        // Create dataset based on storage type
        Dataset dataset = createDataset(arguments);

        // Register JSON-LD format if not already registered
        if (!RDFLanguages.isRegistered(Lang.JSONLD)) {
            RDFLanguages.register(Lang.JSONLD);
        }

        // Load example data if requested
        if (arguments.loadExampleData) {
            try {
                System.out.println("Loading example offerings...");
                SampleDatasetLoader.loadExampleOffering(dataset);
                SampleDatasetLoader.addTestOffering(dataset);
            } catch (Exception e) {
                System.err.println("Warning: Could not load example offerings. Continuing with empty dataset.");
                e.printStackTrace();
            }
        }

        // Create handlers
        // TestHandler testHandler = new TestHandler();
        OfferingGSPHandler offeringHandler = new OfferingGSPHandler(dataset);
        OfferingListingService graphListingService = new OfferingListingService(dataset, SEDIMARK_OFFERING);

        // Create and start Fuseki server with custom GSP handler
        FusekiServer server = FusekiServer.create()
                .port(arguments.port)
                .add("/catalogue", dataset) // Mount dataset at /catalogue endpoint
                .addProcessor("/catalogue/manager", offeringHandler) // Use custom handler for GSP
                // .addProcessor("/catalogue/test", testHandler) // Test handler on a different endpoint
                .addServlet("/catalogue/graphs", graphListingService) // graph listing service
                .build();

        // Use debug helper if requested
        if (arguments.debug) {
            FusekiDebugHelper.registerHandlersWithDispatcher(dataset);
            FusekiDebugHelper.printProcessorInfo(server);
        }

        server.start();

        // Print server information
        ArgumentsHelper.printServerInformation(server, arguments);

        // Keep the server running
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            System.err.println("Server interrupted: " + e.getMessage());
            server.stop();
        }
    }

    /**
     * Create a dataset based on the specified storage type
     */
    private static Dataset createDataset(Arguments arguments) {
        switch (arguments.storageType) {
            case TDB:
                System.out.println("Using TDB2 persistent storage at: " + arguments.tdbLocation);
                ArgumentsHelper.ensureDirectoryExists(arguments.tdbLocation);
                return TDB2Factory.connectDataset(arguments.tdbLocation);
            case MEMORY:
            default:
                System.out.println("Using in-memory storage");
                Dataset dataset = DatasetFactory.createTxnMem();
                // Create an empty model in the default graph
                Model model = ModelFactory.createDefaultModel();
                dataset.setDefaultModel(model);
                return dataset;
        }
    }
}