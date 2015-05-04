#!/bin/bash
#
# Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#         http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#


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
