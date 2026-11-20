package com.ecardfolio.card;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CardRepository extends JpaRepository<BusinessCard, Long> { }
