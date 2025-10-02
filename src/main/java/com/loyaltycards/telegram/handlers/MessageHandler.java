package com.loyaltycards.telegram.handlers;

import com.loyaltycards.telegram.entity.TelegramUser;
import com.loyaltycards.telegram.keyboards.KeyboardFactory;
import com.loyaltycards.telegram.service.CardService;
import com.loyaltycards.telegram.service.UserService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import java.util.HashMap;
import java.util.Map;

@Component
public class MessageHandler {

    private final UserService userService;
    private final CardService cardService;
    private final KeyboardFactory keyboardFactory;

    // Хранение состояний пользователей (в реальном приложении используй БД или Redis)
    private final Map<Long, String> userStates = new HashMap<>();

    public MessageHandler(UserService userService, CardService cardService, KeyboardFactory keyboardFactory) {
        this.userService = userService;
        this.cardService = cardService;
        this.keyboardFactory = keyboardFactory;
    }

    public SendMessage handleMessage(Update update) {
        String text = update.getMessage().getText();
        Long chatId = update.getMessage().getChatId();
        TelegramUser user = userService.getOrCreateUser(update.getMessage().getFrom(), chatId);

        // Проверяем состояние пользователя
        String currentState = userStates.get(chatId);

        if (text.equals("/start")) {
            userStates.remove(chatId); // Сбрасываем состояние
            return handleStartCommand(update);
        }

        // Обработка добавления карты
        if ("WAITING_STORE_NAME".equals(currentState)) {
            userStates.put(chatId, "WAITING_CARD_NUMBER:" + text);

            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("📝 Магазин: " + text + "\n\nТеперь отправьте номер карты:");
            return message;
        }

        if (currentState != null && currentState.startsWith("WAITING_CARD_NUMBER:")) {
            String storeName = currentState.substring("WAITING_CARD_NUMBER:".length());
            String cardNumber = text;

            // Создаём карту
            cardService.createCard(user, storeName, cardNumber);
            userStates.remove(chatId);

            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("✅ Карта успешно добавлена!\n\n" +
                    "🏪 Магазин: " + storeName + "\n" +
                    "💳 Номер: " + cardNumber);
            message.setReplyMarkup(keyboardFactory.getMainMenu());
            return message;
        }

        // Неизвестная команда
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("❓ Я не понимаю эту команду. Используйте /start");
        return message;
    }

    public SendMessage handleStartCommand(Update update) {
        Long chatId = update.getMessage().getChatId();
        String userName = update.getMessage().getFrom().getFirstName();
        TelegramUser user = userService.getOrCreateUser(update.getMessage().getFrom(), chatId);

        Long cardCount = cardService.countUserCards(user);

        String welcomeText = "👋 Привет, " + userName + "!\n\n" +
                "🎴 Добро пожаловать в бот управления картами лояльности!\n\n" +
                "📊 У вас сохранено карт: " + cardCount + "\n\n" +
                "Выберите действие:";

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(welcomeText);
        message.setReplyMarkup(keyboardFactory.getMainMenu());

        return message;
    }

    public void setUserState(Long chatId, String state) {
        userStates.put(chatId, state);
    }
}
