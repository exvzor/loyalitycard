package com.loyaltycards.telegram.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "loyalty_cards")
public class LoyaltyCard {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private TelegramUser user;

    @Column(nullable = false)
    private String storeName;

    @Column(nullable = false)
    private String cardNumber;
    private Integer points = 0;


    private String barcode;
    private String description;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public LoyaltyCard() {}

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public TelegramUser getUser() { return user; }
    public void setUser(TelegramUser user) { this.user = user; }

    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }

    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // ➕ ДОБАВЬТЕ эти методы в конец класса:
    public Integer getPoints() { return points; }
    public void setPoints(Integer points) {
        this.points = points;
        this.updatedAt = LocalDateTime.now();
    }

    // 💰 Методы для работы с баллами
    public void addPoints(Integer amount) {
        this.points = (this.points == null ? 0 : this.points) + amount;
        this.updatedAt = LocalDateTime.now();
    }

    public void deductPoints(Integer amount) throws IllegalStateException {
        if (this.points == null || this.points < amount) {
            throw new IllegalStateException("Недостаточно баллов для списания");
        }
        this.points = this.points - amount;
        this.updatedAt = LocalDateTime.now();
    }

}
