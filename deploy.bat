@echo off
echo ========================================
echo    🚀 ASQAROVBULL BOT DEPLOY SCRIPT
echo ========================================
echo.

echo 1️⃣  Git status tekshirilmoqda...
git status

echo.
echo 2️⃣  Fayllar qo'shilmoqda...
git add .

echo.
echo 3️⃣  Commit qilinmoqda...
git commit -m "Update bot"

echo.
echo 4️⃣  GitHub ga yuklanmoqda...
git push

echo.
echo ✅ Bajarildi! Endi Railway avtomatik deploy qiladi.
echo.
pause