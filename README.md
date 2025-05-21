# SEDIMARK Catalogue Server (Fuseki)

This project implements a customized Jena Fuseki server that manages SEDIMARK offerings in a semantic catalogue. The server provides specialized handlers for storing, retrieving, and managing offerings according to the SEDIMARK ontology.

## Project Structure

```
catalogue
├── src
│   └── main
│       ├── java
│       │   └── eu
│       │       └── sedimark
│       │           └── catalogue
│       │               ├── CatalogueServerLauncher.java    # Main server setup
│       │               ├── handlers
│       │               │   ├── OfferingGSPHandler.java     # Custom handler for offerings
│       │               │   └── TestHandler.java            # Test endpoint handler
│       │               ├── services
│       │               │   └── OfferingListingService.java    # Service to list all Offerings
│       │               ├── loaders
│       │               │   └── SampleDatasetLoader.java    # Loader for example data
│       │               ├── debug
│       │               │   └── FusekiDebugHelper.java      # Debug utilities
│       │               └── utils
│       │                   └── ArgumentsHelper.java        # Command-line argument utilities
│       └── resources
│           ├── log4j2.properties                # Logging configuration
│           ├── ontology
│           │   └── sedimark-ontology.ttl        # SEDIMARK ontology definition
│           ├── shacl
│           │   └── shapes.ttl                   # SHACL shapes for validation
│           └── examples
│               └── offerings_1.jsonld           # Example offering data
├── pom.xml
└── README.md
```

## Prerequisites

- Java Development Kit (JDK) 21
- Apache Maven 3.6 or higher
- Understanding of RDF, JSON-LD, and the SEDIMARK ontology

## Setup Instructions

1. **Clone the Repository**
   ```bash
   git clone <repository-url>
   cd sedimark-catalogue-server
   ```

2. **Build the Project**
   Use Maven to build the project:
   ```bash
   mvn clean package
   ```

3. **Run the Fuseki Server**
   Execute the main class with options for storage type and other settings:
   
   In-memory storage (default):
   ```bash
   java -cp target/catalogue-1.0-jar-with-dependencies.jar
   ```
   
   Persistent TDB storage:
   ```bash
   java -cp target/catalogue-1.0-jar-with-dependencies.jar --tdb
   ```
   
   With example data:
   ```bash
   java -cp target/catalogue-1.0-jar-with-dependencies.jar --load-examples
   ```

4. **Command Line Options**
   ```
   --memory         Use in-memory storage (default)
   --tdb [path]     Use TDB2 persistent storage at the specified path
                    Default: ./sedimark-tdb (relative to JAR location)
   --port <number>  Specify the server port (default: 3030)
   --load-examples  Load example offerings (disabled by default)
   --debug          Enable debug mode with additional logging
   --help           Show this help message
   ```

5. **Access the Server**
   The server will start on port 3030 by default:
   ```
   http://localhost:3030/
   ```

## SEDIMARK Catalogue Interfaces

The server exposes the following endpoints for interacting with the SEDIMARK catalogue:

### 1. Offering Publication Endpoint
- **URL:** `http://localhost:3030/catalogue/manager`
- **Methods:** POST, PUT
- **Description:** Publish new offerings or update existing ones
- **Content Types:** application/ld+json (JSON-LD)
- **Example:**
  ```bash
  curl -H "Content-Type: application/ld+json" -X POST --data @offering.jsonld http://localhost:3030/catalogue/manager
  ```
- **Response:** JSON confirmation with count of stored offerings and their URIs
  ```json
  {
    "status": "success",
    "message": "Offerings stored successfully",
    "storedOfferings": [
      {
        "uri": "http://example.org/offering_1",
        "statements": 125,
        "prefixes": 5
      }
    ],
    "totalOfferings": 1
  }
  ```

### 2. Offering Retrieval Endpoint
- **URL:** `http://localhost:3030/catalogue/manager?graph=<offering-uri>`
- **Method:** GET
- **Description:** Retrieve a specific offering by URI
- **Accept Headers:** application/ld+json, text/turtle, application/rdf+xml
- **Example:**
  ```bash
  curl -H "Accept: application/ld+json" http://localhost:3030/catalogue/manager?graph=http://example.org/offering_1
  ```
- **Response:** The offering data in the requested format
- **Metadata Option:** Add `metadata=true` to retrieve information about the graph instead of the actual RDF data:
  ```bash
  curl http://localhost:3030/catalogue/manager?graph=http://example.org/offering_1&metadata=true
  ```

### 3. Offering Deletion Endpoint
- **URL:** `http://localhost:3030/catalogue/manager?graph=<offering-uri>`
- **Method:** DELETE
- **Description:** Remove an offering from the catalogue
- **Example:**
  ```bash
  curl -X DELETE http://localhost:3030/catalogue/manager?graph=http://example.org/offering_1
  ```
- **Response:** JSON confirmation of deletion
  ```json
  {
    "status": "success",
    "message": "Offering graph deleted successfully",
    "graphUri": "http://example.org/offering_1",
    "statementsRemoved": 125,
    "timestamp": "2023-05-20T14:30:15Z"
  }
  ```

### 4. Graph Listing Service
- **URL:** `http://localhost:3030/catalogue/graphs`
- **Method:** GET
- **Description:** List all offerings in the catalogue
- **Example:**
  ```bash
  curl http://localhost:3030/catalogue/graphs
  ```
- **Response:** JSON listing of all offering graphs
  ```json
  {
    "status": "success",
    "message": "Retrieved offering graphs",
    "totalCount": 2,
    "offerings": [
      {
        "uri": "http://example.org/offering_1",
        "selfListing": "http://example.org/catalogue/listing_1",
        "assets": 3
      },
      {
        "uri": "http://example.org/offering_2",
        "selfListing": "http://example.org/catalogue/listing_2",
        "assets": 1
      }
    ],
    "timestamp": "2023-05-20T14:30:15Z"
  }
  ```

### 5. Standard SPARQL Endpoints
- **Query Endpoint:** `http://localhost:3030/catalogue/sparql`
- **Update Endpoint:** `http://localhost:3030/catalogue/update`
- **Description:** Standard SPARQL 1.1 Protocol endpoints for querying and updating data
- **Example Query:**
  ```bash
  curl -H "Accept: application/json" --data-urlencode "query=SELECT * WHERE { ?s a <https://w3id.org/sedimark/ontology#Offering> }" http://localhost:3030/catalogue/sparql
  ```
- **Content Type for SPARQL Queries:** When sending queries via POST, use `Content-Type: application/sparql-query`

### 6. Test Endpoint
- **URL:** `http://localhost:3030/catalogue/test`
- **Method:** GET
- **Description:** Simple test endpoint to verify server functionality
- **Example:**
  ```bash
  curl http://localhost:3030/catalogue/test
  ```

## Storage Options

### In-Memory Storage
- Default mode if no storage option is specified
- Data is lost when the server is restarted
- Suitable for development and testing

### TDB2 Persistent Storage
- Data is stored persistently on disk
- Available by using the `--tdb` option
- Automatically creates the storage directory if it doesn't exist
- Default location is `./sedimark-tdb` relative to the JAR file location
- Custom location can be specified: `--tdb /path/to/storage`

## Key Features

- **Named Graph Storage:** Each offering is stored in its own named graph with the offering URI as the graph name
- **JSON-LD Support:** Full support for JSON-LD formatted offerings with prefix preservation
- **Offering Extraction:** Extracts offerings from incoming data by identifying resources of type sedimark:Offering
- **Custom Headers:** Responses include an X-Handler header indicating which handler processed the request
- **Detailed Logging:** Comprehensive logging of request handling with configurable verbosity
- **Persistent Storage:** Optional TDB2-based persistent storage
- **Command-line Options:** Flexible configuration via command-line arguments
- **Content Negotiation:** Support for different RDF serialization formats based on Accept headers
- **Standardized JSON Responses:** Consistent JSON response format for all operations with timestamp in ISO 8601 format

## Ontology Integration

The server is built around the SEDIMARK ontology, specifically focusing on:

- **sedimark:Offering** - The core concept representing Offerings
- **sedimark:Asset** - Assets linked to Offerings via `sedimark:hasAsset` property
- **sedimark:Self-Listing** - Self-Listings that provide information about Offerings for a Participant.

## Configuration

Logging can be configured in the `src/main/resources/log4j2.properties` file. The default configuration provides detailed logs for the SEDIMARK handler operations.

## Dependencies

This project uses the following dependencies:
- Apache Jena Fuseki version 5.4.0
- Apache Jena Core version 5.4.0
- Apache Jena ARQ version 5.4.0
- Apache Jena TDB2 version 5.4.0
- Log4j version 2.24.3
- SLF4J version 2.0.17
- Java Servlet API version 4.0.1

## License

This software is licensed under the [European Union Public License 1.2](https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12) (EUPL-1.2).

This updated README reflects the new project structure, command-line options, storage choices, and includes information about the utility classes and debugging features.