# Indy Keycloak Security HOW-TO

## Local / Test Installation

0. add indy.local and keycloak.local as aliases for 127.0.0.1 in /etc/hosts
1. Install Keycloak
    1. `docker run -ti --net=host -p 8080:8080 -p 9090:9090 --name=keycloak jboss/keycloak`
2. Setup Keycloak server
    1. change admin password
    2. add realm `indy`
    3. add realm client `indy`
        - access type: `confidential`
    4. add realm client `indy-ui`
        - access type: `public`
        - add web origin: `http://indy.local:8081`
        - add valid redirect uri: `http://indy.local:8081/index.html`
    5. create some users or enable some identity providers
3. configure keycloak.conf
    - url=http://keycloak.local:8080/auth/
    - enabled=true
    - realm.public.key=[Public Key field from Keycloak realm > Keys]
    - server.credential.secret=[Secret field from Keycloak realm > Clients > indy > Credentials]
4. Enjoy your secured Indy instance!

## Notes

1. This has only been tested in the 'savant' Indy flavor. For now, YMMV when using min and easyprox flavors.
2. Java client API is not yet compatible with Keycloak security. Work is in progress on this feature.
