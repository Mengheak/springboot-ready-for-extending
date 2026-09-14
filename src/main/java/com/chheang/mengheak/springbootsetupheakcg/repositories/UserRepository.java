package com.chheang.mengheak.springbootsetupheakcg.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chheang.mengheak.springbootsetupheakcg.entities.User;

// Auto-implemented by Spring Data into a bean called userRepository.
// JpaRepository adds paging, sorting, flushing and List-returning finders on top of CrudRepository.
public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
