This is a Docker image configuration based on CentOS for the easyprox [AProx](https://github.com/jdcasey/aprox) Vert.x launcher variant, which consists of the core repository manager + core REST endpoints + a basic JavaScript UI (aprox minimal flavor), along with the [autoprox](https://github.com/jdcasey/aprox/wiki/AddonAutoprox) + [dotMaven](https://github.com/jdcasey/aprox/wiki/AddonDotMaven) add-ons.

It installs OpenJDK 1.7 and `which` via `yum`. Then, it downloads `aprox-launcher-easyprox` from the central Maven repository, unpacks it, and launches it.

The service listens on port `8080` (which is exposed). To access it from outside your localhost after launching, you may need to insert an iptables rule exposing port 8080 and forwarding it to `docker0`. A more general (if less safe) way to do this is to add the following two rules:

    -A FORWARD -i eth0 -o docker0 -j ACCEPT
    -A FORWARD -i docker0 -o eth0 -j ACCEPT

