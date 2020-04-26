@echo off

set PATH=%PATH%;D:\apps\openjdk-11\bin
set NIMBUS_HOME=D:\code\nimbus
set LOG_FILE=sync-example.log
set CONF_FILE=sync-example.conf
set NIMBUS_ARGS=

rem pour les caractères accentués, comme dans Vidéos
chcp 1252

choice /C YNS /M "=> Documents ? (y=Yes, n=No, s=Stop)"
if %ERRORLEVEL% == 3 goto end
if %ERRORLEVEL% == 1 set NIMBUS_ARGS=%NIMBUS_ARGS% 0

choice /C YNS /M "=> Musique ? (y=Yes, n=No, s=Stop)"
if %ERRORLEVEL% == 3 goto end
if %ERRORLEVEL% == 1 set NIMBUS_ARGS=%NIMBUS_ARGS% 1

choice /C YNS /M "=> Photos ? (y=Yes, n=No, s=Stop)"
if %ERRORLEVEL% == 3 goto end
if %ERRORLEVEL% == 1 set NIMBUS_ARGS=%NIMBUS_ARGS% 2

choice /C YNS /M "=> Vidéos ? (y=Yes, n=No, s=Stop)"
if %ERRORLEVEL% == 3 goto end
if %ERRORLEVEL% == 1 set NIMBUS_ARGS=%NIMBUS_ARGS% 3

if exist %LOG_FILE% del %LOG_FILE%
java -cp %NIMBUS_HOME%\bin;%NIMBUS_HOME%\lib\commons-io-2.6.jar;%NIMBUS_HOME%\lib\gson-2.8.6.jar -Dnimbus.log=%LOG_FILE% -Dnimbus.conf=%CONF_FILE% fr.techgp.nimbus.sync.SyncMain %NIMBUS_ARGS%
pause

:end
