package com.ecardfolio.card;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CardRepository extends JpaRepository<BusinessCard, Long> {
    java.util.List<BusinessCard> findAllByOwner_EmailOrderByIdDesc(String email);
    java.util.Optional<BusinessCard> findByIdAndOwner_Email(Long id, String email);
    java.util.List<BusinessCard> findAllByOwnerIsNull();
}
