@echo off
rem Windows launcher for the Bloom real-server harness (scripts/realserver/run.sh).
rem
rem Usage:   scripts\realserver\run.bat [minecraft-version ...]
rem Example: scripts\realserver\run.bat 1.8.8
rem With no versions, runs the default set: 1.7.10 1.8.8 1.21.11 26.3
rem
rem Running a Minecraft server requires accepting the Minecraft EULA (https://aka.ms/MinecraftEULA).
rem You are asked to type YES unless BLOOM_ACCEPT_MINECRAFT_EULA=true is already set.
setlocal EnableDelayedExpansion

set "BASH=%ProgramFiles%\Git\bin\bash.exe"
if not exist "%BASH%" (
    echo Git Bash was not found at "%BASH%". Install Git for Windows: https://git-scm.com/download/win
    exit /b 1
)

if /i not "%BLOOM_ACCEPT_MINECRAFT_EULA%"=="true" (
    echo Running a Minecraft server requires accepting the Minecraft EULA:
    echo   https://aka.ms/MinecraftEULA
    set /p "ANSWER=Type YES to accept it and continue: "
    if /i not "!ANSWER!"=="YES" (
        echo EULA not accepted; nothing was started.
        exit /b 2
    )
    set "BLOOM_ACCEPT_MINECRAFT_EULA=true"
)

set "VERSIONS=%*"
if "%VERSIONS%"=="" set "VERSIONS=1.7.10 1.8.8 1.21.11 26.3"

set "FAILED="
for %%V in (%VERSIONS%) do (
    echo.
    echo ===== Minecraft %%V =====
    "%BASH%" "%~dp0run.sh" %%V
    if errorlevel 1 (set "FAILED=!FAILED! %%V") else (echo ----- %%V passed)
)

echo.
if defined FAILED (
    echo FAILED:%FAILED%
    echo Logs and results are in build\realserver\^<version^>\
    exit /b 1
)
echo All versions passed: %VERSIONS%
exit /b 0
