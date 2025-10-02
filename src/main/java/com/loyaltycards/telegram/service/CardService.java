package com.loyaltycards.telegram.service;

import com.loyaltycards.telegram.entity.LoyaltyCard;
import com.loyaltycards.telegram.entity.TelegramUser;
import com.loyaltycards.telegram.repository.LoyaltyCardRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class CardService {

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
}
