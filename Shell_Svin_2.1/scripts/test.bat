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

if exist "%OUT%" rmdir /s /q "%OUT%"
mkdir "%OUT%\classes"
mkdir "%OUT%\test-classes"
dir /s /b "%ROOT%\src\main\java\*.java" > "%OUT%\main-sources.txt"
dir /s /b "%ROOT%\tests\java\*.java" > "%OUT%\test-sources.txt"

javac --release 17 -encoding UTF-8 -d "%OUT%\classes" @"%OUT%\main-sources.txt"
if errorlevel 1 (
    echo ERROR: Main-source compilation failed.
    exit /b 1
)
javac --release 17 -encoding UTF-8 -cp "%OUT%\classes" -d "%OUT%\test-classes" @"%OUT%\test-sources.txt"
if errorlevel 1 (
    echo ERROR: Test-source compilation failed.
    exit /b 1
)

cd /d "%ROOT%"
java -cp "%OUT%\classes;%OUT%\test-classes" ru.example.dependencygraph.ConfigValidatorTest
exit /b %errorlevel%
