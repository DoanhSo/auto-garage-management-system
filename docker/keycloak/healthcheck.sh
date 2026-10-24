#!/bin/bash
set -eu
exec 3<>/dev/tcp/127.0.0.1/9000
printf 'HEAD /health/ready HTTP/1.0\r\nHost: localhost\r\nConnection: close\r\n\r\n' >&3
IFS= read -r status <&3
[[ "$status" == *' 200 '* ]]
