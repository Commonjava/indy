#!/bin/bash

export JAVA_DEBUG=server

LAUNCHER=$1
echo "Launching: $LAUNCHER"

if [ "x${LAUNCHER}" == "x" ]; then
  echo "Usage $0 <launcher-name> (example: $0 savant)"
  echo ""
  exit 127
fi

cd launchers/$LAUNCHER/target
rm -rf aprox-launcher-$LAUNCHER
tar -zxvf aprox-launcher-$LAUNCHER-*-launcher.tar.gz
cd aprox-launcher-$LAUNCHER
exec ./bin/aprox.sh 2>&1 | tee launcher.log

