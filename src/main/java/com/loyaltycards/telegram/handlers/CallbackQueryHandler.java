package com.loyaltycards.telegram.handlers;

import com.loyaltycards.telegram.entity.LoyaltyCard;
import com.loyaltycards.telegram.entity.TelegramUser;
import com.loyaltycards.telegram.keyboards.KeyboardFactory;
import com.loyaltycards.telegram.service.CardService;
import com.loyaltycards.telegram.service.UserService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import java.util.List;
import java.util.UUID;

@Component
public class CallbackQueryHandler {

    private final KeyboardFactory keyboardFactory;
    private final UserService userService;
    private final CardService cardService;
    private final MessageHandler messageHandler;

    public CallbackQueryHandler(KeyboardFactory keyboardFactory, UserService userService,
                                CardService cardService, MessageHandler messageHandler) {
        this.keyboardFactory = keyboardFactory;
        this.userService = userService;
        this.cardService = cardService;
        this.messageHandler = messageHandler;
    }

    public SendMessage handleCallback(Update update) {
        String callbackData = update.getCallbackQuery().getData();
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        Long userId = update.getCallbackQuery().getFrom().getId();

        SendMessage message = new SendMessage();
        message.setChatId(chatId);

        // Получаем пользователя
        TelegramUser user = userService.getOrCreateUser(update.getCallbackQuery().getFrom(), chatId);

        // Обработка callbacks
        if (callbackData.equals("add_card")) {
            messageHandler.setUserState(chatId, "WAITING_STORE_NAME");
            message.setText("📝 Добавление новой карты\n\n" +
                    "Отправьте название магазина или заведения:");
            message.setReplyMarkup(keyboardFactory.getBackToMenuKeyboard());

        } else if (callbackData.equals("my_cards")) {
            List<LoyaltyCard> cards = cardService.getUserCards(user);

            if (cards.isEmpty()) {
                message.setText("💳 У вас пока нет сохранённых карт.\n\n" +
                        "Нажмите '➕ Добавить карту' чтобы добавить первую карту!");
                message.setReplyMarkup(keyboardFactory.getMainMenu());
            } else {
                StringBuilder text = new StringBuilder("💳 Ваши карты лояльности:\n\n");
                int index = 1;
                for (LoyaltyCard card : cards) {
                    text.append(index++).append(". 🏪 ")
                            .append(card.getStoreName())
                            .append("\n   💳 ")
                            .append(card.getCardNumber())
                            .append("\n\n");
                }
                text.append("📊 Всего карт: ").append(cards.size());

                message.setText(text.toString());
                message.setReplyMarkup(keyboardFactory.getMainMenu());
            }

        } else if (callbackData.equals("help")) {
            message.setText("❓ Помощь\n\n" +
                    "🎴 Этот бот помогает хранить карты лояльности\n\n" +
                    "Команды:\n" +
                    "➕ Добавить карту - сохранить новую карту\n" +
                    "💳 Мои карты - просмотр всех карт\n" +
                    "❓ Помощь - это сообщение\n\n" +
                    "Для начала работы используйте /start");
            message.setReplyMarkup(keyboardFactory.getMainMenu());

        } else if (callbackData.equals("main_menu")) {
            messageHandler.setUserState(chatId, null); // Сбрасываем состояние
            message.setText("🏠 Главное меню\n\nВыберите действие:");
            message.setReplyMarkup(keyboardFactory.getMainMenu());

        } else if (callbackData.startsWith("delete_card:")) {
            String cardIdStr = callbackData.substring("delete_card:".length());
            try {
                UUID cardId = UUID.fromString(cardIdStr);
                cardService.deleteCard(cardId);
                message.setText("✅ Карта успешно удалена!");
                message.setReplyMarkup(keyboardFactory.getMainMenu());
            } catch (Exception e) {
                message.setText("❌ Ошибка при удалении карты.");
                message.setReplyMarkup(keyboardFactory.getMainMenu());
            }

        } else {
            message.setText("❓ Неизвестная команда.");
            message.setReplyMarkup(keyboardFactory.getMainMenu());
        }

        return message;
    }
}
