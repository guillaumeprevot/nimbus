#!/bin/bash

######################
#### CUSTOMIZABLE ####
######################

export CONF=nimbus.conf
export FOLDER=`pwd`
export OPTIONS=-Djdk.tls.ephemeralDHKeySize=2048 -Djdk.tls.rejectClientInitiatedRenegotiation=true
# export PATH=/path/to/jre8/bin:$PATH
# export PATH=/path/to/maven/bin:$PATH
# export PATH=/path/to/mongodb/bin:$PATH

######################################
#### EXTRACTED FROM CONFIGURATION ####
######################################

function prop {
	grep "${1}" $CONF | cut -d "=" -f2
}
export MONGO_HOST=$(prop 'mongo.host')
export MONGO_HOST=${MONGO_HOST:-localhost}
export MONGO_PORT=$(prop 'mongo.port')
export MONGO_PORT=${MONGO_PORT:-27017}
export MONGO_DATABASE=$(prop 'mongo.database')
export MONGO_DATABASE=${MONGO_DATABASE:-nimbus}
export STORAGE=$(prop 'storage.path')
export STORAGE=${STORAGE:-storage}

###########################
#### COMMAND SELECTION ####
###########################

echo "Commande ? [start, stop, update, backup, test]"
read cmd

case $cmd in
    start )
        cd $FOLDER
        export CLASSPATH=./bin:./lib/*:./lib/image4j/*:./lib/javazoom/*:./lib/jave/*
        java $OPTIONS fr.techgp.nimbus.Application &
        ;;
    stop )
        kill $(ps aux | grep '[n]imbus' | awk '{print $2}');
        ;;
    update )
        cd $FOLDER
        rm -rf ./bin/*
        rm ./lib/*.jar
        git pull
        mvn install
        ;;
    backup )
        cd $FOLDER
        d=$(date +'%Y-%m-%d')
        mkdir $d-nimbus
        mongoexport --host $MONGO_HOST:$MONGO_PORT --db $MONGO_DATABASE --collection users --out $d-nimbus/users.json
        mongoexport --host $MONGO_HOST:$MONGO_PORT --db $MONGO_DATABASE --collection items --out $d-nimbus/items.json
        mongoexport --host $MONGO_HOST:$MONGO_PORT --db $MONGO_DATABASE --collection counters --out $d-nimbus/counters.json
        mongodump --host $MONGO_HOST:$MONGO_PORT --db $MONGO_DATABASE --out $d-nimbus
        tar -czf $d-nimbus/files.gz $STORAGE
        ;;
    test )
        echo conf=$CONF
        echo folder=$FOLDER
        echo options=$OPTIONS
        echo host=$MONGO_HOST
        echo port=$MONGO_PORT
        echo database=$MONGO_DATABASE
        echo storage=$STORAGE
        ;;
esac
