#!/bin/bash
mkdir /zeppelin/logs
echo "Starting zeppelin: /zeppelin/bin/zeppelin.sh \"$@\""
/bin/mtail /zeppelin/logs & /zeppelin/bin/zeppelin.sh "$@"