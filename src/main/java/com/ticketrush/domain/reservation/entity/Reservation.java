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

    private LocalDateTime holdExpiresAt;

    private LocalDateTime confirmedAt;

    private LocalDateTime expiredAt;

    private LocalDateTime cancelledAt;

    private LocalDateTime refundedAt;

    public Reservation(User user, Seat seat) {
        this(user, seat, ReservationStatus.PENDING, LocalDateTime.now(), null);
    }

    private Reservation(User user, Seat seat, ReservationStatus status, LocalDateTime reservedAt, LocalDateTime holdExpiresAt) {
        this.user = user;
        this.seat = seat;
        this.status = status;
        this.reservedAt = reservedAt;
        this.holdExpiresAt = holdExpiresAt;
    }

    public static Reservation hold(User user, Seat seat, LocalDateTime now, LocalDateTime holdExpiresAt) {
        return new Reservation(user, seat, ReservationStatus.HOLD, now, holdExpiresAt);
    }

    public void confirm() {
        this.status = ReservationStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
        this.holdExpiresAt = null;
    }

    public void startPaying(LocalDateTime now) {
        ensureStatus(ReservationStatus.HOLD, "Only HOLD reservation can transition to PAYING.");
        ensureNotExpired(now);
        this.status = ReservationStatus.PAYING;
    }

    public void confirmPayment(LocalDateTime now) {
        ensureStatus(ReservationStatus.PAYING, "Only PAYING reservation can transition to CONFIRMED.");
        ensureNotExpired(now);
        this.status = ReservationStatus.CONFIRMED;
        this.confirmedAt = now;
        this.holdExpiresAt = null;
    }

    public void expire(LocalDateTime now) {
        if (this.status != ReservationStatus.HOLD && this.status != ReservationStatus.PAYING) {
            throw new IllegalStateException("Only HOLD/PAYING reservation can transition to EXPIRED.");
        }
        this.status = ReservationStatus.EXPIRED;
        this.expiredAt = now;
        this.holdExpiresAt = null;
    }

    public void cancel(LocalDateTime now) {
        ensureStatus(ReservationStatus.CONFIRMED, "Only CONFIRMED reservation can transition to CANCELLED.");
        this.status = ReservationStatus.CANCELLED;
        this.cancelledAt = now;
    }

    public void refund(LocalDateTime now) {
        ensureStatus(ReservationStatus.CANCELLED, "Only CANCELLED reservation can transition to REFUNDED.");
        this.status = ReservationStatus.REFUNDED;
        this.refundedAt = now;
    }

    public boolean isExpired(LocalDateTime now) {
        return holdExpiresAt != null && !holdExpiresAt.isAfter(now);
    }

    public boolean isHoldInProgress() {
        return this.status == ReservationStatus.HOLD || this.status == ReservationStatus.PAYING;
    }

    private void ensureStatus(ReservationStatus expected, String message) {
        if (this.status != expected) {
            throw new IllegalStateException(message + " currentStatus=" + this.status);
        }
    }

    private void ensureNotExpired(LocalDateTime now) {
        if (isExpired(now)) {
            throw new IllegalStateException("Reservation hold has expired.");
        }
    }

    public enum ReservationStatus {
        PENDING, HOLD, PAYING, CONFIRMED, EXPIRED, CANCELLED, REFUNDED
    }
}
