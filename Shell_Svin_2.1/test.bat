@echo off
setlocal
chcp 65001 >nul
set "ROOT=%~dp0"
cd /d "%ROOT%"

if not exist "%ROOT%scripts\test.bat" (
    echo ERROR: scripts\test.bat not found.
    echo Make sure you run this file from the project folder.
    pause
    endlocal & exit /b 1
)

call "%ROOT%scripts\test.bat"
set "RC=%ERRORLEVEL%"
echo.
if "%RC%"=="0" (
    echo All tests passed.
) else (
    echo Tests failed with error code %RC%.
)
pause
endlocal & exit /b %RC%
