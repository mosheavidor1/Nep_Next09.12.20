#!/bin/bash

echo "Tesing Springboot apps =========="
apps_ports=$(find /work/services/nepa-services /work/services/stub-srv/etc/dummy-portal-services -name application.properties -exec grep server.port {} \; | cut -d= -f2)
err=1
retries=1
max_retries=10
while [ $err -ne 0 ] && [ ! $retries -gt $max_retries ]
do
    echo "Trying ($retries/$max_retries)"
    echo "Sleeping 30 secs ..." && sleep 30
    err=0
    for i in $apps_ports 123
    do
        echo -n "Checking port $i ... "
        curl -s http://localhost:$i/healthcheck/quick | grep -sq '"appStatus":"OK"' && echo PASS && continue
        echo FAILED
        err=1
    done
    ((++retries))
done
