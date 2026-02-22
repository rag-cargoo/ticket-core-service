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

    public ConcertOption(Concert concert, LocalDateTime concertDate) {
        this(concert, concertDate, null, null);
    }

    public ConcertOption(Concert concert, LocalDateTime concertDate, Venue venue) {
        this(concert, concertDate, venue, null);
    }

    public ConcertOption(Concert concert, LocalDateTime concertDate, Venue venue, Long ticketPriceAmount) {
        this.concert = concert;
        this.concertDate = concertDate;
        this.venue = venue;
        this.ticketPriceAmount = normalizeTicketPriceAmount(ticketPriceAmount);
    }

    public void updateSchedule(LocalDateTime concertDate, Venue venue) {
        updateSchedule(concertDate, venue, null);
    }

    public void updateSchedule(LocalDateTime concertDate, Venue venue, Long ticketPriceAmount) {
        if (concertDate != null) {
            this.concertDate = concertDate;
        }
        this.venue = venue;
        if (ticketPriceAmount != null) {
            this.ticketPriceAmount = normalizeTicketPriceAmount(ticketPriceAmount);
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
}
