APP_HOME="`dirname $0`/../"

if [ -f "$APP_HOME"/event-listener.pid ]; then
  PID=`cat $APP_HOME/event-listener.pid`
  echo "Sending interrupt ($PID)"
  kill -HUP $PID 2>&1 > /dev/null
  echo "Use ps to check event listener stopped and check end of log file"
fi