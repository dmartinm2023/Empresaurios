package com.example.mustacheapp.repository;

import com.example.mustacheapp.model.Credential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CredentialRepository extends JpaRepository<Credential, Long> {
    List<Credential> findByOwnerUsername(String ownerUsername);
}