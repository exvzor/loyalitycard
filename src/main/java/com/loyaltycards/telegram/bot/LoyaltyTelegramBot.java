package com.loyaltycards.telegram.bot;

import com.loyaltycards.telegram.handlers.CallbackQueryHandler;
import com.loyaltycards.telegram.handlers.MessageHandler;
import com.loyaltycards.telegram.keyboards.KeyboardFactory;
import com.loyaltycards.telegram.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
public class LoyaltyTelegramBot extends TelegramLongPollingBot {

    @Value("${bot.token}")
    private String botToken;

    @Value("${bot.username}")
    private String botUsername;

    private final UserService userService;
    private final KeyboardFactory keyboardFactory;
    private final MessageHandler messageHandler;
    private final CallbackQueryHandler callbackQueryHandler;

    public LoyaltyTelegramBot(UserService userService, KeyboardFactory keyboardFactory,
                              MessageHandler messageHandler, CallbackQueryHandler callbackQueryHandler) {
        this.userService = userService;
        this.keyboardFactory = keyboardFactory;
        this.messageHandler = messageHandler;
        this.callbackQueryHandler = callbackQueryHandler;
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
        System.out.println(update.toString() + "RRE");
        try {
            if (update.hasMessage() && update.getMessage().hasText()) {
                SendMessage response = messageHandler.handleMessage(update);
                execute(response);
            } else if (update.hasCallbackQuery()) {
                SendMessage response = callbackQueryHandler.handleCallback(update);
                if (response != null) {
                    execute(response);
                }
            }
        } catch (TelegramApiException e) {
            System.err.println("Telegram API error: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Error processing update: " + e.getMessage());
            e.printStackTrace();
        } // mvn clean package
    }
}
