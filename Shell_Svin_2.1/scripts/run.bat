@echo off
setlocal
chcp 65001 >nul
set "ROOT=%~dp0.."
for %%I in ("%ROOT%") do set "ROOT=%%~fI"
set "OUT=%ROOT%\out"

where javac >nul 2>nul
if errorlevel 1 (
    echo ERROR: javac not found. Install JDK 17 or newer and add its bin folder to PATH.
    exit /b 1
)
where java >nul 2>nul
if errorlevel 1 (
    echo ERROR: java not found. Install JDK 17 or newer and add its bin folder to PATH.
    exit /b 1
)

if "%~1"=="" (
    if not exist "%ROOT%\config.ini" (
        echo ERROR: config.ini not found: "%ROOT%\config.ini"
        echo Restore config.ini to the project root or pass a path to an INI file.
        exit /b 1
    )
) else (
    if not exist "%~1" if not exist "%ROOT%\%~1" (
        echo ERROR: configuration file not found: "%~1"
        exit /b 1
    )
)

if not exist "%OUT%\classes" mkdir "%OUT%\classes"
dir /s /b "%ROOT%\src\main\java\*.java" > "%OUT%\main-sources.txt"
javac --release 17 -encoding UTF-8 -d "%OUT%\classes" @"%OUT%\main-sources.txt"
if errorlevel 1 (
    echo ERROR: Java compilation failed.
    exit /b 1
)

cd /d "%ROOT%"
if "%~1"=="" (
    java -cp "%OUT%\classes" ru.example.dependencygraph.Main "%ROOT%\config.ini"
) else (
    if exist "%~1" (
        java -cp "%OUT%\classes" ru.example.dependencygraph.Main "%~1"
    ) else (
        java -cp "%OUT%\classes" ru.example.dependencygraph.Main "%ROOT%\%~1"
    )
)
exit /b %errorlevel%
