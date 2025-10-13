package com.loyaltycards.telegram.handlers;

import com.loyaltycards.telegram.entity.LoyaltyCard;
import com.loyaltycards.telegram.entity.TelegramUser;
import com.loyaltycards.telegram.exception.QrCodeException;
import com.loyaltycards.telegram.keyboards.KeyboardFactory;
import com.loyaltycards.telegram.service.CardService;
import com.loyaltycards.telegram.service.UserService;
import com.loyaltycards.telegram.service.QrCodeService;
import com.loyaltycards.telegram.service.CardGeneratorService;
import com.loyaltycards.telegram.bot.LoyaltyTelegramBot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 🎯 ПОЛНОФУНКЦИОНАЛЬНЫЙ ОБРАБОТЧИК с QR-кодами И генерацией карточек
 */
@Component
public class MessageHandler implements ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(MessageHandler.class);

    private final UserService userService;
    private final CardService cardService;
    private final KeyboardFactory keyboardFactory;
    private final QrCodeService qrCodeService;
    private final CardGeneratorService cardGeneratorService;

    // 🔄 Ленивое получение бота через ApplicationContext (разрывает цикл)
    private ApplicationContext applicationContext;
    private LoyaltyTelegramBot bot;

    // 🗂️ Хранение состояний пользователей
    private final Map<Long, String> userStates = new HashMap<>();

    /**
     * 🏗️ КОНСТРУКТОР с CardGeneratorService
     */
    public MessageHandler(UserService userService,
                          CardService cardService,
                          KeyboardFactory keyboardFactory,
                          QrCodeService qrCodeService,
                          CardGeneratorService cardGeneratorService) {
        this.userService = userService;
        this.cardService = cardService;
        this.keyboardFactory = keyboardFactory;
        this.qrCodeService = qrCodeService;
        this.cardGeneratorService = cardGeneratorService;

        logger.info("🚀 MessageHandler инициализирован с полной поддержкой QR-кодов И генерацией карточек");
    }

    /**
     * 🔄 ПОЛУЧЕНИЕ ApplicationContext (разрывает циклическую зависимость)
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * 🤖 ЛЕНИВОЕ получение bot-а (вызывается только при необходимости)
     */
    private LoyaltyTelegramBot getBot() {
        if (bot == null) {
            bot = applicationContext.getBean(LoyaltyTelegramBot.class);
        }
        return bot;
    }

    /**
     * 🎯 ГЛАВНАЯ точка входа - обработка всех сообщений
     */
    public SendMessage handleMessage(Update update) {
        if (update.getMessage() == null) {
            logger.warn("⚠️ Получено пустое сообщение");
            return createErrorMessage(0L, "Неподдерживаемый тип сообщения");
        }

        Message messageObj = update.getMessage();
        Long chatId = messageObj.getChatId();

        if (messageObj.getFrom() == null) {
            return createErrorMessage(chatId, "❌ Не удалось определить отправителя");
        }

        try {
            TelegramUser user = userService.getOrCreateUser(messageObj.getFrom(), chatId);

            // 🔀 Маршрутизация по типу сообщения
            if (messageObj.hasPhoto()) {
                return handlePhotoMessage(messageObj, user);
            }

            if (messageObj.hasText()) {
                return handleTextMessage(messageObj, user);
            }

            return createErrorMessage(chatId, "❌ Отправьте текст или фотографию с QR-кодом");

        } catch (Exception e) {
            logger.error("💥 Ошибка обработки сообщения: {}", e.getMessage(), e);
            return createErrorMessage(chatId, "💥 Произошла техническая ошибка");
        }
    }

    /**
     * 📝 ОБРАБОТКА текстовых сообщений и команд
     */
    private SendMessage handleTextMessage(Message message, TelegramUser user) {
        String text = message.getText();
        Long chatId = message.getChatId();

        logger.debug("📝 Текстовое сообщение от {}: {}", user.getUsername(), text);

        String currentState = userStates.get(chatId);

        if ("/start".equals(text)) {
            userStates.remove(chatId);
            return handleStartCommand(message, user);
        }

        if ("/cards".equals(text) || "💳 Мои карты".equals(text)) {
            return handleShowCardsCommand(chatId, user);
        }

        if ("/help".equals(text) || "❓ Помощь".equals(text)) {
            return handleHelpCommand(chatId);
        }

        if ("📸 Сканировать QR".equals(text) || "📸 Сканировать QR-код".equals(text)) {
            return handleScanInstructions(chatId);
        }

        if ("➕ Добавить карту".equals(text)) {
            userStates.put(chatId, "WAITING_STORE_NAME");
            return createMessage(chatId, "🏪 **Добавление карты**\n\nВведите название магазина:");
        }

        if ("↩️ Главное меню".equals(text) || "↩️ Вернуться в главное меню".equals(text)) {
            userStates.remove(chatId);
            return handleBackToMenu(chatId);
        }

        // 🔄 ОБРАБОТКА состояний для ручного добавления карт
        if ("WAITING_STORE_NAME".equals(currentState)) {
            userStates.put(chatId, "WAITING_CARD_NUMBER:" + text);
            return createMessage(chatId,
                    "📝 **Магазин:** " + text + "\n\n💳 Теперь введите **номер карты:**");
        }

        if (currentState != null && currentState.startsWith("WAITING_CARD_NUMBER:")) {
            String storeName = currentState.substring("WAITING_CARD_NUMBER:".length());
            String cardNumber = text;

            try {
                LoyaltyCard card = cardService.createCard(user, storeName, cardNumber);
                card.setPoints(100); // 🎁 Стартовый бонус
                userStates.remove(chatId);

                // 🎨 ГЕНЕРИРУЕМ КРАСИВУЮ КАРТОЧКУ
                sendGeneratedCard(chatId, card, user);

                SendMessage response = createMessage(chatId,
                        "✅ **Карта успешно добавлена!**\n\n" +
                                "🏪 **Магазин:** " + storeName + "\n" +
                                "💳 **Номер:** `" + cardNumber + "`\n" +
                                "🎁 **Стартовый бонус:** 100 баллов\n\n" +
                                "🎨 **Ваша именная карточка создана!**");
                response.setReplyMarkup(keyboardFactory.getMainMenu());
                return response;

            } catch (Exception e) {
                userStates.remove(chatId);
                return createErrorMessage(chatId, "Ошибка создания карты: " + e.getMessage());
            }
        }

        return createMessage(chatId,
                "❓ **Неизвестная команда:** `" + text + "`\n\nИспользуйте кнопки меню или команду /help");
    }

    /**
     * 📷 ПОЛНАЯ ОБРАБОТКА фотографий с QR-кодами
     */
    private SendMessage handlePhotoMessage(Message message, TelegramUser user) {
        Long chatId = message.getChatId();
        logger.info("📸 Обработка фото от пользователя: {}", user.getUsername());

        try {
            // 📥 Скачиваем изображение с Telegram серверов
            byte[] imageBytes = downloadPhoto(message);
            logger.debug("📊 Загружено изображение: {} байт", imageBytes.length);

            // 🔍 Распознаем QR-код с помощью ZXing
            String qrContent = qrCodeService.decodeQrCode(imageBytes);

            logger.info("🎯 QR-код распознан: {}",
                    qrContent.length() > 50 ? qrContent.substring(0, 50) + "..." : qrContent);

            // 🔀 Определяем тип QR-кода и обрабатываем
            if (qrCodeService.isValidLoyaltyCardFormat(qrContent)) {
                return handleLoyaltyCardQR(chatId, qrContent, user);
            } else if (qrCodeService.isOperationQrCode(qrContent)) {
                return handleOperationQR(chatId, qrContent);
            } else {
                return handleUnknownQR(chatId, qrContent);
            }

        } catch (QrCodeException e) {
            logger.warn("⚠️ QR-ошибка: {}", e.getMessage());
            return createMessage(chatId,
                    "⚠️ **Проблема с QR-кодом**\n\n" + e.getMessage() +
                            "\n\n💡 **Советы:**\n" +
                            "• 📸 Сделайте фото четче\n" +
                            "• 💡 Улучшите освещение\n" +
                            "• 🎯 QR-код полностью в кадре");
        } catch (Exception e) {
            logger.error("💥 Ошибка обработки фото: {}", e.getMessage(), e);
            return createMessage(chatId, "💥 Произошла ошибка обработки фото. Попробуйте еще раз.");
        }
    }

    /**
     * 💳 ОБРАБОТКА QR-кода карты лояльности
     */
    private SendMessage handleLoyaltyCardQR(Long chatId, String qrContent, TelegramUser user) {
        try {
            logger.info("💳 Обработка QR-кода карты лояльности");

            Map<String, String> cardData = qrCodeService.extractCardData(qrContent);
            String cardNumber = cardData.get("cardNumber");
            String storeName = cardData.get("storeName");

            // Проверяем дубликат
            try {
                LoyaltyCard existing = cardService.findByCardNumber(cardNumber);
                return createMessage(chatId, String.format(
                        "⚠️ **Карта уже существует**\n\n" +
                                "🏪 **Магазин:** %s\n" +
                                "💳 **Номер:** `%s`\n" +
                                "💎 **Баллы:** %d\n\n" +
                                "Эта карта уже зарегистрирована в системе.",
                        existing.getStoreName(), existing.getCardNumber(), existing.getPoints()
                ));
            } catch (IllegalArgumentException e) {
                // Карта не найдена - можно создавать
            }

            // Создаем новую карту
            LoyaltyCard newCard = cardService.createCard(user, storeName, cardNumber);
            newCard.setPoints(100); // 🎁 Стартовый бонус

            logger.info("✅ Карта {} создана для {}", cardNumber, user.getUsername());

            // 🎨 ГЕНЕРИРУЕМ КРАСИВУЮ КАРТОЧКУ
            sendGeneratedCard(chatId, newCard, user);

            String successMessage = String.format("""
                ✅ **Карта лояльности добавлена!**
                
                🏪 **Магазин:** %s
                💳 **Номер карты:** `%s`
                🎁 **Стартовый бонус:** 100 баллов
                📅 **Дата добавления:** %s
                
                🎨 **Ваша именная карточка создана!** 
                Теперь вы можете использовать её для накопления баллов.
                
                💡 **Совет:** Показывайте QR-код кассирам для начисления баллов при покупках.
                """,
                    storeName,
                    cardNumber,
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
            );

            SendMessage response = createMessage(chatId, successMessage);
            response.setReplyMarkup(keyboardFactory.getMainMenu());
            return response;

        } catch (QrCodeException e) {
            logger.warn("❌ Ошибка валидации QR-кода: {}", e.getMessage());
            return createErrorMessage(chatId, "Ошибка QR-кода: " + e.getMessage());
        } catch (Exception e) {
            logger.error("❌ Ошибка создания карты: {}", e.getMessage());
            return createErrorMessage(chatId, "Ошибка добавления карты: " + e.getMessage());
        }
    }

    /**
     * ⚡ ОБРАБОТКА операционного QR-кода (начисление/списание баллов)
     */
    private SendMessage handleOperationQR(Long chatId, String qrContent) {
        try {
            logger.info("⚡ Обработка операционного QR-кода");

            Map<String, String> opData = qrCodeService.extractOperationData(qrContent);
            String action = opData.get("action");
            String cardNumber = opData.get("cardNumber");
            int amount = Integer.parseInt(opData.get("amount"));

            logger.info("🔧 Операция: {} {} баллов для карты {}", action, amount, cardNumber);

            LoyaltyCard card;
            String resultMessage;

            if ("ADD".equals(action)) {
                card = cardService.addPoints(cardNumber, amount);
                resultMessage = String.format("""
                    ✅ **Баллы начислены!**
                    
                    🏪 **Магазин:** %s
                    💳 **Карта:** `%s`
                    ➕ **Начислено:** %d баллов
                    💎 **Новый баланс:** %d баллов
                    📅 **Время:** %s
                    
                    Спасибо за покупку! 🛍️✨
                    """,
                        card.getStoreName(),
                        card.getCardNumber(),
                        amount,
                        card.getPoints(),
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
                );

            } else if ("DEDUCT".equals(action)) {
                card = cardService.deductPoints(cardNumber, amount);
                resultMessage = String.format("""
                    ✅ **Баллы списаны!**
                    
                    🏪 **Магазин:** %s
                    💳 **Карта:** `%s`
                    ➖ **Списано:** %d баллов
                    💎 **Остаток:** %d баллов
                    📅 **Время:** %s
                    
                    Приятных покупок! 🛒💫
                    """,
                        card.getStoreName(),
                        card.getCardNumber(),
                        amount,
                        card.getPoints(),
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
                );
            } else {
                return createErrorMessage(chatId, "Неизвестная операция: " + action);
            }

            return createMessage(chatId, resultMessage);

        } catch (IllegalStateException e) {
            logger.warn("⚠️ Ошибка операции: {}", e.getMessage());
            return createErrorMessage(chatId, e.getMessage());
        } catch (NumberFormatException e) {
            return createErrorMessage(chatId, "Некорректное количество баллов в QR-коде");
        } catch (Exception e) {
            logger.error("❌ Ошибка выполнения операции: {}", e.getMessage());
            return createErrorMessage(chatId, "Ошибка выполнения операции: " + e.getMessage());
        }
    }

    /**
     * ❓ ОБРАБОТКА неизвестного формата QR-кода
     */
    private SendMessage handleUnknownQR(Long chatId, String qrContent) {
        logger.info("❓ Неизвестный QR формат: {}", qrContent.substring(0, Math.min(50, qrContent.length())));

        return createMessage(chatId, String.format("""
            ❌ **Неподдерживаемый формат QR-кода**
            
            📋 **Содержимое:** `%s`
            
            **Поддерживаемые форматы:**
            • 💳 QR-коды карт лояльности
            • ⚡ Операционные QR-коды кассиров
            
            **Что попробовать:**
            • 🔄 Попросите новый QR-код  
            • 📞 Обратитесь к персоналу магазина
            • 📷 Убедитесь что сканируете правильный код
            """,
                qrContent.length() > 80 ? qrContent.substring(0, 80) + "..." : qrContent
        ));
    }

    /**
     * 💳 КОМАНДА показа всех карт пользователя
     */
    private SendMessage handleShowCardsCommand(Long chatId, TelegramUser user) {
        List<LoyaltyCard> cards = cardService.getUserCards(user);

        if (cards.isEmpty()) {
            return createMessage(chatId, """
                📭 **У вас пока нет карт лояльности**
                
                **Как добавить карту:**
                1. 📷 Отправьте фото QR-кода карты
                2. ✅ Карта автоматически добавится  
                3. 🎁 Получите 100 бонусных баллов!
                4. 🎨 Получите красивую именную карточку!
                
                **Готовы начать?** Отправьте фото QR-кода прямо сейчас! 📸
                """);
        }

        StringBuilder cardsInfo = new StringBuilder("💳 **Ваши карты лояльности:**\n\n");
        int totalPoints = 0;

        for (int i = 0; i < cards.size(); i++) {
            LoyaltyCard card = cards.get(i);
            int cardPoints = card.getPoints() != null ? card.getPoints() : 0;
            totalPoints += cardPoints;

            cardsInfo.append(String.format("""
                **%d.** 🏪 **%s**
                📋 Номер: `%s`
                💎 Баллы: **%d**
                📅 Добавлена: %s
                
                """,
                    i + 1,
                    card.getStoreName(),
                    card.getCardNumber(),
                    cardPoints,
                    card.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
            ));
        }

        cardsInfo.append(String.format("""
            📊 **Итого:**
            • Карт: **%d**
            • Баллов: **%d**
            • Среднее: **%.1f баллов на карту**
            """,
                cards.size(),
                totalPoints,
                cards.size() > 0 ? (double) totalPoints / cards.size() : 0.0
        ));

        return createMessage(chatId, cardsInfo.toString());
    }

    private SendMessage handleHelpCommand(Long chatId) {
        String helpText = """
            📚 **Руководство пользователя**
            
            **🔧 Команды:**
            • `/start` - главное меню
            • `/cards` - показать карты  
            • `/help` - эта справка
            
            **📷 QR-коды:**
            • Отправьте фото QR-кода карты
            • Бот автоматически добавит карту
            • Получите 100 стартовых баллов!
            • Получите красивую именную карточку!
            
            **⚡ Операции:**
            • QR-коды от кассиров автоматически обрабатываются
            • Начисление и списание баллов
            
            **💡 Советы:**
            • Фото должно быть четким
            • QR-код полностью в кадре  
            • Хорошее освещение обязательно
            
            **❓ Проблемы?** Напишите в поддержку!
            """;

        SendMessage response = createMessage(chatId, helpText);
        response.setReplyMarkup(keyboardFactory.getMainMenu());
        return response;
    }

    private SendMessage handleScanInstructions(Long chatId) {
        return createMessage(chatId, """
            📸 **Как сканировать QR-код**
            
            **Пошагово:**
            1. 📱 Найдите QR-код на карте лояльности
            2. 📷 Сфотографируйте QR-код
            3. 📤 Отправьте фото в этот чат
            4. ⏱️ Дождитесь обработки (2-3 секунды)
            5. ✅ Получите подтверждение и бонус!
            6. 🎨 Получите именную карточку!
            
            **Требования к фото:**
            • 🎯 QR-код полностью в кадре
            • 💡 Хорошее освещение  
            • 📐 Камера параллельно коду
            • 🔍 Четкое изображение
            
            **Готовы?** Отправьте фото QR-кода! 📸
            """);
    }

    private SendMessage handleBackToMenu(Long chatId) {
        SendMessage response = createMessage(chatId, "🏠 **Главное меню**\n\nВыберите действие:");
        response.setReplyMarkup(keyboardFactory.getMainMenu());
        return response;
    }

    private SendMessage handleStartCommand(Message message, TelegramUser user) {
        Long chatId = message.getChatId();
        String userName = message.getFrom().getFirstName();
        Long cardCount = cardService.countUserCards(user);

        String welcomeText = String.format("""
            👋 **Привет, %s!**
            
            🎴 Добро пожаловать в бот управления картами лояльности!
            
            📊 **Ваша статистика:**
            • Сохранено карт: **%d**
            • Статус: %s
            
            **🤖 Что умеет бот:**
            • 📷 Сканировать QR-коды карт
            • 🎨 Генерировать красивые именные карточки
            • 💰 Отслеживать баллы и бонусы
            • ⚡ Обрабатывать операции начисления/списания  
            • 📊 Показывать статистику
            
            Выберите действие в меню: 👇
            """,
                userName,
                cardCount,
                cardCount > 0 ? "Активный пользователь 🌟" : "Новичок 🔰"
        );

        SendMessage response = createMessage(chatId, welcomeText);
        response.setReplyMarkup(keyboardFactory.getMainMenu());
        return response;
    }

    /**
     * 📥 СКАЧИВАНИЕ фотографии с Telegram серверов
     */
    private byte[] downloadPhoto(Message message) throws Exception {
        try {
            List<PhotoSize> photos = message.getPhoto();
            if (photos == null || photos.isEmpty()) {
                throw new Exception("Фотография не найдена в сообщении");
            }

            // 🖼️ Получаем фото наилучшего качества
            PhotoSize largestPhoto = photos.stream()
                    .max(Comparator.comparing(PhotoSize::getFileSize))
                    .orElseThrow(() -> new Exception("Не удалось получить фото"));

            logger.debug("📊 Выбрано фото: размер {} байт", largestPhoto.getFileSize());

            // 📡 Получаем файл через Telegram API (ленивое получение бота)
            GetFile getFileMethod = new GetFile();
            getFileMethod.setFileId(largestPhoto.getFileId());

            File file = getBot().execute(getFileMethod);
            if (file == null || file.getFilePath() == null) {
                throw new Exception("Не удалось получить информацию о файле");
            }

            // 🌐 Формируем URL для скачивания
            String fileUrl = "https://api.telegram.org/file/bot" + getBot().getBotToken() + "/" + file.getFilePath();
            logger.debug("🔗 URL файла: {}", fileUrl);

            // 📥 Скачиваем файл
            try (InputStream is = new URL(fileUrl).openStream()) {
                byte[] imageBytes = is.readAllBytes();
                logger.debug("✅ Файл скачан: {} байт", imageBytes.length);
                return imageBytes;
            }

        } catch (TelegramApiException e) {
            logger.error("💥 Ошибка Telegram API: {}", e.getMessage());
            throw new Exception("Ошибка загрузки фото: " + e.getMessage());
        } catch (IOException e) {
            logger.error("💥 Ошибка ввода-вывода: {}", e.getMessage());
            throw new Exception("Ошибка скачивания файла: " + e.getMessage());
        }
    }

    /**
     * 🎨 ГЕНЕРАЦИЯ и отправка карточки пользователю
     */
    private void sendGeneratedCard(Long chatId, LoyaltyCard card, TelegramUser user) {
        try {
            logger.info("🎨 Генерируем карточку для пользователя: {}", user.getUsername());

            // 🖼️ Генерируем изображение карточки
            byte[] cardImage = cardGeneratorService.generateLoyaltyCard(card, user);

            logger.info("✅ Карточка сгенерирована! Размер: {} байт", cardImage.length);

            // TODO: В следующем шаге добавим отправку изображения через Telegram API

        } catch (Exception e) {
            logger.error("❌ Ошибка генерации карточки: {}", e.getMessage());
        }
    }

    private SendMessage createMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        message.setParseMode("Markdown");
        message.setDisableWebPagePreview(true);
        return message;
    }

    private SendMessage createErrorMessage(Long chatId, String errorText) {
        return createMessage(chatId, "🚫 **Ошибка:** " + errorText);
    }

    public void setUserState(Long chatId, String state) {
        userStates.put(chatId, state);
        logger.debug("🗂️ Установлено состояние '{}' для чата {}", state, chatId);
    }

    public String getUserState(Long chatId) {
        return userStates.get(chatId);
    }

    public void clearUserState(Long chatId) {
        userStates.remove(chatId);
        logger.debug("🗂️ Очищено состояние для чата {}", chatId);
    }
    /**
     * 🎯 ПОЛНАЯ ОБРАБОТКА callback'ов от всех кнопок
     */
    public SendMessage handleCallbackQuery(org.telegram.telegrambots.meta.api.objects.CallbackQuery callbackQuery) {
        String data = callbackQuery.getData();
        Long chatId = callbackQuery.getMessage().getChatId();

        try {
            TelegramUser user = userService.getOrCreateUser(callbackQuery.getFrom(), chatId);

            logger.debug("🎯 Обработка callback: {}", data);

            // 🏠 ГЛАВНОЕ МЕНЮ
            if ("main_menu".equals(data)) {
                userStates.remove(chatId);
                return handleBackToMenu(chatId);
            }

            // 📸 QR ФУНКЦИИ
            if ("scan_qr".equals(data) || "add_card_qr".equals(data)) {
                return handleScanInstructions(chatId);
            }

            if ("generate_card".equals(data)) {
                return handleGenerateCardMenu(chatId, user);
            }

            if (data.startsWith("generate_card:")) {
                String cardId = data.substring("generate_card:".length());
                return handleGenerateSpecificCard(chatId, UUID.fromString(cardId), user);
            }

            // 💳 УПРАВЛЕНИЕ КАРТАМИ
            if ("my_cards".equals(data)) {
                return handleMyCardsMenu(chatId, user);
            }

            if ("add_card".equals(data) || "add_card_manual".equals(data)) {
                return handleAddCardCallback(chatId);
            }

            if ("view_all_cards".equals(data)) {
                return handleShowCardsCommand(chatId, user);
            }

            // 📊 СТАТИСТИКА
            if ("statistics".equals(data)) {
                return handleStatisticsMenu(chatId, user);
            }

            if ("stats_overall".equals(data)) {
                return handleOverallStats(chatId, user);
            }

            // ⚙️ НАСТРОЙКИ
            if ("settings".equals(data)) {
                return handleSettingsMenu(chatId, user);
            }

            // ⚡ ОПЕРАЦИИ
            if ("operations".equals(data)) {
                return handleOperationsMenu(chatId);
            }

            // ❓ ПОМОЩЬ
            if ("help".equals(data)) {
                return handleHelpCommand(chatId);
            }

            // 🗑️ УДАЛЕНИЕ КАРТЫ
            if (data.startsWith("delete_card:")) {
                String cardId = data.substring("delete_card:".length());
                return handleDeleteCardConfirmation(chatId, UUID.fromString(cardId));
            }

            if (data.startsWith("confirm_delete:")) {
                String cardId = data.substring("confirm_delete:".length());
                return handleConfirmDeleteCard(chatId, UUID.fromString(cardId));
            }

            // 💎 ОПЕРАЦИИ С БАЛЛАМИ
            if (data.startsWith("add_points:")) {
                String cardId = data.substring("add_points:".length());
                return handlePointsOperationKeyboard(chatId, "add_points", UUID.fromString(cardId));
            }

            if (data.startsWith("deduct_points:")) {
                String cardId = data.substring("deduct_points:".length());
                return handlePointsOperationKeyboard(chatId, "deduct_points", UUID.fromString(cardId));
            }

            // 🔢 ОПЕРАЦИИ С ЧИСЛАМИ
            if (data.startsWith("add_points:") && data.split(":").length == 3) {
                return handlePointsOperation(chatId, data, "ADD");
            }

            if (data.startsWith("deduct_points:") && data.split(":").length == 3) {
                return handlePointsOperation(chatId, data, "DEDUCT");
            }

            return createErrorMessage(chatId, "Неизвестное действие: " + data);

        } catch (Exception e) {
            logger.error("💥 Ошибка обработки callback: {}", e.getMessage(), e);
            return createErrorMessage(chatId, "Произошла ошибка при обработке действия");
        }
    }

// ➕ НОВЫЕ МЕТОДЫ ДЛЯ ОБРАБОТКИ CALLBACKS:

    private SendMessage handleMyCardsMenu(Long chatId, TelegramUser user) {
        List<LoyaltyCard> cards = cardService.getUserCards(user);
        int totalPoints = cards.stream().mapToInt(c -> c.getPoints() != null ? c.getPoints() : 0).sum();

        String menuText = String.format("""
        💳 **Управление картами**
        
        📊 **Ваша статистика:**
        • Всего карт: **%d**
        • Всего баллов: **%d**
        • Среднее на карту: **%.1f**
        
        Выберите действие:
        """,
                cards.size(),
                totalPoints,
                cards.size() > 0 ? (double) totalPoints / cards.size() : 0.0
        );

        SendMessage response = createMessage(chatId, menuText);
        response.setReplyMarkup(keyboardFactory.getCardsMenu(cards.size(), totalPoints));
        return response;
    }

    private SendMessage handleStatisticsMenu(Long chatId, TelegramUser user) {
        SendMessage response = createMessage(chatId, "📊 **Статистика и аналитика**\n\nВыберите тип отчета:");
        response.setReplyMarkup(keyboardFactory.getStatisticsMenu());
        return response;
    }

    private SendMessage handleSettingsMenu(Long chatId, TelegramUser user) {
        SendMessage response = createMessage(chatId, "⚙️ **Настройки**\n\nНастройте бот под себя:");
        response.setReplyMarkup(keyboardFactory.getSettingsMenu(true)); // true - уведомления включены
        return response;
    }

    private SendMessage handleOperationsMenu(Long chatId) {
        SendMessage response = createMessage(chatId, "⚡ **Операции с баллами**\n\nВыберите тип операции:");
        response.setReplyMarkup(keyboardFactory.getOperationsMenu());
        return response;
    }

    private SendMessage handleGenerateCardMenu(Long chatId, TelegramUser user) {
        List<LoyaltyCard> cards = cardService.getUserCards(user);
        if (cards.isEmpty()) {
            return createMessage(chatId, "💳 **У вас пока нет карт**\n\nСначала добавьте карту через QR-код или вручную!");
        }

        StringBuilder cardsList = new StringBuilder("🎨 **Генерация карточек**\n\nВыберите карту для создания красивой карточки:\n\n");
        for (int i = 0; i < cards.size(); i++) {
            LoyaltyCard card = cards.get(i);
            cardsList.append(String.format("**%d.** 🏪 %s - 💳 `%s`\n",
                    i + 1, card.getStoreName(), card.getCardNumber()));
        }

        return createMessage(chatId, cardsList.toString());
    }

    private SendMessage handleDeleteCardConfirmation(Long chatId, UUID cardId) {
        try {
            // Получаем карту для отображения информации
            // LoyaltyCard card = cardService.findById(cardId); // Нужно добавить этот метод

            String confirmText = """
            🗑️ **Подтверждение удаления**
            
            ⚠️ Вы действительно хотите удалить эту карту?
            
            **Это действие нельзя отменить!**
            """;

            SendMessage response = createMessage(chatId, confirmText);
            response.setReplyMarkup(keyboardFactory.getConfirmationKeyboard("delete", cardId, null));
            return response;

        } catch (Exception e) {
            return createErrorMessage(chatId, "Карта не найдена");
        }
    }

    private SendMessage handlePointsOperationKeyboard(Long chatId, String operation, UUID cardId) {
        String actionText = "add_points".equals(operation) ? "начисления" : "списания";
        String emoji = "add_points".equals(operation) ? "➕" : "➖";

        String text = String.format("%s **Выберите количество баллов для %s:**", emoji, actionText);

        SendMessage response = createMessage(chatId, text);
        response.setReplyMarkup(keyboardFactory.getNumberKeyboard(operation, cardId));
        return response;
    }

    private SendMessage handlePointsOperation(Long chatId, String data, String action) {
        try {
            String[] parts = data.split(":");
            UUID cardId = UUID.fromString(parts[1]);
            int amount = Integer.parseInt(parts[2]);

            LoyaltyCard card;
            if ("ADD".equals(action)) {
                card = cardService.addPoints(cardService.findById(cardId).getCardNumber(), amount);
            } else {
                card = cardService.deductPoints(cardService.findById(cardId).getCardNumber(), amount);
            }

            String resultText = String.format("""
            ✅ **Операция выполнена!**
            
            🏪 **Магазин:** %s
            💳 **Карта:** `%s`
            %s **%s:** %d баллов
            💎 **Новый баланс:** %d баллов
            """,
                    card.getStoreName(),
                    card.getCardNumber(),
                    "ADD".equals(action) ? "➕" : "➖",
                    "ADD".equals(action) ? "Начислено" : "Списано",
                    amount,
                    card.getPoints()
            );

            return createMessage(chatId, resultText);

        } catch (Exception e) {
            logger.error("Ошибка операции с баллами: {}", e.getMessage());
            return createErrorMessage(chatId, "Ошибка выполнения операции: " + e.getMessage());
        }
    }

    private SendMessage handleOverallStats(Long chatId, TelegramUser user) {
        List<LoyaltyCard> cards = cardService.getUserCards(user);

        if (cards.isEmpty()) {
            return createMessage(chatId, "📊 **Статистика пуста**\n\nУ вас пока нет карт лояльности.");
        }

        int totalPoints = cards.stream().mapToInt(c -> c.getPoints() != null ? c.getPoints() : 0).sum();

        Map<String, Integer> storeStats = new HashMap<>();
        for (LoyaltyCard card : cards) {
            storeStats.put(card.getStoreName(),
                    storeStats.getOrDefault(card.getStoreName(), 0) + (card.getPoints() != null ? card.getPoints() : 0));
        }

        StringBuilder statsText = new StringBuilder("""
        📊 **Общая статистика**
        
        📈 **Основные показатели:**
        • Всего карт: **%d**
        • Всего баллов: **%d**
        • Среднее на карту: **%.1f**
        
        🏪 **По магазинам:**
        """.formatted(cards.size(), totalPoints, cards.size() > 0 ? (double) totalPoints / cards.size() : 0.0));

        storeStats.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(5)
                .forEach(entry -> statsText.append(String.format("• **%s:** %d баллов\n", entry.getKey(), entry.getValue())));

        SendMessage response = createMessage(chatId, statsText.toString());
        response.setReplyMarkup(keyboardFactory.getBackToMenuKeyboard());
        return response;
    }

    private SendMessage handleConfirmDeleteCard(Long chatId, UUID cardId) {
        try {
            cardService.deleteCardById(cardId);
            return createMessage(chatId, "✅ **Карта успешно удалена!**\n\nКарта была удалена из вашего списка.");
        } catch (Exception e) {
            return createErrorMessage(chatId, "Ошибка удаления карты: " + e.getMessage());
        }
    }

    private SendMessage handleGenerateSpecificCard(Long chatId, UUID cardId, TelegramUser user) {
        try {
            // LoyaltyCard card = cardService.findById(cardId); // Нужно добавить этот метод
            // sendGeneratedCard(chatId, card, user);

            return createMessage(chatId, "🎨 **Карточка создается...**\n\nПожалуйста, подождите несколько секунд.");

        } catch (Exception e) {
            return createErrorMessage(chatId, "Ошибка генерации карточки: " + e.getMessage());
        }
    }
    /**
     * ➕ НАЧАЛО процесса добавления карты
     */
    private SendMessage handleAddCardCallback(Long chatId) {
        userStates.put(chatId, "WAITING_STORE_NAME");
        return createMessage(chatId,
                "🏪 **Добавление новой карты**\n\n" +
                        "**Способы добавления:**\n" +
                        "1. 📸 **Отправьте фото QR-кода** для автоматического распознавания\n" +
                        "2. ✏️ **Введите название магазина** для ручного добавления\n\n" +
                        "Что выберете?");
    }
}
