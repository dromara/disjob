@echo off

cd /d %~dp0
cd ..

set path=%~1
set name=%~1

if not "%path%" == "" (
  set path=%path%/
) else (
  set name=disjob
)

set path=./%path%pom.xml

echo.
echo build %name%: %path%
echo.

call mvnw.cmd clean install -DskipTests -Dcheckstyle.skip=true -Dmaven.javadoc.skip=true -U -f %path%

pause
