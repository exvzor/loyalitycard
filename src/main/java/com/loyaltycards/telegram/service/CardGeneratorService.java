package com.loyaltycards.telegram.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.loyaltycards.telegram.entity.LoyaltyCard;
import com.loyaltycards.telegram.entity.TelegramUser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 🎨 ГЕНЕРАТОР красивых карт лояльности с QR-кодами
 */
@Service
public class CardGeneratorService {

    private static final Logger logger = LoggerFactory.getLogger(CardGeneratorService.class);

    // 🎨 Константы дизайна карточки
    private static final int CARD_WIDTH = 800;
    private static final int CARD_HEIGHT = 500;
    private static final int QR_SIZE = 180;
    private static final int CORNER_RADIUS = 30;

    // 🌈 Цветовая схема
    private static final Color PRIMARY_COLOR = new Color(45, 52, 54);
    private static final Color ACCENT_COLOR = new Color(0, 184, 148);
    private static final Color TEXT_COLOR = Color.WHITE;
    private static final Color SECONDARY_TEXT = new Color(220, 221, 225);

    /**
     * 🎨 ГЕНЕРАЦИЯ красивой карточки лояльности
     */
    public byte[] generateLoyaltyCard(LoyaltyCard card, TelegramUser user) throws IOException {
        logger.info("🎨 Генерация карточки для пользователя: {}", user.getUsername());

        // 🖼️ Создаем холст карточки
        BufferedImage cardImage = new BufferedImage(CARD_WIDTH, CARD_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = cardImage.createGraphics();

        // 🎭 Настройки качества рендеринга
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        try {
            // 🎨 Рисуем фон карточки с градиентом
            drawCardBackground(g2d);

            // 📱 Генерируем и размещаем QR-код
            BufferedImage qrImage = generateQRCode(card);
            g2d.drawImage(qrImage, CARD_WIDTH - QR_SIZE - 40, 40, null);

            // ✍️ Добавляем текстовую информацию
            drawCardText(g2d, card, user);

            // 🎯 Добавляем декоративные элементы
            drawCardDecorations(g2d);

            logger.info("✅ Карточка успешно сгенерирована");

        } finally {
            g2d.dispose();
        }

        // 💾 Конвертируем в байты
        return imageToBytes(cardImage);
    }

    /**
     * 🎨 Рисование фона карточки
     */
    private void drawCardBackground(Graphics2D g2d) {
        // 🌈 Создаем градиентный фон
        GradientPaint gradient = new GradientPaint(
                0, 0, PRIMARY_COLOR,
                CARD_WIDTH, CARD_HEIGHT, new Color(70, 70, 70)
        );
        g2d.setPaint(gradient);

        // 📐 Рисуем скруглённый прямоугольник
        g2d.fillRoundRect(0, 0, CARD_WIDTH, CARD_HEIGHT, CORNER_RADIUS, CORNER_RADIUS);

        // ✨ Добавляем акцентную полосу
        g2d.setColor(ACCENT_COLOR);
        g2d.fillRoundRect(0, 0, CARD_WIDTH, 8, CORNER_RADIUS, CORNER_RADIUS);
    }

    /**
     * 📱 Генерация QR-кода
     */
    private BufferedImage generateQRCode(LoyaltyCard card) {
        try {
            // 📋 Формируем данные для QR-кода в JSON формате
            String qrData = String.format(
                    "{\"type\":\"loyalty_card\",\"cardNumber\":\"%s\",\"storeName\":\"%s\",\"userId\":\"%s\",\"points\":%d}",
                    card.getCardNumber(),
                    card.getStoreName(),
                    card.getUser().getId(),
                    card.getPoints() != null ? card.getPoints() : 0
            );

            logger.debug("🔍 QR данные: {}", qrData);

            // ⚙️ Настройки QR-кода
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1);

            // 🎯 Генерируем QR-код
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(qrData, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE, hints);

            // 🖼️ Конвертируем в изображение
            BufferedImage qrImage = new BufferedImage(QR_SIZE, QR_SIZE, BufferedImage.TYPE_INT_RGB);
            Graphics2D qrG2d = qrImage.createGraphics();

            qrG2d.setColor(Color.WHITE);
            qrG2d.fillRect(0, 0, QR_SIZE, QR_SIZE);
            qrG2d.setColor(Color.BLACK);

            for (int x = 0; x < QR_SIZE; x++) {
                for (int y = 0; y < QR_SIZE; y++) {
                    if (matrix.get(x, y)) {
                        qrG2d.fillRect(x, y, 1, 1);
                    }
                }
            }

            qrG2d.dispose();
            return qrImage;

        } catch (WriterException e) {
            logger.error("❌ Ошибка генерации QR-кода: {}", e.getMessage());
            // Возвращаем заглушку
            BufferedImage stub = new BufferedImage(QR_SIZE, QR_SIZE, BufferedImage.TYPE_INT_RGB);
            Graphics2D stubG2d = stub.createGraphics();
            stubG2d.setColor(Color.LIGHT_GRAY);
            stubG2d.fillRect(0, 0, QR_SIZE, QR_SIZE);
            stubG2d.setColor(Color.BLACK);
            stubG2d.drawString("QR ERROR", 60, 90);
            stubG2d.dispose();
            return stub;
        }
    }

    /**
     * ✍️ Добавление текстовой информации
     */
    private void drawCardText(Graphics2D g2d, LoyaltyCard card, TelegramUser user) {
        // 🎭 Настройка шрифтов
        Font titleFont = new Font("Arial", Font.BOLD, 32);
        Font subtitleFont = new Font("Arial", Font.BOLD, 18);
        Font bodyFont = new Font("Arial", Font.PLAIN, 16);
        Font smallFont = new Font("Arial", Font.PLAIN, 14);

        int leftMargin = 40;
        int currentY = 60;

        // 🏪 Название магазина (основной заголовок)
        g2d.setFont(titleFont);
        g2d.setColor(TEXT_COLOR);
        g2d.drawString(card.getStoreName().toUpperCase(), leftMargin, currentY);
        currentY += 50;

        // 💳 Карта лояльности
        g2d.setFont(subtitleFont);
        g2d.setColor(ACCENT_COLOR);
        g2d.drawString("КАРТА ЛОЯЛЬНОСТИ", leftMargin, currentY);
        currentY += 40;

        // 👤 Владелец карты
        g2d.setFont(bodyFont);
        g2d.setColor(SECONDARY_TEXT);
        g2d.drawString("Владелец:", leftMargin, currentY);
        currentY += 25;

        g2d.setColor(TEXT_COLOR);
        String ownerName = (user.getFirstName() != null ? user.getFirstName() : "") +
                (user.getLastName() != null ? " " + user.getLastName() : "");
        if (ownerName.trim().isEmpty()) {
            ownerName = "@" + user.getUsername();
        }
        g2d.drawString(ownerName, leftMargin, currentY);
        currentY += 35;

        // 💳 Номер карты
        g2d.setColor(SECONDARY_TEXT);
        g2d.drawString("Номер карты:", leftMargin, currentY);
        currentY += 25;

        g2d.setFont(new Font("Courier", Font.BOLD, 18));
        g2d.setColor(ACCENT_COLOR);
        g2d.drawString(card.getCardNumber(), leftMargin, currentY);
        currentY += 35;

        // 💎 Баллы
        g2d.setFont(bodyFont);
        g2d.setColor(SECONDARY_TEXT);
        g2d.drawString("Баллы:", leftMargin, currentY);
        currentY += 25;

        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        g2d.setColor(ACCENT_COLOR);
        int points = card.getPoints() != null ? card.getPoints() : 0;
        g2d.drawString(String.valueOf(points), leftMargin, currentY);

        g2d.setFont(bodyFont);
        g2d.setColor(SECONDARY_TEXT);
        g2d.drawString(" баллов", leftMargin + 60, currentY);

        // 📅 Дата создания (внизу карточки)
        g2d.setFont(smallFont);
        g2d.setColor(SECONDARY_TEXT);
        String createDate = "Создана: " + card.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        g2d.drawString(createDate, leftMargin, CARD_HEIGHT - 30);

        // 🤖 Подпись бота (справа внизу)
        String botSignature = "🤖 LoyaltyBot";
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(botSignature);
        g2d.drawString(botSignature, CARD_WIDTH - textWidth - 40, CARD_HEIGHT - 30);
    }

    /**
     * 🎯 Декоративные элементы
     */
    private void drawCardDecorations(Graphics2D g2d) {
        // ✨ Декоративные круги
        g2d.setColor(new Color(ACCENT_COLOR.getRed(), ACCENT_COLOR.getGreen(), ACCENT_COLOR.getBlue(), 30));
        g2d.fillOval(-50, -50, 150, 150);
        g2d.fillOval(CARD_WIDTH - 100, CARD_HEIGHT - 100, 150, 150);

        // 💫 QR-код рамка
        g2d.setColor(ACCENT_COLOR);
        g2d.setStroke(new BasicStroke(3));
        g2d.drawRoundRect(CARD_WIDTH - QR_SIZE - 45, 35, QR_SIZE + 10, QR_SIZE + 10, 10, 10);

        // 📱 Подпись под QR
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.setColor(SECONDARY_TEXT);
        String qrLabel = "Отсканируйте для использования";
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(qrLabel);
        g2d.drawString(qrLabel, CARD_WIDTH - QR_SIZE/2 - textWidth/2 - 40, QR_SIZE + 70);
    }

    /**
     * 💾 Конвертация изображения в байты
     */
    private byte[] imageToBytes(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        byte[] imageBytes = baos.toByteArray();
        baos.close();
        return imageBytes;
    }
}
