package com.ecardfolio.card;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import com.ecardfolio.account.UserAccountRepository;
import com.ecardfolio.favorite.FavoriteCardRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

// [AI assisted chat-009] AI 協助實作 RESTful CRUD 與 OpenAPI 註解。
@RestController
@RequestMapping("/api/cards")
@CrossOrigin(originPatterns = {"http://localhost:*", "http://127.0.0.1:*", "http://192.168.*.*:*", "http://10.*.*.*:*", "http://172.*.*.*:*"})
@Tag(name = "名片", description = "建立、查詢、更新與刪除數位名片")
public class CardController {
    private final CardRepository cards;
    private final UserAccountRepository users;
    private final FavoriteCardRepository favorites;
    public CardController(CardRepository cards, UserAccountRepository users, FavoriteCardRepository favorites) { this.cards = cards; this.users = users; this.favorites = favorites; }

    @GetMapping @Operation(summary = "列出名片", security = @SecurityRequirement(name = "sessionAuth"))
    public List<BusinessCard> list(Authentication authentication) { return cards.findAllByOwner_EmailOrderByIdDesc(authentication.getName()); }

    @GetMapping("/{id}") @Operation(summary = "取得單張名片")
    public BusinessCard get(@PathVariable Long id) {
        var card = cards.findById(id).orElseThrow(() -> new CardNotFoundException(id));
        if (!card.isPublished()) throw new CardNotFoundException(id);
        return card;
    }

    @PostMapping @ResponseStatus(HttpStatus.CREATED) @Operation(summary = "建立名片", security = @SecurityRequirement(name = "sessionAuth"))
    public BusinessCard create(@Valid @RequestBody CardRequest request, Authentication authentication) {
        var card = request.toEntity();
        card.setOwner(users.findByEmail(authentication.getName()).orElseThrow());
        return cards.save(card);
    }

    @PutMapping("/{id}") @Operation(summary = "修改名片與外觀設定", security = @SecurityRequirement(name = "sessionAuth"))
    public BusinessCard update(@PathVariable Long id, @Valid @RequestBody CardRequest request, Authentication authentication) {
        var card = cards.findByIdAndOwner_Email(id, authentication.getName()).orElseThrow(() -> new CardNotFoundException(id));
        var replacement = request.toEntity();
        replacement.setId(card.getId()); replacement.setCreatedAt(card.getCreatedAt()); replacement.setUpdatedAt(card.getUpdatedAt()); replacement.setOwner(card.getOwner());
        return cards.save(replacement);
    }

    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @Operation(summary = "刪除名片", security = @SecurityRequirement(name = "sessionAuth"))
    public void delete(@PathVariable Long id, Authentication authentication) {
        var card = cards.findByIdAndOwner_Email(id, authentication.getName()).orElseThrow(() -> new CardNotFoundException(id));
        favorites.findAll().stream().filter(favorite -> favorite.getCard().getId().equals(id)).forEach(favorites::delete);
        cards.deleteById(id);
    }
}
