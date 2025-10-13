package com.loyaltycards.telegram.service;

import com.loyaltycards.telegram.entity.LoyaltyCard;
import com.loyaltycards.telegram.entity.TelegramUser;
import com.loyaltycards.telegram.repository.LoyaltyCardRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Service
public class CardService {
    private static final Logger logger = LoggerFactory.getLogger(CardService.class);

    private final LoyaltyCardRepository cardRepository;

    public CardService(LoyaltyCardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    public LoyaltyCard createCard(TelegramUser user, String storeName, String cardNumber) {
        LoyaltyCard card = new LoyaltyCard();
        card.setUser(user);
        card.setStoreName(storeName);
        card.setCardNumber(cardNumber);
        card.setIsActive(true);
        card.setCreatedAt(LocalDateTime.now());
        card.setUpdatedAt(LocalDateTime.now());
        return cardRepository.save(card);
    }

    public List<LoyaltyCard> getUserCards(TelegramUser user) {
        return cardRepository.findByUserAndIsActive(user, true);
    }

    public Long countUserCards(TelegramUser user) {
        return cardRepository.countByUser(user);
    }

    public void deleteCard(UUID cardId) {
        cardRepository.deleteById(cardId);
    }
    // ➕ ДОБАВЬТЕ эти методы в конец CardService:

    /**
     * 🔍 ПОИСК карты по номеру
     */
    public LoyaltyCard findByCardNumber(String cardNumber) {
        return cardRepository.findByCardNumber(cardNumber)
                .orElseThrow(() -> new IllegalArgumentException("Карта не найдена: " + cardNumber));
    }

    /**
     * ➕ НАЧИСЛЕНИЕ баллов на карту
     */
    public LoyaltyCard addPoints(String cardNumber, Integer amount) {
        LoyaltyCard card = findByCardNumber(cardNumber);
        card.addPoints(amount);
        return cardRepository.save(card);
    }

    /**
     * ➖ СПИСАНИЕ баллов с карты
     */
    public LoyaltyCard deductPoints(String cardNumber, Integer amount) throws IllegalStateException {
        LoyaltyCard card = findByCardNumber(cardNumber);
        card.deductPoints(amount);
        return cardRepository.save(card);
    }

    /**
     * 💰 ПОЛУЧЕНИЕ баланса карты
     */
    public Integer getCardBalance(String cardNumber) {
        LoyaltyCard card = findByCardNumber(cardNumber);
        return card.getPoints();
    }
    /**
     * 🔍 ПОИСК карты по ID
     */
    public LoyaltyCard findById(UUID cardId) {
        Optional<LoyaltyCard> card = cardRepository.findById(cardId);
        if (card.isPresent()) {
            logger.debug("🔍 Найдена карта с ID: {}", cardId);
            return card.get();
        } else {
            logger.warn("⚠️ Карта с ID {} не найдена", cardId);
            throw new IllegalArgumentException("Карта с ID " + cardId + " не найдена");
        }
    }

    /**
     * 🗑️ УДАЛЕНИЕ карты по ID (ПЕРЕГРУЗКА существующего метода)
     */
    public void deleteCardById(UUID cardId) {
        try {
            LoyaltyCard card = findById(cardId);
            cardRepository.delete(card);
            logger.info("✅ Карта {} удалена", card.getCardNumber());
        } catch (Exception e) {
            logger.error("❌ Ошибка удаления карты {}: {}", cardId, e.getMessage());
            throw new RuntimeException("Не удалось удалить карту: " + e.getMessage());
        }
    }

}
