package com.example.devnote.repository;

import com.example.devnote.entity.WithdrawnUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WithdrawnUserRepository extends JpaRepository<WithdrawnUser, Long> {
    Optional<WithdrawnUser> findByEmail(String email);
}