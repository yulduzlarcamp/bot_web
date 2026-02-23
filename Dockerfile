# 1. BASE IMAGE - Eclipse Temurin (tavsiya etiladi)
FROM eclipse-temurin:11-jre-alpine

# 2. Metadata
LABEL version="1.0.0"
LABEL description="ASQAROVBULL Telegram Bot"
LABEL maintainer="Your Name"

# 3. Ishlash papkasini yaratish
WORKDIR /app

# 4. Xavfsizlik uchun oddiy foydalanuvchi yaratish
RUN addgroup -S botgroup && adduser -S botuser -G botgroup

# 5. JAR va rasm fayllarini nusxalash
COPY target/bulldrop-bot-1.0.0-shaded.jar app.jar
COPY img.png img.png

# 6. Ruxsatlarni sozlash
RUN chown -R botuser:botgroup /app && \
    chmod 755 /app && \
    chmod 644 /app/app.jar && \
    chmod 644 /app/img.png

# 7. Oddiy foydalanuvchiga o'tish
USER botuser

# 8. Botni ishga tushirish
CMD ["java", "-jar", "app.jar"]