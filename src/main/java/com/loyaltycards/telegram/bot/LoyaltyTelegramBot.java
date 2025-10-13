package com.loyaltycards.telegram.bot;

import com.loyaltycards.telegram.handlers.MessageHandler;
import com.loyaltycards.telegram.keyboards.KeyboardFactory;
import com.loyaltycards.telegram.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



@Component
public class LoyaltyTelegramBot extends TelegramLongPollingBot {
    private static final Logger logger = LoggerFactory.getLogger(LoyaltyTelegramBot.class);
    @Value("${bot.token}")
    private String botToken;

    @Value("${bot.username}")
    private String botUsername;

    private final UserService userService;
    private final KeyboardFactory keyboardFactory;
    private final MessageHandler messageHandler;


    public LoyaltyTelegramBot(UserService userService, KeyboardFactory keyboardFactory,
                              MessageHandler messageHandler) {
        this.userService = userService;
        this.keyboardFactory = keyboardFactory;
        this.messageHandler = messageHandler;
    }


    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            SendMessage response = null;

            // 📝 ОБРАБОТКА текстовых сообщений
            if (update.hasMessage()) {
                response = messageHandler.handleMessage(update);

                // 🎯 ОБРАБОТКА кнопок (callback queries)
            } else if (update.hasCallbackQuery()) {
                // Используем MessageHandler вместо CallbackQueryHandler!
                response = messageHandler.handleCallbackQuery(update.getCallbackQuery());

                // Убираем "часики" с кнопки
                try {
                    org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery answerCallback =
                            new org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery();
                    answerCallback.setCallbackQueryId(update.getCallbackQuery().getId());
                    execute(answerCallback);
                } catch (Exception e) {
                    System.err.println("Callback answer error: " + e.getMessage());
                }
            }

            // 📤 ОТПРАВЛЯЕМ ответ
            if (response != null) {
                execute(response);
            }

        } catch (TelegramApiException e) {
            System.err.println("Telegram API error: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Error processing update: " + e.getMessage());
            e.printStackTrace();
        }
    }


}
