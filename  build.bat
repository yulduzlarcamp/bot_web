@echo off
echo ========================================
echo    🚀 ASQAROVBULL BOT BUILD SCRIPT
echo ========================================
echo.

echo 1️⃣  JAR fayl yaratilmoqda...
call mvn clean package
if %errorlevel% neq 0 (
    echo ❌ Xatolik! Maven package muvaffaqiyatsiz.
    pause
    exit /b %errorlevel%
)

echo 2️⃣  JAR fayl tekshirilmoqda...
dir target\*.jar

echo 3️⃣  Docker image yaratilmoqda...
docker build -t asqarovbull-bot .

echo.
echo ✅ Bajarildi! Bot tayyor.
echo.
pause