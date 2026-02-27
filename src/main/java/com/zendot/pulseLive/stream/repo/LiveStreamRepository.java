package com.zendot.pulseLive.stream.repo;

import com.zendot.pulseLive.stream.model.LiveStream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface LiveStreamRepository extends JpaRepository<LiveStream, String> {

    Optional<LiveStream> findByCallIdAndIsLiveTrue(String callId);

    Optional<LiveStream> findFirstByCreatedByUserIdAndIsLiveTrueOrderByCreatedAtDesc(String createdByUserId);

    Page<LiveStream> findByIsLiveTrueOrderByCreatedAtDesc(Pageable pageable);

    Page<LiveStream> findByIsLiveTrueAndCreatedAtBeforeOrderByCreatedAtDesc(Instant createdAt, Pageable pageable);

    List<LiveStream> findByIsLiveTrueAndUpdatedAtBefore(Instant updatedAt);

    Optional<LiveStream> findFirstByUsernameAndIsLiveTrueOrderByCreatedAtDesc(String username);
}
