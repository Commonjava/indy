# AProx Keycloak Security HOW-TO

## Local / Test Installation

0. add aprox.local and keycloak.local as aliases for 127.0.0.1 in /etc/hosts
1. Install Keycloak (docker run -ti --net=host -p 8080:8080 -p 9090:9090 --name=keycloak jboss/keycloak)
2. Setup Keycloak server
    1. change admin password
    2. add 'aprox' realm
        - copy public key to keycloak.json and keycloak-ui.json
    3. add 'aprox' client with confidential / bearer access
        - copy the credential secret to keycloak.json
    4. add 'aprox-ui' client with public-only access
    5. create some users
    6. configure keycloak.conf
        - url=http://keycloak.local:8080/auth/
        - enabled=true
3. Enjoy your secured UI

## Notes

1. This has only been tested in the 'savant' AProx flavor. For now, YMMV when using min and easyprox flavors.
2. Raw REST access and Java client API are currently not compatible with Keycloak security. This is in progress.
