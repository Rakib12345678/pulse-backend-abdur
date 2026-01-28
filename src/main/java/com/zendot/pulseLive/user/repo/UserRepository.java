package com.zendot.pulseLive.user.repo;

import com.zendot.pulseLive.user.model.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByGoogleId(String googleId);
    Optional<User> findByRefreshToken(String refreshToken);
}
