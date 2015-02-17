#!/bin/bash

rm -rf /tmp/junit*
rm -rf build*.log

for i in {0..39}; do
  mvn $* -Prun-its clean install 2>&1 | tee build-$i.log
  ret=$?
  echo "Returned: $ret"
  if [ $ret != 0 ]; then
    echo "Build failed. Exiting burn-in"
    exit 1
  fi

  failed=$(grep -l 'BUILD FAIL' build-$i.log)
  if [ "x$failed" != "x" ]; then
    echo "Encountered failure(s):\n\n$failed\n\nExiting burn-in"
    exit 2
  fi

#  error=$(grep -l 'ERROR' build-$i.log)
#  if [ "x$error" != "x" ]; then
#    echo "Encountered error(s):\n\n$error\n\nExiting burn-in"
#    exit 3
#  fi
done
