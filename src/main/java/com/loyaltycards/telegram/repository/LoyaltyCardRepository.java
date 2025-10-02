package com.loyaltycards.telegram.repository;

import com.loyaltycards.telegram.entity.LoyaltyCard;
import com.loyaltycards.telegram.entity.TelegramUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface LoyaltyCardRepository extends JpaRepository<LoyaltyCard, UUID> {
    List<LoyaltyCard> findByUserAndIsActive(TelegramUser user, Boolean isActive);
    List<LoyaltyCard> findByUser(TelegramUser user);
    Long countByUser(TelegramUser user);
}
