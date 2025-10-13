package com.loyaltycards.telegram.keyboards;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 🎹 ФАБРИКА КЛАВИАТУР - все кнопки проекта
 */
@Component
public class KeyboardFactory {

    /**
     * 🏠 ГЛАВНОЕ МЕНЮ - основные функции бота
     */
    public InlineKeyboardMarkup getMainMenu() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        // 📸 Первая строка - QR функции
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton scanQrButton = new InlineKeyboardButton();
        scanQrButton.setText("📸 Сканировать QR");
        scanQrButton.setCallbackData("scan_qr");
        row1.add(scanQrButton);

        InlineKeyboardButton generateCardButton = new InlineKeyboardButton();
        generateCardButton.setText("🎨 Создать карточку");
        generateCardButton.setCallbackData("generate_card");
        row1.add(generateCardButton);

        // 💳 Вторая строка - Управление картами
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton myCardsButton = new InlineKeyboardButton();
        myCardsButton.setText("💳 Мои карты");
        myCardsButton.setCallbackData("my_cards");
        row2.add(myCardsButton);

        InlineKeyboardButton addCardButton = new InlineKeyboardButton();
        addCardButton.setText("➕ Добавить карту");
        addCardButton.setCallbackData("add_card");
        row2.add(addCardButton);

        // 📊 Третья строка - Статистика и операции
        List<InlineKeyboardButton> row3 = new ArrayList<>();
        InlineKeyboardButton statisticsButton = new InlineKeyboardButton();
        statisticsButton.setText("📊 Статистика");
        statisticsButton.setCallbackData("statistics");
        row3.add(statisticsButton);

        InlineKeyboardButton operationsButton = new InlineKeyboardButton();
        operationsButton.setText("⚡ Операции");
        operationsButton.setCallbackData("operations");
        row3.add(operationsButton);

        // ⚙️ Четвертая строка - Настройки и помощь
        List<InlineKeyboardButton> row4 = new ArrayList<>();
        InlineKeyboardButton settingsButton = new InlineKeyboardButton();
        settingsButton.setText("⚙️ Настройки");
        settingsButton.setCallbackData("settings");
        row4.add(settingsButton);

        InlineKeyboardButton helpButton = new InlineKeyboardButton();
        helpButton.setText("❓ Помощь");
        helpButton.setCallbackData("help");
        row4.add(helpButton);

        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);
        keyboard.add(row4);

        markup.setKeyboard(keyboard);
        return markup;
    }

    /**
     * 💳 МЕНЮ КАРТ - управление картами лояльности
     */
    public InlineKeyboardMarkup getCardsMenu(int totalCards, int totalPoints) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        if (totalCards > 0) {
            // 📋 Первая строка - Просмотр карт
            List<InlineKeyboardButton> row1 = new ArrayList<>();
            InlineKeyboardButton viewAllButton = new InlineKeyboardButton();
            viewAllButton.setText("📋 Все карты (" + totalCards + ")");
            viewAllButton.setCallbackData("view_all_cards");
            row1.add(viewAllButton);

            InlineKeyboardButton sortButton = new InlineKeyboardButton();
            sortButton.setText("🔄 Сортировка");
            sortButton.setCallbackData("sort_cards");
            row1.add(sortButton);

            // 💎 Вторая строка - Баллы
            List<InlineKeyboardButton> row2 = new ArrayList<>();
            InlineKeyboardButton pointsButton = new InlineKeyboardButton();
            pointsButton.setText("💎 Баллы (" + totalPoints + ")");
            pointsButton.setCallbackData("view_points");
            row2.add(pointsButton);

            InlineKeyboardButton topCardsButton = new InlineKeyboardButton();
            topCardsButton.setText("🏆 Топ карты");
            topCardsButton.setCallbackData("top_cards");
            row2.add(topCardsButton);

            keyboard.add(row1);
            keyboard.add(row2);
        }

        // ➕ Добавление новых карт
        List<InlineKeyboardButton> row3 = new ArrayList<>();
        InlineKeyboardButton addQrButton = new InlineKeyboardButton();
        addQrButton.setText("📸 Добавить QR-кодом");
        addQrButton.setCallbackData("add_card_qr");
        row3.add(addQrButton);

        InlineKeyboardButton addManualButton = new InlineKeyboardButton();
        addManualButton.setText("✏️ Добавить вручную");
        addManualButton.setCallbackData("add_card_manual");
        row3.add(addManualButton);

        // 🔙 Назад
        List<InlineKeyboardButton> row4 = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("◀️ Главное меню");
        backButton.setCallbackData("main_menu");
        row4.add(backButton);

        keyboard.add(row3);
        keyboard.add(row4);

        markup.setKeyboard(keyboard);
        return markup;
    }

    /**
     * 📱 ДЕТАЛИ КАРТЫ - действия с конкретной картой
     */
    public InlineKeyboardMarkup getCardDetailKeyboard(UUID cardId, boolean hasQr) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        // 🎨 Первая строка - Карточка и QR
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        if (hasQr) {
            InlineKeyboardButton showQrButton = new InlineKeyboardButton();
            showQrButton.setText("📱 Показать QR");
            showQrButton.setCallbackData("show_qr:" + cardId);
            row1.add(showQrButton);
        }

        InlineKeyboardButton generateCardButton = new InlineKeyboardButton();
        generateCardButton.setText("🎨 Карточка");
        generateCardButton.setCallbackData("generate_card:" + cardId);
        row1.add(generateCardButton);

        // ⚡ Вторая строка - Операции с баллами
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton addPointsButton = new InlineKeyboardButton();
        addPointsButton.setText("➕ Начислить баллы");
        addPointsButton.setCallbackData("add_points:" + cardId);
        row2.add(addPointsButton);

        InlineKeyboardButton deductPointsButton = new InlineKeyboardButton();
        deductPointsButton.setText("➖ Списать баллы");
        deductPointsButton.setCallbackData("deduct_points:" + cardId);
        row2.add(deductPointsButton);

        // ⚙️ Третья строка - Управление
        List<InlineKeyboardButton> row3 = new ArrayList<>();
        InlineKeyboardButton editButton = new InlineKeyboardButton();
        editButton.setText("✏️ Редактировать");
        editButton.setCallbackData("edit_card:" + cardId);
        row3.add(editButton);

        InlineKeyboardButton deleteButton = new InlineKeyboardButton();
        deleteButton.setText("🗑️ Удалить");
        deleteButton.setCallbackData("delete_card:" + cardId);
        row3.add(deleteButton);

        // 🔙 Назад
        List<InlineKeyboardButton> row4 = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("◀️ К картам");
        backButton.setCallbackData("my_cards");
        row4.add(backButton);

        if (!row1.isEmpty()) keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);
        keyboard.add(row4);

        markup.setKeyboard(keyboard);
        return markup;
    }

    /**
     * 📊 МЕНЮ СТАТИСТИКИ
     */
    public InlineKeyboardMarkup getStatisticsMenu() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        // 📈 Первая строка - Общая статистика
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton overallButton = new InlineKeyboardButton();
        overallButton.setText("📈 Общая статистика");
        overallButton.setCallbackData("stats_overall");
        row1.add(overallButton);

        // 🏪 Вторая строка - По магазинам
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton byStoreButton = new InlineKeyboardButton();
        byStoreButton.setText("🏪 По магазинам");
        byStoreButton.setCallbackData("stats_by_store");
        row2.add(byStoreButton);

        InlineKeyboardButton byPointsButton = new InlineKeyboardButton();
        byPointsButton.setText("💎 По баллам");
        byPointsButton.setCallbackData("stats_by_points");
        row2.add(byPointsButton);

        // 📅 Третья строка - По времени
        List<InlineKeyboardButton> row3 = new ArrayList<>();
        InlineKeyboardButton weeklyButton = new InlineKeyboardButton();
        weeklyButton.setText("📅 За неделю");
        weeklyButton.setCallbackData("stats_weekly");
        row3.add(weeklyButton);

        InlineKeyboardButton monthlyButton = new InlineKeyboardButton();
        monthlyButton.setText("📆 За месяц");
        monthlyButton.setCallbackData("stats_monthly");
        row3.add(monthlyButton);

        // 🔙 Назад
        List<InlineKeyboardButton> row4 = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("◀️ Главное меню");
        backButton.setCallbackData("main_menu");
        row4.add(backButton);

        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);
        keyboard.add(row4);

        markup.setKeyboard(keyboard);
        return markup;
    }

    /**
     * ⚙️ МЕНЮ НАСТРОЕК
     */
    public InlineKeyboardMarkup getSettingsMenu(boolean notificationsEnabled) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        // 🔔 Первая строка - Уведомления
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton notificationButton = new InlineKeyboardButton();
        notificationButton.setText(notificationsEnabled ? "🔔 Уведомления ВКЛ" : "🔕 Уведомления ВЫКЛ");
        notificationButton.setCallbackData("toggle_notifications");
        row1.add(notificationButton);

        // 🎨 Вторая строка - Внешний вид
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton themeButton = new InlineKeyboardButton();
        themeButton.setText("🎨 Тема карточек");
        themeButton.setCallbackData("card_theme");
        row2.add(themeButton);

        InlineKeyboardButton languageButton = new InlineKeyboardButton();
        languageButton.setText("🌍 Язык");
        languageButton.setCallbackData("language");
        row2.add(languageButton);

        // 💾 Третья строка - Данные
        List<InlineKeyboardButton> row3 = new ArrayList<>();
        InlineKeyboardButton exportButton = new InlineKeyboardButton();
        exportButton.setText("💾 Экспорт данных");
        exportButton.setCallbackData("export_data");
        row3.add(exportButton);

        InlineKeyboardButton importButton = new InlineKeyboardButton();
        importButton.setText("📥 Импорт данных");
        importButton.setCallbackData("import_data");
        row3.add(importButton);

        // 🔙 Назад
        List<InlineKeyboardButton> row4 = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("◀️ Главное меню");
        backButton.setCallbackData("main_menu");
        row4.add(backButton);

        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);
        keyboard.add(row4);

        markup.setKeyboard(keyboard);
        return markup;
    }

    /**
     * ⚡ МЕНЮ ОПЕРАЦИЙ
     */
    public InlineKeyboardMarkup getOperationsMenu() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        // 📱 Первая строка - QR операции
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton scanOpButton = new InlineKeyboardButton();
        scanOpButton.setText("📱 Сканировать операцию");
        scanOpButton.setCallbackData("scan_operation");
        row1.add(scanOpButton);

        // ➕➖ Вторая строка - Ручные операции
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton manualAddButton = new InlineKeyboardButton();
        manualAddButton.setText("➕ Начислить баллы");
        manualAddButton.setCallbackData("manual_add_points");
        row2.add(manualAddButton);

        InlineKeyboardButton manualDeductButton = new InlineKeyboardButton();
        manualDeductButton.setText("➖ Списать баллы");
        manualDeductButton.setCallbackData("manual_deduct_points");
        row2.add(manualDeductButton);

        // 📋 Третья строка - История
        List<InlineKeyboardButton> row3 = new ArrayList<>();
        InlineKeyboardButton historyButton = new InlineKeyboardButton();
        historyButton.setText("📋 История операций");
        historyButton.setCallbackData("operation_history");
        row3.add(historyButton);

        // 🔙 Назад
        List<InlineKeyboardButton> row4 = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("◀️ Главное меню");
        backButton.setCallbackData("main_menu");
        row4.add(backButton);

        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);
        keyboard.add(row4);

        markup.setKeyboard(keyboard);
        return markup;
    }

    /**
     * 🔢 ЧИСЛОВАЯ КЛАВИАТУРА для ввода баллов
     */
    public InlineKeyboardMarkup getNumberKeyboard(String action, UUID cardId) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        // Быстрые значения
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        for (int value : new int[]{50, 100, 250, 500}) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(String.valueOf(value));
            button.setCallbackData(action + ":" + cardId + ":" + value);
            row1.add(button);
        }

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        for (int value : new int[]{1000, 2000, 5000}) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(String.valueOf(value));
            button.setCallbackData(action + ":" + cardId + ":" + value);
            row2.add(button);
        }

        // Ввод вручную
        List<InlineKeyboardButton> row3 = new ArrayList<>();
        InlineKeyboardButton customButton = new InlineKeyboardButton();
        customButton.setText("✏️ Ввести вручную");
        customButton.setCallbackData("custom_" + action + ":" + cardId);
        row3.add(customButton);

        // Отмена
        List<InlineKeyboardButton> row4 = new ArrayList<>();
        InlineKeyboardButton cancelButton = new InlineKeyboardButton();
        cancelButton.setText("❌ Отмена");
        cancelButton.setCallbackData("cancel_operation:" + cardId);
        row4.add(cancelButton);

        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);
        keyboard.add(row4);

        markup.setKeyboard(keyboard);
        return markup;
    }

    /**
     * ✅❌ ПОДТВЕРЖДЕНИЕ действия
     */
    public InlineKeyboardMarkup getConfirmationKeyboard(String action, UUID cardId, String extraData) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> row = new ArrayList<>();

        InlineKeyboardButton confirmButton = new InlineKeyboardButton();
        confirmButton.setText("✅ Подтвердить");
        confirmButton.setCallbackData("confirm_" + action + ":" + cardId + (extraData != null ? ":" + extraData : ""));
        row.add(confirmButton);

        InlineKeyboardButton cancelButton = new InlineKeyboardButton();
        cancelButton.setText("❌ Отмена");
        cancelButton.setCallbackData("cancel_" + action + ":" + cardId);
        row.add(cancelButton);

        keyboard.add(row);
        markup.setKeyboard(keyboard);
        return markup;
    }

    /**
     * 🔄 СОРТИРОВКА карт
     */
    public InlineKeyboardMarkup getSortMenu() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton byDateButton = new InlineKeyboardButton();
        byDateButton.setText("📅 По дате");
        byDateButton.setCallbackData("sort_by_date");
        row1.add(byDateButton);

        InlineKeyboardButton byNameButton = new InlineKeyboardButton();
        byNameButton.setText("🔤 По названию");
        byNameButton.setCallbackData("sort_by_name");
        row1.add(byNameButton);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton byPointsButton = new InlineKeyboardButton();
        byPointsButton.setText("💎 По баллам");
        byPointsButton.setCallbackData("sort_by_points");
        row2.add(byPointsButton);

        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("◀️ Назад");
        backButton.setCallbackData("my_cards");
        row2.add(backButton);

        keyboard.add(row1);
        keyboard.add(row2);
        markup.setKeyboard(keyboard);
        return markup;
    }

    /**
     * 🔙 ПРОСТАЯ кнопка "Назад в главное меню"
     */
    public InlineKeyboardMarkup getBackToMenuKeyboard() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("◀️ Главное меню");
        backButton.setCallbackData("main_menu");
        row.add(backButton);

        keyboard.add(row);
        markup.setKeyboard(keyboard);
        return markup;
    }

    /**
     * 📱 REPLY KEYBOARD для быстрого доступа (если нужно)
     */
    public ReplyKeyboardMarkup getQuickAccessKeyboard() {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();

        List<KeyboardRow> rows = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("📸 QR"));
        row1.add(new KeyboardButton("💳 Карты"));
        row1.add(new KeyboardButton("📊 Статистика"));

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("⚙️ Настройки"));
        row2.add(new KeyboardButton("❓ Помощь"));

        rows.add(row1);
        rows.add(row2);

        keyboard.setKeyboard(rows);
        keyboard.setResizeKeyboard(true);
        keyboard.setOneTimeKeyboard(false);

        return keyboard;
    }
}
