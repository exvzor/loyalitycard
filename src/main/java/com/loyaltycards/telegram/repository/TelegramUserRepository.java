package com.loyaltycards.telegram.repository;

import com.loyaltycards.telegram.entity.TelegramUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TelegramUserRepository extends JpaRepository<TelegramUser, UUID> {
    Optional<TelegramUser> findByTelegramId(Long telegramId);
    Optional<TelegramUser> findByChatId(Long chatId);
}
