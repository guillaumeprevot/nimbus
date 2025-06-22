@echo off

REM ######################
REM #### CUSTOMIZABLE ####
REM ######################

set CONF=nimbus.conf
set STORAGE=storage
set FOLDER=%cd%
set OPTIONS=-Djdk.tls.ephemeralDHKeySize=2048 -Djdk.tls.rejectClientInitiatedRenegotiation=true
set PATH=%PATH%;%cd:~0,1%:\apps\openjdk\bin
set PATH=%PATH%;%cd:~0,1%:\apps\apache-maven\bin
set PATH=%PATH%;C:\Program Files\7-Zip

REM ###########################
REM #### COMMAND SELECTION ####
REM ###########################

REM /C pour les choix, /T pour timeout, /D pour default, /N pour cacher le message par défaut et /M pour message
CHOICE /C 123 /T 6 /D 3 /N /M "Command ? [1=start, 2=stop, 3=update] ?"

IF %ERRORLEVEL% == 1 (
	set CLASSPATH=.\bin;.\lib\*;.\lib\image4j\*;.\lib\javazoom\*;.\lib\jave\*
	java %OPTIONS% fr.techgp.nimbus.Application

) ELSE IF %ERRORLEVEL% == 2 (
	taskkill /IM java.exe

) ELSE IF %ERRORLEVEL% == 3 (
	IF EXIST ..\nimbus-java-api (
		cd ..\nimbus-java-api
		git pull --rebase
		mvn install
		cd %FOLDER%
	)
	rmdir /S .\bin
	mkdir .\bin
	del .\lib\*.jar
	git pull --rebase
	mvn install

)

pause
