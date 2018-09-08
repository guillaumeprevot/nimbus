set PATH=%PATH%;%cd:~0,1%:\apps\jdk-8\bin
set CLASSPATH=.\bin
set CLASSPATH=%CLASSPATH%;.\lib\*
set CLASSPATH=%CLASSPATH%;.\lib\image4j\*
set CLASSPATH=%CLASSPATH%;.\lib\javazoom\*
set CLASSPATH=%CLASSPATH%;.\lib\jave\*

java fr.techgp.nimbus.Application

pause
