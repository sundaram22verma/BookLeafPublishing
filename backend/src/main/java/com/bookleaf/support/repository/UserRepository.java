package com.bookleaf.support.repository;

import com.bookleaf.support.entity.User;
import com.bookleaf.support.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<User> findAllByRoleOrderByNameAsc(Role role);
}
