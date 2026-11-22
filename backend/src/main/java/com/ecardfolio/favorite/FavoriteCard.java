package com.ecardfolio.favorite;

import com.ecardfolio.account.UserAccount;
import com.ecardfolio.card.BusinessCard;
import jakarta.persistence.*;
import java.time.Instant;

// [AI assisted chat-015] 使用者收藏名片的持久化關聯。
@Entity
@Table(name = "favorite_cards", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "card_id"}))
public class FavoriteCard {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private UserAccount user;
    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "card_id", nullable = false) private BusinessCard card;
    @Column(nullable = false, updatable = false) private Instant createdAt = Instant.now();
    public Long getId() { return id; }
    public UserAccount getUser() { return user; }
    public void setUser(UserAccount user) { this.user = user; }
    public BusinessCard getCard() { return card; }
    public void setCard(BusinessCard card) { this.card = card; }
    public Instant getCreatedAt() { return createdAt; }
}
