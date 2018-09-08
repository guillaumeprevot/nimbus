set JAVA_HOME=%cd:~0,1%:\apps\jdk-8
set PATH=%PATH%;%JAVA_HOME%\bin;%cd:~0,1%:\apps\apache-maven-3.5.4\bin
mvn compile
