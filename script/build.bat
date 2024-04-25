@echo off
echo.
echo "build disjob"
echo.

cd /d %~dp0
cd ..

set path=%~1
if not "%path%" == "" (
  path=%path%/
)

call mvnw.cmd clean install -DskipTests -Dcheckstyle.skip=true -Dmaven.javadoc.skip=true -U -f ./%path%pom.xml
pause
