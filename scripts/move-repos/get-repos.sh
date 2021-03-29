#!/bin/bash

mkdir -p output

#indy=http://indy-admin-stage.psi.redhat.com
indy=$1
api=$indy/api/admin/stores/_all

prefix="http://"
suffix="/"
host=${indy#"$prefix"}
host=${host%"$suffix"}

curl "$api/group" --output output/$host-group.json
curl "$api/remote" --output output/$host-remote.json
curl "$api/hosted" --output output/$host-hosted.json
