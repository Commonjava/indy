#!/bin/bash

test -f /etc/profile && source /etc/profile
test -f $HOME/.bash_profile &&source $HOME/.bash_profile

THIS=$(cd ${0%/*} && echo $PWD/${0##*/})
# THIS=`realpath ${0}`
BASEDIR=`dirname ${THIS}`
BASEDIR=`dirname ${BASEDIR}`

# echo "basedir: ${BASEDIR}"

APROX_LOGCONF_DIR=${APROX_LOGCONF_DIR:-${BASEDIR}/etc/aprox/logging}

echo "Loading logging config from: ${APROX_LOGCONF_DIR}"

CP="${APROX_LOGCONF_DIR}"
for f in $(find $BASEDIR/lib/aprox-cdi-components-*.jar -type f)
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

exec "$JAVA" ${JAVA_OPTS} -cp "${CP}" -Daprox.home="${BASEDIR}" -Daprox.boot.defaults=${BASEDIR}/bin/boot.properties ${MAIN_CLASS} "$@"

