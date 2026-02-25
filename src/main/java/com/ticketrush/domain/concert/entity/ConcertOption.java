package com.ticketrush.domain.concert.entity;

import com.ticketrush.domain.venue.Venue;
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

    private static final int DEFAULT_MAX_SEATS_PER_ORDER = 2;
    private static final int MAX_ALLOWED_SEATS_PER_ORDER = 10;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concert_id", nullable = false)
    private Concert concert;

    @Column(nullable = false)
    private LocalDateTime concertDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id")
    private Venue venue;

    @Column(name = "ticket_price_amount")
    private Long ticketPriceAmount;

    @Column(nullable = false, columnDefinition = "integer default 2")
    private int maxSeatsPerOrder = DEFAULT_MAX_SEATS_PER_ORDER;

    public ConcertOption(Concert concert, LocalDateTime concertDate) {
        this(concert, concertDate, null, null, null);
    }

    public ConcertOption(Concert concert, LocalDateTime concertDate, Venue venue) {
        this(concert, concertDate, venue, null, null);
    }

    public ConcertOption(Concert concert, LocalDateTime concertDate, Venue venue, Long ticketPriceAmount) {
        this(concert, concertDate, venue, ticketPriceAmount, null);
    }

    public ConcertOption(
            Concert concert,
            LocalDateTime concertDate,
            Venue venue,
            Long ticketPriceAmount,
            Integer maxSeatsPerOrder
    ) {
        this.concert = concert;
        this.concertDate = concertDate;
        this.venue = venue;
        this.ticketPriceAmount = normalizeTicketPriceAmount(ticketPriceAmount);
        this.maxSeatsPerOrder = normalizeMaxSeatsPerOrder(maxSeatsPerOrder);
    }

    public void updateSchedule(LocalDateTime concertDate, Venue venue) {
        updateSchedule(concertDate, venue, null, null);
    }

    public void updateSchedule(LocalDateTime concertDate, Venue venue, Long ticketPriceAmount) {
        updateSchedule(concertDate, venue, ticketPriceAmount, null);
    }

    public void updateSchedule(LocalDateTime concertDate, Venue venue, Long ticketPriceAmount, Integer maxSeatsPerOrder) {
        if (concertDate != null) {
            this.concertDate = concertDate;
        }
        this.venue = venue;
        if (ticketPriceAmount != null) {
            this.ticketPriceAmount = normalizeTicketPriceAmount(ticketPriceAmount);
        }
        if (maxSeatsPerOrder != null) {
            this.maxSeatsPerOrder = normalizeMaxSeatsPerOrder(maxSeatsPerOrder);
        }
    }

    private Long normalizeTicketPriceAmount(Long ticketPriceAmount) {
        if (ticketPriceAmount == null) {
            return null;
        }
        if (ticketPriceAmount < 0L) {
            throw new IllegalArgumentException("ticketPriceAmount must be greater than or equal to 0");
        }
        return ticketPriceAmount;
    }

    private int normalizeMaxSeatsPerOrder(Integer maxSeatsPerOrder) {
        if (maxSeatsPerOrder == null) {
            return DEFAULT_MAX_SEATS_PER_ORDER;
        }
        if (maxSeatsPerOrder < 1) {
            throw new IllegalArgumentException("maxSeatsPerOrder must be greater than or equal to 1");
        }
        if (maxSeatsPerOrder > MAX_ALLOWED_SEATS_PER_ORDER) {
            throw new IllegalArgumentException("maxSeatsPerOrder must be less than or equal to " + MAX_ALLOWED_SEATS_PER_ORDER);
        }
        return maxSeatsPerOrder;
    }
}
