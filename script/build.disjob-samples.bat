@echo off
echo.
echo "build disjob-samples"
echo.

cd /d %~dp0

cd ..
call mvnw.cmd clean package -DskipTests -Dcheckstyle.skip=true -Dmaven.javadoc.skip=true -U -f ./disjob-samples/pom.xml

pause
