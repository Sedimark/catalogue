# Dockerfile for SEDIMARK Catalogue Server

## Usage Instructions

### Building the Docker Image

From the project root directory:

```bash
# Using -f to specify Dockerfile in subdirectory
docker build -f docker/Dockerfile -t sedimark/catalogue-server:1.0 .
```

Alternative approaches:

```bash
# Change to the docker directory first
cd docker
docker build -t sedimark/catalogue-server:1.0 ..
```

Or using Docker Compose (if you have a docker-compose.yml file):

```bash
docker-compose build
```

### Running the Container

Basic usage with default settings (in-memory storage):

```bash
docker run -d -p 3030:3030 --name sedimark-catalogue sedimark/catalogue-server:1.0
```

With TDB2 persistent storage:

```bash
docker run -d -p 3030:3030 -v ~/sedimark-data:/data -e STORAGE_TYPE=tdb --name sedimark-catalogue sedimark/catalogue-server:1.0
```

Loading example data:

```bash
docker run -d -p 3030:3030 -e LOAD_EXAMPLES=true --name sedimark-catalogue sedimark/catalogue-server:1.0
```

With debug mode enabled:

```bash
docker run -d -p 3030:3030 -e DEBUG=true --name sedimark-catalogue sedimark/catalogue-server:1.0
```

Custom port mapping:

```bash
docker run -d -p 8080:3030 -e SERVER_PORT=3030 --name sedimark-catalogue sedimark/catalogue-server:1.0
```

### Environment Variables

| Variable      | Default  | Description                                       |
|---------------|----------|---------------------------------------------------|
| SERVER_PORT   | 3030     | Port the server listens on inside the container   |
| STORAGE_TYPE  | memory   | Storage type: "memory" or "tdb"                   |
| TDB_PATH      | /data/sedimark-tdb | Path for TDB2 storage                   |
| LOAD_EXAMPLES | false    | Whether to load example data on startup           |
| DEBUG         | false    | Enable debug mode                                 |

### Persisting Data

When using TDB2 storage, you should mount a volume to persist the data:

```bash
docker run -d -p 3030:3030 -v ~/sedimark-data:/data -e STORAGE_TYPE=tdb --name sedimark-catalogue sedimark/catalogue-server:1.0
```

This maps the host directory `~/sedimark-data` to `/data` in the container, where the TDB2 storage is located.

### Checking Container Health

The Dockerfile includes a healthcheck that verifies the server is running:

```bash
docker inspect --format "{{.State.Health.Status}}" sedimark-catalogue
```

### Stopping the Container

```bash
docker stop sedimark-catalogue
```

### Viewing Logs

```bash
docker logs sedimark-catalogue
```

Following logs in real-time:

```bash
docker logs -f sedimark-catalogue
```

This Dockerfile and usage instructions provide a complete containerization solution for the SEDIMARK Catalogue Server with all configuration options accessible as environment variables.
