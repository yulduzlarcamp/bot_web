#!/bin/bash

echo "╔════════════════════════════════════╗"
echo "║    🚀 ASQAROVBULL BOT v2.0        ║"
echo "╚════════════════════════════════════╝"

# Java version tekshirish
if ! command -v java &> /dev/null; then
    echo "❌ Java topilmadi! Java 11 o'rnating."
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
echo "✅ Java versiyasi: $JAVA_VERSION"

# JAR faylni tekshirish
JAR_FILE="target/AsqarovBullBot-1.0-SNAPSHOT-jar-with-dependencies.jar"
if [ ! -f "$JAR_FILE" ]; then
    echo "❌ JAR fayl topilmadi! Avval 'mvn clean package' bajaring."
    exit 1
fi

# Rasm faylini tekshirish
if [ ! -f "img.png" ]; then
    echo "⚠️ img.png topilmadi! Rasm serverga yuklanmagan."
fi

echo "✅ Barcha tekshiruvlar o'tdi!"
echo "🚀 Bot ishga tushirilmoqda..."
echo "----------------------------------------"

# Botni ishga tushirish
java -Xmx256m -jar "$JAR_FILE"