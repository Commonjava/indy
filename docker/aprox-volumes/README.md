This Docker image provides storage for an AProx instance hosted in another Docker container. It's uses the CentOS base image, and simply runs a short python script that loops infinitely, to enable it to be daemonized. This image is designed to be used in conjunction with a `--volumes-from` clause in the launch command for the Docker container that actually runs the AProx server.

## Volumes

The following volumes can be exported to provide persistent storage for an instance of the `aprox-server` Docker image:

- `/var/lib/aprox/storage` - Cache storage for artifacts provided by AProx
- `/var/lib/aprox/data` - Stores definitions for repositories, along with definitional information that add-ons use (such as autoprox rules)
- `/var/log/aprox` - Logs will be stored here, obviously
- `/etc/aprox` - Configures paths and features/add-ons in the AProx deployment.

