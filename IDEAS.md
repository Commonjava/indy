# Improvements


## Lower timeouts


## Disable implied repos for some groups

* we should whitelist which groups are able to contain implied repos

    * don't process for repos in poms on file storage unless entrypoint is whitelisted

* we should mark implied repos with metadata and enable filtering if a group doesn't allow them

    * NOTE: How to do this in a decoupled way??


## Use HTTP 100 Continue response to keep the connection live

* We could try it

    * If it doesn't help we'll have to use websocket or callback


