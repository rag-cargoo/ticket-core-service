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

    @Column(name = "ticket_price_amount")
    private Long ticketPriceAmount;

    public ConcertOption(Concert concert, LocalDateTime concertDate) {
        this(concert, concertDate, null);
    }

    public ConcertOption(Concert concert, LocalDateTime concertDate, Long ticketPriceAmount) {
        this.concert = concert;
        this.concertDate = concertDate;
        this.ticketPriceAmount = ticketPriceAmount;
    }

    public void update(LocalDateTime concertDate, Long ticketPriceAmount) {
        this.concertDate = concertDate;
        this.ticketPriceAmount = ticketPriceAmount;
    }
}
