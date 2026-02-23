package com.ticketrush.application.concert.port.inbound;

import com.ticketrush.application.concert.model.ConcertOptionResult;
import com.ticketrush.application.concert.model.ConcertResult;
import com.ticketrush.application.concert.model.SeatResult;
import com.ticketrush.domain.concert.entity.Concert;
import com.ticketrush.domain.concert.entity.ConcertOption;
import com.ticketrush.domain.concert.entity.Seat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface ConcertUseCase {

    List<Concert> getConcerts();

    default List<ConcertResult> getConcertResults() {
        return getConcerts().stream()
                .map(ConcertResult::from)
                .toList();
    }

    default Page<Concert> searchConcerts(String keyword, String artistName, Pageable pageable) {
        return searchConcerts(keyword, artistName, null, pageable);
    }

    Page<Concert> searchConcerts(String keyword, String artistName, String entertainmentName, Pageable pageable);

    default Page<ConcertResult> searchConcertResults(String keyword, String artistName, Pageable pageable) {
        return searchConcertResults(keyword, artistName, null, pageable);
    }

    default Page<ConcertResult> searchConcertResults(String keyword, String artistName, String entertainmentName, Pageable pageable) {
        return searchConcerts(keyword, artistName, entertainmentName, pageable)
                .map(ConcertResult::from);
    }

    List<ConcertOption> getConcertOptions(Long concertId);

    default List<ConcertOptionResult> getConcertOptionResults(Long concertId) {
        return getConcertOptions(concertId).stream()
                .map(ConcertOptionResult::from)
                .toList();
    }

    List<Seat> getAvailableSeats(Long concertOptionId);

    default List<SeatResult> getAvailableSeatResults(Long concertOptionId) {
        return getAvailableSeats(concertOptionId).stream()
                .map(SeatResult::from)
                .toList();
    }

    default Concert createConcert(String title, String artistName, String entertainmentName) {
        return createConcert(title, artistName, entertainmentName, null, null, null, null, null);
    }

    default ConcertResult createConcertResult(String title, String artistName, String entertainmentName) {
        return ConcertResult.from(createConcert(title, artistName, entertainmentName));
    }

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
        return ConcertResult.from(
                createConcert(
                        title,
                        artistName,
                        entertainmentName,
                        artistDisplayName,
                        artistGenre,
                        artistDebutDate,
                        entertainmentCountryCode,
                        entertainmentHomepageUrl
                )
        );
    }

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
        return ConcertResult.from(
                createConcert(
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
                        promoterHomepageUrl
                )
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
            String promoterHomepageUrl,
            String youtubeVideoUrl
    ) {
        return ConcertResult.from(
                createConcert(
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
                        youtubeVideoUrl
                )
        );
    }

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
            String promoterHomepageUrl,
            String youtubeVideoUrl
    ) {
        return ConcertResult.from(
                updateConcert(
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
                        youtubeVideoUrl
                )
        );
    }

    default Concert createConcertByReferences(String title, Long artistId, Long promoterId) {
        return createConcertByReferences(title, artistId, promoterId, null);
    }

    default ConcertResult createConcertByReferencesResult(String title, Long artistId, Long promoterId, String youtubeVideoUrl) {
        return ConcertResult.from(createConcertByReferences(title, artistId, promoterId, youtubeVideoUrl));
    }

    Concert createConcertByReferences(String title, Long artistId, Long promoterId, String youtubeVideoUrl);

    default Concert updateConcertByReferences(Long concertId, String title, Long artistId, Long promoterId) {
        return updateConcertByReferences(concertId, title, artistId, promoterId, null);
    }

    default ConcertResult updateConcertByReferencesResult(
            Long concertId,
            String title,
            Long artistId,
            Long promoterId,
            String youtubeVideoUrl
    ) {
        return ConcertResult.from(updateConcertByReferences(concertId, title, artistId, promoterId, youtubeVideoUrl));
    }

    Concert updateConcertByReferences(Long concertId, String title, Long artistId, Long promoterId, String youtubeVideoUrl);

    Concert getConcert(Long concertId);

    default ConcertResult getConcertResult(Long concertId) {
        return ConcertResult.from(getConcert(concertId));
    }

    default ConcertOption addOption(Long concertId, LocalDateTime date) {
        return addOption(concertId, date, null, null);
    }

    default ConcertOptionResult addOptionResult(Long concertId, LocalDateTime date) {
        return ConcertOptionResult.from(addOption(concertId, date));
    }

    default ConcertOption addOption(Long concertId, LocalDateTime date, Long venueId) {
        return addOption(concertId, date, venueId, null);
    }

    default ConcertOptionResult addOptionResult(Long concertId, LocalDateTime date, Long venueId) {
        return ConcertOptionResult.from(addOption(concertId, date, venueId));
    }

    ConcertOption addOption(Long concertId, LocalDateTime date, Long venueId, Long ticketPriceAmount);

    default ConcertOptionResult addOptionResult(Long concertId, LocalDateTime date, Long venueId, Long ticketPriceAmount) {
        return ConcertOptionResult.from(addOption(concertId, date, venueId, ticketPriceAmount));
    }

    default ConcertOption updateOption(Long optionId, LocalDateTime date, Long venueId) {
        return updateOption(optionId, date, venueId, null);
    }

    default ConcertOptionResult updateOptionResult(Long optionId, LocalDateTime date, Long venueId) {
        return ConcertOptionResult.from(updateOption(optionId, date, venueId));
    }

    ConcertOption updateOption(Long optionId, LocalDateTime date, Long venueId, Long ticketPriceAmount);

    default ConcertOptionResult updateOptionResult(
            Long optionId,
            LocalDateTime date,
            Long venueId,
            Long ticketPriceAmount
    ) {
        return ConcertOptionResult.from(updateOption(optionId, date, venueId, ticketPriceAmount));
    }

    void deleteOption(Long optionId);

    void createSeats(Long optionId, int count);

    void deleteConcert(Long concertId);

    Concert updateThumbnail(Long concertId, byte[] imageBytes, String contentType);

    default ConcertResult updateThumbnailResult(Long concertId, byte[] imageBytes, String contentType) {
        return ConcertResult.from(updateThumbnail(concertId, imageBytes, contentType));
    }

    void deleteThumbnail(Long concertId);

    ConcertThumbnail getThumbnail(Long concertId);

    record ConcertThumbnail(byte[] bytes, String contentType) {
    }
}
