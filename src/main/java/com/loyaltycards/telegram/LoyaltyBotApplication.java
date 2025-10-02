package com.loyaltycards.telegram;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import com.loyaltycards.telegram.bot.LoyaltyTelegramBot;

@SpringBootApplication
public class LoyaltyBotApplication {

    public static void main(String[] args) {
        System.out.println("=================================");
        System.out.println("🚀 ЗАПУСК TELEGRAM BOT");
        System.out.println("=================================");

        try {
            System.out.println("1️⃣ Запуск Spring Boot...");
            ConfigurableApplicationContext context = SpringApplication.run(LoyaltyBotApplication.class, args);

            System.out.println("2️⃣ Инициализация Telegram API...");
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);

            System.out.println("3️⃣ Получение бота из контекста...");
            LoyaltyTelegramBot bot = context.getBean(LoyaltyTelegramBot.class);

            System.out.println("4️⃣ Регистрация бота...");
            botsApi.registerBot(bot);

            System.out.println("=================================");
            System.out.println("✅ БОТ ЗАПУЩЕН: @" + bot.getBotUsername());
            System.out.println("=================================");

        } catch (TelegramApiException e) {
            System.err.println("❌ ОШИБКА TELEGRAM API:");
            System.err.println(e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("💥 КРИТИЧЕСКАЯ ОШИБКА:");
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }
}
