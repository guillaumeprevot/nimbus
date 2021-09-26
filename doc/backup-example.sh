#!/bin/bash

######################
#### CUSTOMIZABLE ####
######################

export CONF=nimbus.conf
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
#### COMMAND EXECUTION ####
###########################

d=$(date +'%Y-%m-%d')
mkdir $d-nimbus
mongoexport --host $MONGO_HOST:$MONGO_PORT --db $MONGO_DATABASE --collection users --out $d-nimbus/users.json
mongoexport --host $MONGO_HOST:$MONGO_PORT --db $MONGO_DATABASE --collection items --out $d-nimbus/items.json
mongoexport --host $MONGO_HOST:$MONGO_PORT --db $MONGO_DATABASE --collection counters --out $d-nimbus/counters.json
mongodump --host $MONGO_HOST:$MONGO_PORT --db $MONGO_DATABASE --out $d-nimbus
tar -czf $d-nimbus/files.gz $STORAGE
