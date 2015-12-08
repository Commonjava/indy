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


test -f /etc/profile && source /etc/profile
test -f $HOME/.bash_profile &&source $HOME/.bash_profile

THIS=$(cd ${0%/*} && echo $PWD/${0##*/})
# THIS=`realpath ${0}`
BASEDIR=`dirname ${THIS}`
BASEDIR=`dirname ${BASEDIR}`

# echo "basedir: ${BASEDIR}"

APROX_LOCALLIB_DIR=${APROX_LOCALLIB_DIR:-${BASEDIR}/lib/local}
APROX_LOGCONF_DIR=${APROX_LOGCONF_DIR:-${BASEDIR}/etc/aprox/logging}

echo "Loading logging config from: ${APROX_LOGCONF_DIR}"

CP="${APROX_LOCALLIB_DIR}:${APROX_LOGCONF_DIR}"
for f in $(find $BASEDIR/lib/aprox-embedder-*.jar -type f)
do
  CP=${CP}:${f}
done

for f in $(find $BASEDIR/lib/thirdparty -type f)
do
  CP=${CP}:${f}
done

# echo "Classpath: ${CP}"

JAVA=`which java`
$JAVA -version 2>&1 > /dev/null
if [ $? != 0 ]; then
  PATH=${JAVA_HOME}/bin:${PATH}
  JAVA=${JAVA_HOME}/bin/java
fi

APROX_ENV=${APROX_ENV:-${BASEDIR}/etc/aprox/env.sh}
test -f ${APROX_ENV} && source ${APROX_ENV}

# echo "Command: '${JAVA} -cp ${CP} -Daprox.home=${BASEDIR} -Daprox.boot.defaults=${BASEDIR}/bin/boot.properties ${MAIN_CLASS} $@'"
# exec "$JAVA" ${JAVA_OPTS} -cp "${CP}" -Daprox.logging="${APROX_LOGCONF}" -Daprox.home="${BASEDIR}" -Daprox.boot.defaults=${BASEDIR}/bin/boot.properties ${MAIN_CLASS} "$@"

#JAVA_DEBUG_OPTS="-Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=n"
JAVA_OPTS="$JAVA_OPTS $JAVA_DEBUG_OPTS"

MAIN_CLASS=org.commonjava.indy.boot.jaxrs.JaxRsBooter

"$JAVA" ${JAVA_OPTS} -cp "${CP}" -Daprox.home="${BASEDIR}" -Daprox.boot.defaults=${BASEDIR}/bin/boot.properties ${MAIN_CLASS} "$@"
ret=$?
if [ $ret == 0 -o $ret == 130 ]; then
  exit 0
else
  exit $ret
fi
