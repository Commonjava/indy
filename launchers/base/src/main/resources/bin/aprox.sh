#!/bin/sh

THIS=$(cd ${0%/*} && echo $PWD/${0##*/})
# THIS=`realpath ${0}`
BASEDIR=`dirname ${THIS}`
BASEDIR=`dirname ${BASEDIR}`

# echo "basedir: ${BASEDIR}"

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
if [ x${JAVA_HOME} != 'x' ]; then
  PATH=${JAVA_HOME}/bin:${PATH}
  JAVA=$JAVA_HOME/bin/java
fi

test -f ${BASEDIR}/etc/aprox/env.sh && source ${BASEDIR}/etc/aprox/env.sh

# echo "Command: '${JAVA} -cp ${CP} -Daprox.home=${BASEDIR} ${MAIN_CLASS} -C ${BASEDIR}/etc/main.conf $@'"
exec "$JAVA" -cp "${CP}" -Daprox.home="${BASEDIR}" ${MAIN_CLASS} -c "${BASEDIR}/etc/aprox/main.conf" "$@"

