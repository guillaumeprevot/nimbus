#!/bin/bash

# Folder containing "nimbus" and "nimbus-java-api" subfolders
export GIT_FOLDER=/path/to/git/folder

# Default options and optional "nimbus.conf" and "nimbus.log" configuration
OPTIONS="-Djdk.tls.ephemeralDHKeySize=2048 -Djdk.tls.rejectClientInitiatedRenegotiation=true"
#OPTIONS="$OPTIONS -Dnimbus.conf=/path/to/custom/nimbus.conf"
#OPTIONS="$OPTIONS -Dnimbus.log=/path/to/custom/nimbus.log"

# Update PATH if needed
# export PATH=/path/to/openjdk-17/bin:$PATH
# export PATH=/path/to/maven/bin:$PATH

# Command selection
echo "Command ? [start, stop, update]"
read cmd

case $cmd in
    start )
        cd $GIT_FOLDER/nimbus
        export CLASSPATH=./bin:./lib/*:./lib/image4j/*:./lib/jaudiotagger/*:./lib/javazoom/*:./lib/jave/*
        java $OPTIONS fr.techgp.nimbus.Application &
        ;;
    stop )
        kill $(ps aux | grep '[n]imbus' | awk '{print $2}');
        ;;
    update )
        cd $GIT_FOLDER/nimbus-java-api
        git pull
        mvn install
        cd $GIT_FOLDER/nimbus
        if [ -d "./bin" ];then
            rm -rf ./bin/*
        else
            mkdir ./bin
        fi
        rm ./lib/*.jar
        git pull
        mvn install
        ;;
esac
