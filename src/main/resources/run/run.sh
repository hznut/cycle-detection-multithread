source `dirname $0`/env.sh

echo "JAVA_HOME=$JAVA_HOME"
if [ -z $JAVA_HOME ]; then
  echo "Please set JAVA_HOME"
  exit -1
fi

JCONSOLE_OPTS=" -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.port=2251 -Djava.rmi.server.hostname=`hostname`"
JVM_OPTS="-Xms256m -Xmx512m"
#DEBUGGING_OPTS="-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,address=3408,suspend=y"

JAVA_ARGS="$JCONSOLE_OPTS $JVM_OPTS"
if [ "$1" == "-d" ]; then
  JAVA_ARGS="$JAVA_ARGS $DEBUGGING_OPTS"
fi

$JAVA_HOME/bin/java -cp "$CP" $JAVA_ARGS $MAIN_CLASS $@ > $APP_HOME/console.log 2>&1 &
echo $! >event-listener.pid
