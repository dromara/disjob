@REM --------------------------------------------------
@REM direct execute      : .\mvnw.cmd clean install
@REM build disjob        : .\build.bat
@REM build disjob-samples: .\build.bat disjob-samples
@REM build disjob-admin  : .\build.bat disjob-admin
@REM build all           : .\build.bat && .\build.bat disjob-samples && .\build.bat disjob-admin
@REM --------------------------------------------------

@echo off

cd /d %~dp0

set p_path=%~1
set p_name=%~1

if not "%p_path%" == "" (
  set p_path=%p_path%/
) else (
  set p_name=disjob
)

set p_path=./%p_path%pom.xml

echo.
echo build %p_name%: %p_path%
echo.

call mvnw.cmd clean install -DskipTests -Dcheckstyle.skip=true -Dmaven.javadoc.skip=true -U -f %p_path%

pause
