#!/bin/bash

THIS=$(cd ${0%/*} && echo $PWD/${0##*/})
# THIS=`realpath ${0}`
BASEDIR=`dirname ${THIS}`

docker run --rm -p 5432 -e POSTGRES_PASSWORD=test -e POSTGRES_USER=test --name postgres postgres:10 || exit -1

export TEST_ETC=$BASEDIR/indy-etc

$BASEDIR/../../../bin/test-setup.sh

echo "Postgres container is STILL RUNNING. When done testing, stop using the command:"
echo "docker stop postgres"
