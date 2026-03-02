package com.zendot.pulseLive.stream.repo;

import com.zendot.pulseLive.stream.model.CohostStatus;
import com.zendot.pulseLive.stream.model.LiveStreamCohost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LiveStreamCohostRepository extends JpaRepository<LiveStreamCohost, Long> {
    Optional<LiveStreamCohost> findByCallIdAndUsername(String callId, String username);

    long countByCallIdAndStatus(String callId, CohostStatus status);

    List<LiveStreamCohost> findByCallIdOrderByIdAsc(String callId);

    List<LiveStreamCohost> findByCallIdIn(List<String> callIds);
}
