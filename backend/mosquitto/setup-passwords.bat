@echo off
REM ===========================================
REM Mosquitto Password Setup Script (Windows)
REM Smart Parking System
REM ===========================================

set PASSWORDS_FILE=config\passwords
set BACKEND_USER=sps-backend

echo === Mosquitto Password Setup ===
echo.

REM Check if mosquitto_passwd is available
where mosquitto_passwd >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo ERROR: mosquitto_passwd not found!
    echo Install Mosquitto from https://mosquitto.org/download/
    echo Make sure mosquitto_passwd is in your PATH
    exit /b 1
)

REM Create config directory if not exists
if not exist config mkdir config

echo Setting up MQTT backend password...
echo.

set /p USE_CUSTOM="Use custom password for backend? (y/N): "

if /i "%USE_CUSTOM%"=="y" (
    set /p BACKEND_PASSWORD="Enter password for %BACKEND_USER%: "
) else (
    REM Generate a simple random password (Windows doesn't have openssl by default)
    for /f "tokens=*" %%a in ('powershell -Command "[System.Convert]::ToBase64String([System.Security.Cryptography.RandomNumberGenerator]::GetBytes(24))"') do set BACKEND_PASSWORD=%%a
    echo Generated password: %BACKEND_PASSWORD%
)

echo.
echo Creating password file: %PASSWORDS_FILE%
mosquitto_passwd -c -b "%PASSWORDS_FILE%" "%BACKEND_USER%" "%BACKEND_PASSWORD%"

if %ERRORLEVEL% equ 0 (
    echo.
    echo === SUCCESS ===
    echo Password file created: %PASSWORDS_FILE%
    echo.
    echo Backend credentials:
    echo   Username: %BACKEND_USER%
    echo   Password: %BACKEND_PASSWORD%
    echo.
    echo Add to your .env file or docker-compose environment:
    echo   MQTT_BACKEND_PASSWORD=%BACKEND_PASSWORD%
    echo.
    echo Or update application.properties:
    echo   mqtt.username=%BACKEND_USER%
    echo   mqtt.password=%BACKEND_PASSWORD%
) else (
    echo ERROR: Failed to create password file
    exit /b 1
)

pause

