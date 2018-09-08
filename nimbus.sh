#!/bin/sh

if [ -d "./bin" ];then
 rm -rf ./bin/*
else
 mkdir ./bin
fi

mvn install

java -cp ./bin:./lib/*:./lib/image4j/*:./lib/javazoom/*:./lib/jave/* fr.techgp.nimbus.Application

