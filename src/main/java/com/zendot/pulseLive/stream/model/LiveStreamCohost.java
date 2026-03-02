package com.zendot.pulseLive.stream.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "live_stream_cohosts",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_live_stream_cohost_call_username", columnNames = {"callId", "username"})
        },
        indexes = {
                @Index(name = "idx_live_stream_cohost_call", columnList = "callId")
        }
)
public class LiveStreamCohost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String callId;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(length = 500)
    private String profileImage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CohostStatus status = CohostStatus.ACTIVE;
}
