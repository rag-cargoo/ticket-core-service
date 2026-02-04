package com.ticketrush.domain.reservation.entity;

import com.ticketrush.domain.concert.entity.Seat;
import com.ticketrush.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "reservations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;

    @Column(nullable = false)
    private LocalDateTime reservedAt;

    public Reservation(User user, Seat seat) {
        this.user = user;
        this.seat = seat;
        this.status = ReservationStatus.PENDING;
        this.reservedAt = LocalDateTime.now();
    }

    public void confirm() {
        this.status = ReservationStatus.CONFIRMED;
    }

    public enum ReservationStatus {
        PENDING, CONFIRMED, CANCELLED
    }
}
