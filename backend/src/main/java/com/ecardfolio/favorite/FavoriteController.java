package com.ecardfolio.favorite;

import com.ecardfolio.account.UserAccountRepository;
import com.ecardfolio.card.BusinessCard;
import com.ecardfolio.card.CardNotFoundException;
import com.ecardfolio.card.CardRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

// [AI assisted chat-015] 登入使用者的跨裝置收藏 API。
@RestController
@RequestMapping("/api/favorites")
@Tag(name = "收藏", description = "登入使用者的名片收藏清單")
@SecurityRequirement(name = "sessionAuth")
public class FavoriteController {
    private final FavoriteCardRepository favorites;
    private final UserAccountRepository users;
    private final CardRepository cards;
    public FavoriteController(FavoriteCardRepository favorites, UserAccountRepository users, CardRepository cards) {
        this.favorites = favorites; this.users = users; this.cards = cards;
    }
    @GetMapping @Operation(summary = "列出目前帳號的收藏名片")
    public List<BusinessCard> list(Authentication authentication) {
        return favorites.findAllByUserEmailOrderByCreatedAtDesc(authentication.getName()).stream()
            .map(FavoriteCard::getCard).filter(BusinessCard::isPublished).toList();
    }
    @PostMapping("/{cardId}") @ResponseStatus(HttpStatus.CREATED) @Operation(summary = "收藏一張公開名片")
    public BusinessCard add(@PathVariable Long cardId, Authentication authentication) {
        var card = cards.findById(cardId).filter(BusinessCard::isPublished).orElseThrow(() -> new CardNotFoundException(cardId));
        if (favorites.findByUserEmailAndCardId(authentication.getName(), cardId).isEmpty()) {
            var favorite = new FavoriteCard();
            favorite.setUser(users.findByEmail(authentication.getName()).orElseThrow());
            favorite.setCard(card);
            favorites.save(favorite);
        }
        return card;
    }
    @DeleteMapping("/{cardId}") @ResponseStatus(HttpStatus.NO_CONTENT) @Operation(summary = "移除目前帳號的收藏")
    public void remove(@PathVariable Long cardId, Authentication authentication) {
        favorites.findByUserEmailAndCardId(authentication.getName(), cardId).ifPresent(favorites::delete);
    }
}
