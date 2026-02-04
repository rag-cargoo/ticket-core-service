package com.ticketrush.domain.concert.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "concert_options")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConcertOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concert_id", nullable = false)
    private Concert concert;

    @Column(nullable = false)
    private LocalDateTime concertDate;

    public ConcertOption(Concert concert, LocalDateTime concertDate) {
        this.concert = concert;
        this.concertDate = concertDate;
    }
}
