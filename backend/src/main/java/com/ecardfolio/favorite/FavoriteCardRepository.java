package com.ecardfolio.favorite;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FavoriteCardRepository extends JpaRepository<FavoriteCard, Long> {
    List<FavoriteCard> findAllByUserEmailOrderByCreatedAtDesc(String email);
    Optional<FavoriteCard> findByUserEmailAndCardId(String email, Long cardId);
}
