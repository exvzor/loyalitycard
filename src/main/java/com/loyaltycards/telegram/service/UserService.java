package com.loyaltycards.telegram.service;

import com.loyaltycards.telegram.entity.TelegramUser;
import com.loyaltycards.telegram.repository.TelegramUserRepository;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.User;
import java.time.LocalDateTime;

@Service
public class UserService {

    private final TelegramUserRepository userRepository;

    public UserService(TelegramUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public TelegramUser getOrCreateUser(User telegramUser, Long chatId) {
        return userRepository.findByTelegramId(telegramUser.getId())
                .orElseGet(() -> createNewUser(telegramUser, chatId));
    }

    private TelegramUser createNewUser(User telegramUser, Long chatId) {
        TelegramUser user = new TelegramUser();
        user.setTelegramId(telegramUser.getId());
        user.setUsername(telegramUser.getUserName());
        user.setFirstName(telegramUser.getFirstName());
        user.setLastName(telegramUser.getLastName());
        user.setChatId(chatId);
        user.setIsActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }
}
