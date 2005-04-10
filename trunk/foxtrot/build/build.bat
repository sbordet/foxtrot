@echo off

setlocal

if not "%JAVA_HOME%"=="" goto start
echo JAVA_HOME not set
goto end

:start

set CP=ant.jar
set CP=%CP%;ant-launcher.jar
set CP=%CP%;ant-junit.jar
set CP=%CP%;..\lib\junit.jar
set CP=%CP%;%JAVA_HOME%\lib\tools.jar

%JAVA_HOME%\bin\java -cp "%CP%" org.apache.tools.ant.Main %*

:end
