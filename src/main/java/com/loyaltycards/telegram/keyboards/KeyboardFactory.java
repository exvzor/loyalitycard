package com.loyaltycards.telegram.keyboards;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class KeyboardFactory {

    public InlineKeyboardMarkup getMainMenu() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        // Первая строка
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton addCardButton = new InlineKeyboardButton();
        addCardButton.setText("➕ Добавить карту");
        addCardButton.setCallbackData("add_card");
        row1.add(addCardButton);

        // Вторая строка
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton myCardsButton = new InlineKeyboardButton();
        myCardsButton.setText("💳 Мои карты");
        myCardsButton.setCallbackData("my_cards");
        row2.add(myCardsButton);

        // Третья строка
        List<InlineKeyboardButton> row3 = new ArrayList<>();
        InlineKeyboardButton helpButton = new InlineKeyboardButton();
        helpButton.setText("❓ Помощь");
        helpButton.setCallbackData("help");
        row3.add(helpButton);

        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);

        markup.setKeyboard(keyboard);
        return markup;
    }

    public InlineKeyboardMarkup getCardDetailKeyboard(UUID cardId) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        // Кнопка удаления
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton deleteButton = new InlineKeyboardButton();
        deleteButton.setText("🗑️ Удалить карту");
        deleteButton.setCallbackData("delete_card:" + cardId.toString());
        row1.add(deleteButton);

        // Кнопка назад
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("◀️ Назад к картам");
        backButton.setCallbackData("my_cards");
        row2.add(backButton);

        keyboard.add(row1);
        keyboard.add(row2);

        markup.setKeyboard(keyboard);
        return markup;
    }

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
}
