package com.loyaltycards.telegram.service;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.loyaltycards.telegram.entity.LoyaltyCard;
import com.loyaltycards.telegram.exception.QrCodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * 🔍 СЕРВИС для работы с QR-кодами (ИСПРАВЛЕННАЯ ВЕРСИЯ)
 */
@Service
public class QrCodeService {

    private static final Logger logger = LoggerFactory.getLogger(QrCodeService.class);

    // 🔐 Секретный ключ для подписи QR-кодов
    private static final String SECRET_KEY = "loyalty_bot_secret_2024";

    // 📋 Константы форматов
    private static final String LOYALTY_CARD_PREFIX = "LOYALTY_CARD";
    private static final String OPERATION_PREFIX = "LOYALTY_OP";

    /**
     * 🔍 ДЕКОДИРОВАНИЕ QR-кода из изображения
     */
    public String decodeQrCode(byte[] imageBytes) throws QrCodeException {
        try {
            logger.debug("🔍 Начало декодирования QR-кода...");

            // 📷 Преобразуем байты в BufferedImage
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (image == null) {
                throw new QrCodeException("Не удалось прочитать изображение");
            }

            logger.debug("📊 Размер изображения: {}x{}", image.getWidth(), image.getHeight());

            // 🎯 Создаем объекты для декодирования
            LuminanceSource source = new BufferedImageLuminanceSource(image);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

            // 📱 Настройки декодирования
            Map<DecodeHintType, Object> hints = new HashMap<>();
            hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
            hints.put(DecodeHintType.POSSIBLE_FORMATS, BarcodeFormat.QR_CODE);
            hints.put(DecodeHintType.CHARACTER_SET, "UTF-8");

            // 🔎 Декодируем QR-код
            MultiFormatReader reader = new MultiFormatReader();
            Result result = reader.decode(bitmap, hints);

            String qrText = result.getText();
            logger.info("✅ QR-код успешно декодирован: длина {} символов", qrText.length());
            logger.debug("🔍 Содержимое QR: {}", qrText.length() > 100 ? qrText.substring(0, 100) + "..." : qrText);

            return qrText;

        } catch (IOException e) {
            logger.error("📷 Ошибка чтения изображения: {}", e.getMessage());
            throw new QrCodeException("Не удалось обработать изображение: " + e.getMessage());
        } catch (NotFoundException e) {
            logger.warn("🔍 QR-код не найден на изображении");
            throw new QrCodeException("QR-код не найден на изображении\n\n💡 Убедитесь что:\n• QR-код четко видим\n• Хорошее освещение\n• Камера в фокусе");
        } catch (Exception e) {
            logger.warn("⚠️ Ошибка декодирования: {}", e.getMessage());
            String errorMsg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";

            if (errorMsg.contains("format")) {
                throw new QrCodeException("Неподдерживаемый формат QR-кода");
            } else if (errorMsg.contains("checksum") || errorMsg.contains("corrupt")) {
                throw new QrCodeException("QR-код поврежден или плохо читается\n\nПопробуйте сфотографировать заново");
            } else {
                throw new QrCodeException("Ошибка декодирования QR-кода: " + e.getClass().getSimpleName());
            }
        }
    }

    /**
     * ✅ ПРОВЕРКА формата QR-кода карты лояльности
     */
    public boolean isValidLoyaltyCardFormat(String qrContent) {
        if (qrContent == null || qrContent.trim().isEmpty()) {
            return false;
        }

        try {
            // 🎯 Проверяем собственный JSON формат
            if (qrContent.contains("\"type\":\"loyalty_card\"") &&
                    qrContent.contains("\"cardNumber\"") &&
                    qrContent.contains("\"storeName\"")) {
                logger.debug("🔍 Найден JSON формат карты");
                return true;
            }

            // 🔄 Старый формат LOYALTY_CARD
            String[] parts = qrContent.split(":");
            boolean isOldFormat = parts.length == 5 && LOYALTY_CARD_PREFIX.equals(parts[0]);

            if (isOldFormat) {
                logger.debug("🔍 Найден старый формат LOYALTY_CARD");
                return true;
            }

            logger.debug("🔍 Проверка формата карты: {} -> ❌ невалидный",
                    qrContent.substring(0, Math.min(30, qrContent.length())));
            return false;

        } catch (Exception e) {
            logger.warn("⚠️ Ошибка валидации QR: {}", e.getMessage());
            return false;
        }
    }

    /**
     * ⚡ ПРОВЕРКА операционного QR-кода
     */
    public boolean isOperationQrCode(String qrContent) {
        if (qrContent == null || qrContent.trim().isEmpty()) {
            return false;
        }

        try {
            // 🎯 JSON формат операций
            if (qrContent.contains("\"type\":\"loyalty_operation\"")) {
                logger.debug("🔍 Найден JSON формат операции");
                return true;
            }

            // 🔄 Старый формат операций
            String[] parts = qrContent.split(":");
            boolean isOldOpFormat = parts.length == 5 && OPERATION_PREFIX.equals(parts[0]);

            if (isOldOpFormat) {
                logger.debug("🔍 Найден старый формат LOYALTY_OP");
                return true;
            }

            return false;

        } catch (Exception e) {
            logger.warn("⚠️ Ошибка проверки операционного QR: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 📋 ИЗВЛЕЧЕНИЕ данных карты из QR-кода
     */
    public Map<String, String> extractCardData(String qrContent) throws QrCodeException {
        if (!isValidLoyaltyCardFormat(qrContent)) {
            throw new QrCodeException("Неверный формат QR-кода карты лояльности");
        }

        Map<String, String> cardData = new HashMap<>();

        try {
            if (qrContent.contains("\"type\":\"loyalty_card\"")) {
                // 🎯 НОВЫЙ JSON формат (собственные карточки)
                logger.debug("📋 Парсинг JSON формата карты");

                cardData.put("cardNumber", extractJsonValue(qrContent, "cardNumber"));
                cardData.put("storeName", extractJsonValue(qrContent, "storeName"));
                cardData.put("userId", extractJsonValue(qrContent, "userId"));
                cardData.put("points", extractJsonValue(qrContent, "points"));
                cardData.put("format", "json");

                logger.info("✅ JSON карта валидирована: {}", cardData.get("cardNumber"));

            } else {
                // 🔄 СТАРЫЙ формат с подписью
                logger.debug("📋 Парсинг старого формата карты");

                String[] parts = qrContent.split(":");

                // 🔐 Проверка криптографической подписи
                String dataForHash = String.join(":", parts[1], parts[2], parts[3]);
                String expectedHash = generateSecureHash(dataForHash + SECRET_KEY);

                if (!expectedHash.equals(parts[4])) {
                    logger.warn("🚨 ОБНАРУЖЕНА попытка использования поддельного QR-кода!");
                    throw new QrCodeException("🚨 QR-код поврежден или поддельный!\n\nЭто может быть попытка мошенничества.");
                }

                cardData.put("cardNumber", parts[1]);
                cardData.put("storeName", parts[2].replaceAll("_", " "));
                cardData.put("timestamp", parts[3]);
                cardData.put("format", "legacy");

                logger.info("✅ Legacy карта валидирована: {}", parts[1]);
            }

            return cardData;

        } catch (Exception e) {
            logger.error("❌ Ошибка извлечения данных карты: {}", e.getMessage());
            throw new QrCodeException("Ошибка извлечения данных: " + e.getMessage());
        }
    }

    /**
     * ⚡ ИЗВЛЕЧЕНИЕ данных операции из QR-кода
     */
    public Map<String, String> extractOperationData(String qrContent) throws QrCodeException {
        if (!isOperationQrCode(qrContent)) {
            throw new QrCodeException("Неверный формат операционного QR-кода");
        }

        Map<String, String> operationData = new HashMap<>();

        try {
            if (qrContent.contains("\"type\":\"loyalty_operation\"")) {
                // 🎯 JSON формат операций
                operationData.put("action", extractJsonValue(qrContent, "action"));
                operationData.put("cardNumber", extractJsonValue(qrContent, "cardNumber"));
                operationData.put("amount", extractJsonValue(qrContent, "amount"));
                operationData.put("storeId", extractJsonValue(qrContent, "storeId"));

            } else {
                // 🔄 Старый формат операций
                String[] parts = qrContent.split(":");

                // 🔐 Проверка подписи
                String dataForHash = String.join(":", parts[1], parts[2], parts[3]);
                String expectedHash = generateSecureHash(dataForHash + SECRET_KEY);

                if (!expectedHash.equals(parts[4])) {
                    throw new QrCodeException("Операционный QR-код поврежден или поддельный!");
                }

                operationData.put("action", parts[1]);
                operationData.put("cardNumber", parts[2]);
                operationData.put("amount", parts[3]);
            }

            return operationData;

        } catch (Exception e) {
            logger.error("❌ Ошибка извлечения операционных данных: {}", e.getMessage());
            throw new QrCodeException("Ошибка извлечения данных операции: " + e.getMessage());
        }
    }

    /**
     * 🔧 Вспомогательный метод для парсинга JSON значений
     */
    private String extractJsonValue(String json, String key) {
        try {
            String pattern = "\"" + key + "\":\"?([^,}\"]+)\"?";
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(json);

            if (m.find()) {
                String value = m.group(1);
                logger.debug("🔍 Извлечено значение {}: {}", key, value);
                return value;
            }

            logger.warn("⚠️ Не найден ключ '{}' в JSON", key);
            return "";

        } catch (Exception e) {
            logger.error("❌ Ошибка парсинга JSON для ключа '{}': {}", key, e.getMessage());
            return "";
        }
    }

    /**
     * 🎨 ГЕНЕРАЦИЯ QR-кода для собственной карточки в JSON формате
     */
    public String generateJsonQRData(LoyaltyCard card) {
        try {
            String jsonData = String.format(
                    "{\"type\":\"loyalty_card\",\"cardNumber\":\"%s\",\"storeName\":\"%s\",\"userId\":\"%s\",\"points\":%d,\"created\":\"%s\"}",
                    card.getCardNumber(),
                    card.getStoreName().replaceAll("\"", "\\\\\""), // Экранируем кавычки
                    card.getUser().getId(),
                    card.getPoints() != null ? card.getPoints() : 0,
                    card.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE)
            );

            logger.debug("🎨 Сгенерированы JSON данные для QR: {}", jsonData);
            return jsonData;

        } catch (Exception e) {
            logger.error("❌ Ошибка генерации JSON для QR: {}", e.getMessage());
            return "{\"type\":\"loyalty_card\",\"error\":\"generation_failed\"}";
        }
    }

    /**
     * 🔐 ГЕНЕРАЦИЯ безопасного хеша для подписи
     */
    private String generateSecureHash(String data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(data.getBytes("UTF-8"));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            // Возвращаем первые 16 символов для компактности
            return hexString.toString().substring(0, 16);

        } catch (Exception e) {
            logger.error("❌ Ошибка генерации хеша: {}", e.getMessage());
            return "error_hash";
        }
    }
}
