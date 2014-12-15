This is a Docker image that runs an AProx server. It uses a CentOS base image, and by default will run the most recent release of the Savant launcher, though this is customizable via CLI environment variables.

This image is designed to work with the `aprox-volumes` Docker image, which provides storage of AProx caches, configuration, and logs.

## Volume Mounts

The following volumes can be mounted from outside (like from the aprox-volumes image) to provide persistent storage:

- `/var/lib/aprox/storage` - Cache storage for artifacts provided by AProx
- `/var/lib/aprox/data` - Stores definitions for repositories, along with definitional information that add-ons use (such as autoprox rules)
- `/var/log/aprox` - Logs will be stored here, obviously
- `/etc/aprox` - Configures paths and features/add-ons in the AProx deployment.

## Environment Variables

This AProx Docker image can be customized from the command line using the following environment variables:

- `APROX_BINARY_URL`

  In case you want to deploy a different version of AProx, or a different deployment flavor, you can specify the full URL of the deployable binary (tar.gz) via this envar. If this envar is unspecified, the latest release of the Savant deployment flavor will be used.

- `APROX_ETC_URL`

  In case you want to store the path / feature configuration options for your deployment in a Git repository, the startup script will use git to clone the repository at the URL provided in this envar (if it's specified) into `/etc/aprox` before starting the server. If unspecified, the default `etc/aprox` configuration from the deployment binary will be used.

