#!/bin/bash
mkdir /zeppelin/logs
echo "Starting zeppelin: /zeppelin/bin/zeppelin-orig.sh \"$@\""
/bin/mtail /zeppelin/logs & /zeppelin/bin/zeppelin-orig.sh "$@"