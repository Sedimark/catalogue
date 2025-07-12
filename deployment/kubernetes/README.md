## Deploying the SEDIMARK Catalogue Server in Kubernetes

The manifests provided in the `deployment/kubernetes` directory allows a Sedimark catalogue server instance in a Kubernetes cluster, with a volume claim to persist the data.

First, set the necessary variables in your environment, for example:

```bash
export STORAGE_CLASS="nfs-storageclass" # the StorageClass used in your cluster
export CATALOGUE_APP_NAME="jena-fuseki" # name of the application
export CATALOGUE_NAMESPACE="catalogue" # namespace where the application will be deployed
export CATALOGUE_DOCKER_REGISTRY="stain/jena-fuseki" # the Docker registry where the image is stored
export CATALOGUE_IMAGETAG="latest" # the tag of the image
export CATALOGUE_ADMIN_PASSWORD="your base64 encoded password" # the password for the admin user
```

Then, deploy the application:

```bash
cat ./deployment/kubernetes/*.yaml | envsubst | kubectl apply -f -
```

The application doesn't have an ingress yet, so to access it, you can use port-forwarding:

```bash
kubectl port-forward svc/jena-fuseki 3030:3030 -n catalogue
```

Then, you can access the application at `http://localhost:3030/`.

