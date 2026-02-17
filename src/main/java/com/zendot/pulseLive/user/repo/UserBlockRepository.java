package com.zendot.pulseLive.user.repo;

import com.zendot.pulseLive.user.model.UserBlock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserBlockRepository extends JpaRepository<UserBlock, Long> {

    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    void deleteByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    List<UserBlock> findAllByBlockerId(Long blockerId);

    boolean existsByBlockerIdAndBlockedIdOrBlockerIdAndBlockedId(
            Long a1, Long b1,
            Long a2, Long b2
    );
}

