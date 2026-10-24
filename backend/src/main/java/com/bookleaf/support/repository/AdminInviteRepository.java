package com.bookleaf.support.repository;

import com.bookleaf.support.entity.AdminInvite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AdminInviteRepository extends JpaRepository<AdminInvite, Long> {

    @Modifying
    @Query("""
            update AdminInvite invite
            set invite.usedAt = :now
            where invite.tokenHash = :tokenHash
              and invite.email = :email
              and invite.usedAt is null
              and invite.expiresAt > :now
            """)
    int consume(@Param("tokenHash") String tokenHash,
                @Param("email") String email,
                @Param("now") LocalDateTime now);
}
