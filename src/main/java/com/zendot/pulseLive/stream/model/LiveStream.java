package com.zendot.pulseLive.stream.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "live_streams", indexes = {
        @Index(name = "idx_live_streams_live_created", columnList = "isLive,createdAt"),
        @Index(name = "idx_live_streams_host_live", columnList = "createdByUserId,isLive"),
        @Index(name = "idx_live_streams_username_live", columnList = "username,isLive"),
        @Index(name = "idx_live_streams_live_updated", columnList = "isLive,updatedAt")
})
public class LiveStream {

    @Id
    @Column(nullable = false, length = 100)
    private String callId;

    @Column(nullable = false, length = 100)
    private String callType;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(length = 120)
    private String title;

    @Column(length = 80)
    private String category;

    @Column(length = 500)
    private String profileImage;

    @Column(nullable = false, length = 50)
    private String createdByUserId;

    @Column(nullable = false)
    private Integer participantCount = 1;

    @Column(nullable = false)
    private Long followerCount = 0L;

    @Column(nullable = false)
    private Boolean isLive = true;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
