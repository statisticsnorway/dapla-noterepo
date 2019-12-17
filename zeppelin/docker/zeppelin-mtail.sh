#!/bin/bash
mkdir /zeppelin/logs
/bin/mtail /zeppelin/logs & /zeppelin/bin/zeppelin.sh "$@"