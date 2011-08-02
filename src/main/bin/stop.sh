APP_HOME="`dirname $0`/../"

if [ -f "$APP_HOME"/cycle-detection.pid ]; then
  PID=`cat $APP_HOME/cycle-detection.pid`
  echo "Sending interrupt ($PID)"
  kill -HUP $PID 2>&1 > /dev/null
  echo "Use ps to check cycle-detection stopped and check end of log file"
fi