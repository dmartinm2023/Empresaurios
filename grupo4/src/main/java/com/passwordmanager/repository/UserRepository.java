package com.passwordmanager.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.passwordmanager.model;

public interface UserRepository extends JpaRepository<User, Long> {

}
