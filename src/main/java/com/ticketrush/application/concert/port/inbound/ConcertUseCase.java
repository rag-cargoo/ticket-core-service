package com.ticketrush.application.concert.port.inbound;

import com.ticketrush.application.concert.model.ConcertOptionResult;
import com.ticketrush.application.concert.model.ConcertResult;
import com.ticketrush.application.concert.model.SeatResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface ConcertUseCase {

    List<ConcertResult> getConcertResults();

    default Page<ConcertResult> searchConcertResults(String keyword, String artistName, Pageable pageable) {
        return searchConcertResults(keyword, artistName, null, pageable);
    }

    Page<ConcertResult> searchConcertResults(String keyword, String artistName, String entertainmentName, Pageable pageable);

    List<ConcertOptionResult> getConcertOptionResults(Long concertId);

    List<SeatResult> getAvailableSeatResults(Long concertOptionId);

    default ConcertResult createConcertResult(String title, String artistName, String entertainmentName) {
        return createConcertResult(title, artistName, entertainmentName, null, null, null, null, null);
    }

    default ConcertResult createConcertResult(
            String title,
            String artistName,
            String entertainmentName,
            String artistDisplayName,
            String artistGenre,
            LocalDate artistDebutDate,
            String entertainmentCountryCode,
            String entertainmentHomepageUrl
    ) {
        return createConcertResult(
                title,
                artistName,
                entertainmentName,
                artistDisplayName,
                artistGenre,
                artistDebutDate,
                entertainmentCountryCode,
                entertainmentHomepageUrl,
                null,
                null,
                null,
                null
        );
    }

    default ConcertResult createConcertResult(
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
        return createConcertResult(
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

    ConcertResult createConcertResult(
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

    default ConcertResult updateConcertResult(
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
        return updateConcertResult(
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

    ConcertResult updateConcertResult(
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

    default ConcertResult createConcertByReferencesResult(String title, Long artistId, Long promoterId) {
        return createConcertByReferencesResult(title, artistId, promoterId, null);
    }

    ConcertResult createConcertByReferencesResult(String title, Long artistId, Long promoterId, String youtubeVideoUrl);

    default ConcertResult updateConcertByReferencesResult(Long concertId, String title, Long artistId, Long promoterId) {
        return updateConcertByReferencesResult(concertId, title, artistId, promoterId, null);
    }

    ConcertResult updateConcertByReferencesResult(
            Long concertId,
            String title,
            Long artistId,
            Long promoterId,
            String youtubeVideoUrl
    );

    ConcertResult getConcertResult(Long concertId);

    default ConcertOptionResult addOptionResult(Long concertId, LocalDateTime date) {
        return addOptionResult(concertId, date, null, null);
    }

    default ConcertOptionResult addOptionResult(Long concertId, LocalDateTime date, Long venueId) {
        return addOptionResult(concertId, date, venueId, null);
    }

    ConcertOptionResult addOptionResult(Long concertId, LocalDateTime date, Long venueId, Long ticketPriceAmount);

    default ConcertOptionResult updateOptionResult(Long optionId, LocalDateTime date, Long venueId) {
        return updateOptionResult(optionId, date, venueId, null);
    }

    ConcertOptionResult updateOptionResult(
            Long optionId,
            LocalDateTime date,
            Long venueId,
            Long ticketPriceAmount
    );

    void deleteOption(Long optionId);

    void createSeats(Long optionId, int count);

    void deleteConcert(Long concertId);

    ConcertResult updateThumbnailResult(Long concertId, byte[] imageBytes, String contentType);

    void deleteThumbnail(Long concertId);

    ConcertThumbnail getThumbnail(Long concertId);

    record ConcertThumbnail(byte[] bytes, String contentType) {
    }
}
