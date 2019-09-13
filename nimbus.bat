@echo off

REM ######################
REM #### CUSTOMIZABLE ####
REM ######################

set CONF=nimbus.conf
set FOLDER=%cd%
set OPTIONS=-Djdk.tls.ephemeralDHKeySize=2048 -Djdk.tls.rejectClientInitiatedRenegotiation=true
set PATH=%PATH%;%cd:~0,1%:\apps\openjdk-12\bin
set PATH=%PATH%;%cd:~0,1%:\apps\apache-maven-3.6.2\bin
set PATH=%PATH%;%cd:~0,1%:\apps\mongodb\bin
set PATH=%PATH%;C:\Program Files\7-Zip

REM ######################################
REM #### EXTRACTED FROM CONFIGURATION ####
REM ######################################

set MONGO_HOST=localhost
set MONGO_PORT=27017
set MONGO_DATABASE=nimbus
set STORAGE=storage
FOR /F "tokens=1* delims==" %%A IN (%CONF%) DO (
    IF "%%A"=="mongo.host" set MONGO_HOST=%%B
    IF "%%A"=="mongo.port" set MONGO_PORT=%%B
    IF "%%A"=="mongo.database" set MONGO_DATABASE=%%B
    IF "%%A"=="storage.path" set STORAGE=%%B
)

REM ###########################
REM #### COMMAND SELECTION ####
REM ###########################

REM /C pour les choix, /T pour timeout, /D pour default, /N pour cacher le message par défaut et /M pour message
CHOICE /C 123456 /T 6 /D 6 /N /M "Command ? [1=start, 2=stop, 3=update, 4=backup, 5=compile, 6=test] ?"
set CURRENT_DATETIME=%date:~6,4%-%date:~3,2%-%date:~0,2%-%time:~0,2%-%time:~3,2%

IF %ERRORLEVEL% == 1 (
    set CLASSPATH=.\bin;.\lib\*;.\lib\image4j\*;.\lib\javazoom\*;.\lib\jave\*
    java %OPTIONS% fr.techgp.nimbus.Application

) ELSE IF %ERRORLEVEL% == 2 (
    taskkill /IM java.exe

) ELSE IF %ERRORLEVEL% == 3 (
    rmdir /S ./bin
    mkdir ./bin
    del ./lib/*.jar
    git pull
    mvn install

) ELSE IF %ERRORLEVEL% == 4 (
    mkdir nimbus-%CURRENT_DATETIME%
    mongoexport --host %MONGO_HOST%:%MONGO_PORT% --db %MONGO_DATABASE% --collection users --out nimbus-%CURRENT_DATETIME%/users.json
    mongoexport --host %MONGO_HOST%:%MONGO_PORT% --db %MONGO_DATABASE% --collection items --out nimbus-%CURRENT_DATETIME%/items.json
    mongoexport --host %MONGO_HOST%:%MONGO_PORT% --db %MONGO_DATABASE% --collection counters --out nimbus-%CURRENT_DATETIME%/counters.json
    mongodump --host %MONGO_HOST%:%MONGO_PORT% --db %MONGO_DATABASE% --out nimbus-%CURRENT_DATETIME%
    7z.exe a -r -tzip "nimbus-%CURRENT_DATETIME%/files.zip" %STORAGE%

) ELSE IF %ERRORLEVEL% == 5 (
    mvn compile

) ELSE IF %ERRORLEVEL% == 6 (
    echo conf=%CONF%
    echo folder=%FOLDER%
    echo options=%OPTIONS%
    echo host=%MONGO_HOST%
    echo port=%MONGO_PORT%
    echo database=%MONGO_DATABASE%
    echo storage=%STORAGE%
    echo datetime=%CURRENT_DATETIME%

)

pause
