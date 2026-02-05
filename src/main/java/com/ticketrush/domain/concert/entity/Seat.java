package com.ticketrush.domain.concert.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "seats", indexes = {
        @Index(name = "idx_concert_option", columnList = "concert_option_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concert_option_id", nullable = false)
    private ConcertOption concertOption;

    @Column(nullable = false)
    private String seatNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatStatus status;

    @Version // For Optimistic Lock
    private Long version;

    public Seat(ConcertOption concertOption, String seatNumber) {
        this.concertOption = concertOption;
        this.seatNumber = seatNumber;
        this.status = SeatStatus.AVAILABLE;
    }

    public void reserve() {
        if (this.status != SeatStatus.AVAILABLE) {
            throw new IllegalStateException("Seat is already reserved.");
        }
        
        // Race Condition 유도를 위한 인위적 지연 (테스트 시에만 사용)
        try { Thread.sleep(50); } catch (InterruptedException e) {}
        
        this.status = SeatStatus.RESERVED;
    }

    public void cancel() {
        this.status = SeatStatus.AVAILABLE;
    }

    public enum SeatStatus {
        AVAILABLE, RESERVED, TEMP_RESERVED
    }
}
