@echo off
echo.
echo "build disjob"
echo.

cd /d %~dp0

cd ..
call mvnw.cmd clean install -DskipTests -Dcheckstyle.skip=true -Dmaven.javadoc.skip=true -U -f ./pom.xml

pause
