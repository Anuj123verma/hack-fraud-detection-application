@echo off
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot"
set "MVND_HOME=C:\Users\a0v0f63\Downloads\maven-mvnd-1.0.6-windows-amd64\maven-mvnd-1.0.6-windows-amd64"
set "PATH=%JAVA_HOME%\bin;%MVND_HOME%\bin;%PATH%"
echo JAVA_HOME=%JAVA_HOME%
java -version
echo ---
mvnd --version
