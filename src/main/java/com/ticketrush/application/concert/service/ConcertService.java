package com.ticketrush.application.concert.service;

import com.ticketrush.application.concert.port.inbound.ConcertUseCase;
import com.ticketrush.domain.concert.entity.Concert;
import com.ticketrush.domain.concert.entity.ConcertOption;
import com.ticketrush.domain.concert.entity.Seat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface ConcertService extends ConcertUseCase {

    List<Concert> getConcerts();

    default Page<Concert> searchConcerts(String keyword, String artistName, Pageable pageable) {
        return searchConcerts(keyword, artistName, null, pageable);
    }

    Page<Concert> searchConcerts(String keyword, String artistName, String entertainmentName, Pageable pageable);

    List<ConcertOption> getConcertOptions(Long concertId);

    List<Seat> getAvailableSeats(Long concertOptionId);

    Concert createConcert(String title, String artistName, String entertainmentName);

    Concert createConcert(
            String title,
            String artistName,
            String entertainmentName,
            String artistDisplayName,
            String artistGenre,
            LocalDate artistDebutDate,
            String entertainmentCountryCode,
            String entertainmentHomepageUrl
    );

    default Concert createConcert(
            String title,
            String artistName,
            String entertainmentName,
            String artistDisplayName,
            String artistGenre,
            LocalDate artistDebutDate,
            String entertainmentCountryCode,
            String entertainmentHomepageUrl,
            String promoterName,
            String promoterCountryCode,
            String promoterHomepageUrl
    ) {
        return createConcert(
                title,
                artistName,
                entertainmentName,
                artistDisplayName,
                artistGenre,
                artistDebutDate,
                entertainmentCountryCode,
                entertainmentHomepageUrl,
                promoterName,
                promoterCountryCode,
                promoterHomepageUrl,
                null
        );
    }

    Concert createConcert(
            String title,
            String artistName,
            String entertainmentName,
            String artistDisplayName,
            String artistGenre,
            LocalDate artistDebutDate,
            String entertainmentCountryCode,
            String entertainmentHomepageUrl,
            String promoterName,
            String promoterCountryCode,
            String promoterHomepageUrl,
            String youtubeVideoUrl
    );

    default Concert updateConcert(
            Long concertId,
            String title,
            String artistName,
            String entertainmentName,
            String artistDisplayName,
            String artistGenre,
            LocalDate artistDebutDate,
            String entertainmentCountryCode,
            String entertainmentHomepageUrl,
            String promoterName,
            String promoterCountryCode,
            String promoterHomepageUrl
    ) {
        return updateConcert(
                concertId,
                title,
                artistName,
                entertainmentName,
                artistDisplayName,
                artistGenre,
                artistDebutDate,
                entertainmentCountryCode,
                entertainmentHomepageUrl,
                promoterName,
                promoterCountryCode,
                promoterHomepageUrl,
                null
        );
    }

    Concert updateConcert(
            Long concertId,
            String title,
            String artistName,
            String entertainmentName,
            String artistDisplayName,
            String artistGenre,
            LocalDate artistDebutDate,
            String entertainmentCountryCode,
            String entertainmentHomepageUrl,
            String promoterName,
            String promoterCountryCode,
            String promoterHomepageUrl,
            String youtubeVideoUrl
    );

    default Concert createConcertByReferences(String title, Long artistId, Long promoterId) {
        return createConcertByReferences(title, artistId, promoterId, null);
    }

    Concert createConcertByReferences(String title, Long artistId, Long promoterId, String youtubeVideoUrl);

    default Concert updateConcertByReferences(Long concertId, String title, Long artistId, Long promoterId) {
        return updateConcertByReferences(concertId, title, artistId, promoterId, null);
    }

    Concert updateConcertByReferences(Long concertId, String title, Long artistId, Long promoterId, String youtubeVideoUrl);

    Concert getConcert(Long concertId);

    default ConcertOption addOption(Long concertId, LocalDateTime date) {
        return addOption(concertId, date, null, null);
    }

    default ConcertOption addOption(Long concertId, LocalDateTime date, Long venueId) {
        return addOption(concertId, date, venueId, null);
    }

    ConcertOption addOption(Long concertId, LocalDateTime date, Long venueId, Long ticketPriceAmount);

    default ConcertOption updateOption(Long optionId, LocalDateTime date, Long venueId) {
        return updateOption(optionId, date, venueId, null);
    }

    ConcertOption updateOption(Long optionId, LocalDateTime date, Long venueId, Long ticketPriceAmount);

    Concert updateThumbnail(Long concertId, byte[] imageBytes, String contentType);

    Seat getSeat(Long seatId);

    Seat getSeatWithPessimisticLock(Long seatId);
}
