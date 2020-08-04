#!/bin/bash

echo "Testing Springboot apps =========="
apps_ports=$(find /work/services/nepa-services\
                  /work/services/stub-srv/etc/dummy-portal-services \
                  -name application.properties \
                  -exec grep server.port {} \; | cut -d= -f2)

err=1
retries=1
max_retries=10
sleep_time=30
temp_file=/tmp/port_status
while [ $err -ne 0 ] && [ ! $retries -gt $max_retries ]
do
    echo "Trying ($retries/$max_retries)"
    echo "Sleeping ${sleep_time} secs ..." && sleep $sleep_time
    err=0
    rm -f $temp_file
    for i in $apps_ports
    do
        echo -n "Checking port $i ... "
        curl -s http://localhost:$i/healthcheck/quick | grep -sq '"appStatus":"OK"' \
            && echo PASS \
            && echo "port $i PASSED" >> $temp_file \
            && continue
        echo 'FAILED'
        err=1
        echo "port $i FAILED" >> $temp_file
    done
    ((++retries))
done

echo ""
echo "==============================================="
echo "Summary:"

if grep -sq 'FAILED' $temp_file; then
    echo "test java apps port availability NOTOK"
    cat $temp_file
else
    echo "test java apps port availability OK"
fi
