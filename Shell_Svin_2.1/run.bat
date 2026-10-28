@echo off
setlocal
chcp 65001 >nul
set "ROOT=%~dp0"

if not exist "%ROOT%scripts\run.bat" (
    echo ERROR: scripts\run.bat not found.
    echo Make sure you run this file from the project folder.
    pause
    endlocal & exit /b 1
)

call "%ROOT%scripts\run.bat" %*
set "RC=%ERRORLEVEL%"
echo.
if "%RC%"=="0" (
    echo Application finished successfully.
) else (
    echo Application finished with error code %RC%.
)
pause
endlocal & exit /b %RC%
