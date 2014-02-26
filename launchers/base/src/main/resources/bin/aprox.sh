#!/bin/bash -l

THIS=$(cd ${0%/*} && echo $PWD/${0##*/})
# THIS=`realpath ${0}`
BASEDIR=`dirname ${THIS}`
BASEDIR=`dirname ${BASEDIR}`

# echo "basedir: ${BASEDIR}"

APROX_LOGCONF=${APROX_LOGCONF:-${BASEDIR}/etc/aprox/log4j.properties}

CP=""
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

test -f ${BASEDIR}/etc/aprox/env.sh && source ${BASEDIR}/etc/aprox/env.sh

# echo "Command: '${JAVA} -cp ${CP} -Daprox.home=${BASEDIR} -Daprox.boot.defaults=${BASEDIR}/bin/boot.properties ${MAIN_CLASS} $@'"
exec "$JAVA" ${JAVA_OPTS} -cp "${CP}" -Daprox.logging="${APROX_LOGCONF}" -Daprox.home="${BASEDIR}" -Daprox.boot.defaults=${BASEDIR}/bin/boot.properties ${MAIN_CLASS} "$@"

