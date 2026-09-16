@echo off
rem Working directory must be this folder: the game loads resource\ by relative path.
cd /d "%~dp0"

where javaw >nul 2>nul
if errorlevel 1 (
    echo Java not found on PATH. Please install JDK 17 first.
    pause
    exit /b 1
)

start "" javaw -jar "plane.jar"
