package com.ticketrush.application.concert.service;

import com.ticketrush.application.concert.model.ConcertOptionResult;
import com.ticketrush.application.concert.model.ConcertResult;
import com.ticketrush.application.concert.model.ConcertHighlightItemResult;
import com.ticketrush.application.concert.model.ConcertHighlightsResult;
import com.ticketrush.application.concert.model.SeatResult;
import com.ticketrush.domain.entertainment.Entertainment;
import com.ticketrush.domain.entertainment.EntertainmentRepository;
import com.ticketrush.domain.artist.Artist;
import com.ticketrush.domain.artist.ArtistRepository;
import com.ticketrush.domain.concert.entity.Concert;
import com.ticketrush.domain.concert.entity.ConcertOption;
import com.ticketrush.domain.concert.entity.Seat;
import com.ticketrush.domain.concert.repository.ConcertSeatStatsProjection;
import com.ticketrush.domain.concert.repository.ConcertOptionRepository;
import com.ticketrush.domain.concert.repository.ConcertRepository;
import com.ticketrush.domain.concert.repository.SeatRepository;
import com.ticketrush.domain.promoter.Promoter;
import com.ticketrush.domain.promoter.PromoterRepository;
import com.ticketrush.domain.reservation.entity.SalesPolicy;
import com.ticketrush.domain.reservation.repository.SalesPolicyRepository;
import com.ticketrush.domain.venue.Venue;
import com.ticketrush.domain.venue.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ConcertServiceImpl implements ConcertService {
    private static final String CACHE_CONCERT_LIST = "concert:list";
    private static final String CACHE_CONCERT_SEARCH = "concert:search";
    private static final String CACHE_CONCERT_OPTIONS = "concert:options";
    private static final String CACHE_CONCERT_AVAILABLE_SEATS = "concert:available-seats";
    private static final ZoneId KST_ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final int DEFAULT_HIGHLIGHT_LIMIT = 3;
    private static final int MAX_HIGHLIGHT_LIMIT = 20;
    private static final int OPENING_SOON_WITHIN_HOURS = 12;
    private static final long OPENING_SOON_WITHIN_SECONDS = OPENING_SOON_WITHIN_HOURS * 3600L;
    private static final int SELL_OUT_RISK_SEAT_THRESHOLD = 30;
    private static final double SELL_OUT_RISK_RATIO_THRESHOLD = 0.18d;

    private final ConcertRepository concertRepository;
    private final ConcertOptionRepository concertOptionRepository;
    private final SeatRepository seatRepository;
    private final EntertainmentRepository entertainmentRepository;
    private final ArtistRepository artistRepository;
    private final PromoterRepository promoterRepository;
    private final VenueRepository venueRepository;
    private final SalesPolicyRepository salesPolicyRepository;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CACHE_CONCERT_LIST)
    public List<Concert> getConcerts() {
        return concertRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CACHE_CONCERT_LIST)
    public List<ConcertResult> getConcertResults() {
        return getConcerts().stream()
                .map(ConcertResult::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ConcertHighlightsResult getConcertHighlights(int openingSoonLimit, int sellOutRiskLimit) {
        int normalizedOpeningSoonLimit = normalizeHighlightLimit(openingSoonLimit);
        int normalizedSellOutRiskLimit = normalizeHighlightLimit(sellOutRiskLimit);

        List<Concert> concerts = concertRepository.findAllWithArtistAndEntertainment();
        if (concerts.isEmpty()) {
            return new ConcertHighlightsResult(
                    List.of(),
                    List.of(),
                    LocalDateTime.now(),
                    OPENING_SOON_WITHIN_HOURS,
                    SELL_OUT_RISK_SEAT_THRESHOLD,
                    SELL_OUT_RISK_RATIO_THRESHOLD
            );
        }

        List<Long> concertIds = concerts.stream()
                .map(Concert::getId)
                .toList();
        Map<Long, SalesPolicy> salesPolicyByConcertId = toSalesPolicyMap(salesPolicyRepository.findByConcertIdIn(concertIds));
        Map<Long, SeatStats> seatStatsByConcertId = toSeatStatsMap(
                seatRepository.findSeatStatsByConcertIds(concertIds, Seat.SeatStatus.AVAILABLE)
        );

        ZonedDateTime nowKst = ZonedDateTime.now(KST_ZONE_ID);
        LocalDate todayKst = nowKst.toLocalDate();
        LocalDateTime now = nowKst.toLocalDateTime();

        List<ConcertHighlightCandidate> openingSoonCandidates = concerts.stream()
                .map(concert -> toHighlightCandidate(concert, salesPolicyByConcertId, seatStatsByConcertId, now))
                .filter(candidate -> candidate != null && candidate.saleOpensInSeconds > 0)
                .filter(candidate -> candidate.saleOpensInSeconds <= OPENING_SOON_WITHIN_SECONDS)
                .filter(candidate -> isSameKstDate(candidate.saleOpensAt, todayKst))
                .sorted(Comparator
                        .comparingLong((ConcertHighlightCandidate candidate) -> candidate.saleOpensInSeconds)
                        .thenComparing(candidate -> candidate.concert.getTitle(), String.CASE_INSENSITIVE_ORDER))
                .limit(normalizedOpeningSoonLimit)
                .toList();

        List<ConcertHighlightCandidate> sellOutRiskCandidates = concerts.stream()
                .map(concert -> toHighlightCandidate(concert, salesPolicyByConcertId, seatStatsByConcertId, now))
                .filter(candidate -> candidate != null && candidate.saleOpensInSeconds <= 0)
                .filter(candidate -> isSellOutRisk(candidate.availableSeatCount, candidate.totalSeatCount))
                .sorted(Comparator
                        .comparingDouble((ConcertHighlightCandidate candidate) -> candidate.remainingRatio)
                        .thenComparingLong(candidate -> candidate.availableSeatCount)
                        .thenComparing(candidate -> candidate.concert.getTitle(), String.CASE_INSENSITIVE_ORDER))
                .limit(normalizedSellOutRiskLimit)
                .toList();

        return new ConcertHighlightsResult(
                openingSoonCandidates.stream().map(this::toHighlightItem).toList(),
                sellOutRiskCandidates.stream().map(this::toHighlightItem).toList(),
                now,
                OPENING_SOON_WITHIN_HOURS,
                SELL_OUT_RISK_SEAT_THRESHOLD,
                SELL_OUT_RISK_RATIO_THRESHOLD
        );
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = CACHE_CONCERT_SEARCH,
            key = "#keyword + '|' + #artistName + '|' + #entertainmentName + '|' + #pageable.pageNumber + '|' + #pageable.pageSize + '|' + #pageable.sort.toString()"
    )
    public Page<Concert> searchConcerts(String keyword, String artistName, String entertainmentName, Pageable pageable) {
        String normalizedKeyword = normalize(keyword);
        String keywordPattern = toLowerContainsPattern(normalizedKeyword);
        Long keywordId = parseKeywordId(normalizedKeyword);
        String normalizedArtistName = normalizeLower(artistName);
        String normalizedEntertainmentName = normalizeLower(entertainmentName);
        return concertRepository.searchPaged(
                keywordPattern,
                keywordId,
                normalizedArtistName,
                normalizedEntertainmentName,
                pageable
        );
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = CACHE_CONCERT_SEARCH,
            key = "#keyword + '|' + #artistName + '|' + #entertainmentName + '|' + #pageable.pageNumber + '|' + #pageable.pageSize + '|' + #pageable.sort.toString()"
    )
    public Page<ConcertResult> searchConcertResults(String keyword, String artistName, String entertainmentName, Pageable pageable) {
        return searchConcerts(keyword, artistName, entertainmentName, pageable)
                .map(ConcertResult::from);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CACHE_CONCERT_OPTIONS, key = "#concertId")
    public List<ConcertOption> getConcertOptions(Long concertId) {
        Concert concert = concertRepository.findById(concertId)
                .orElseThrow(() -> new IllegalArgumentException("Concert not found"));
        return concert.getOptions();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CACHE_CONCERT_OPTIONS, key = "#concertId")
    public List<ConcertOptionResult> getConcertOptionResults(Long concertId) {
        return getConcertOptions(concertId).stream()
                .map(ConcertOptionResult::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CACHE_CONCERT_AVAILABLE_SEATS, key = "#concertOptionId")
    public List<Seat> getAvailableSeats(Long concertOptionId) {
        return seatRepository.findByConcertOptionIdAndStatus(concertOptionId, Seat.SeatStatus.AVAILABLE);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CACHE_CONCERT_AVAILABLE_SEATS, key = "#concertOptionId")
    public List<SeatResult> getAvailableSeatResults(Long concertOptionId) {
        return getAvailableSeats(concertOptionId).stream()
                .map(SeatResult::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SeatResult> getSeatMapResults(Long concertOptionId, List<String> statuses) {
        Set<Seat.SeatStatus> targetStatuses = parseSeatStatuses(statuses);
        List<Seat> seats = targetStatuses.isEmpty()
                ? seatRepository.findByConcertOptionIdOrderBySeatNumberAsc(concertOptionId)
                : seatRepository.findByConcertOptionIdAndStatusInOrderBySeatNumberAsc(concertOptionId, targetStatuses);
        return seats.stream()
                .map(SeatResult::from)
                .toList();
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CACHE_CONCERT_LIST, allEntries = true),
            @CacheEvict(cacheNames = CACHE_CONCERT_OPTIONS, allEntries = true),
            @CacheEvict(cacheNames = CACHE_CONCERT_SEARCH, allEntries = true),
            @CacheEvict(cacheNames = CACHE_CONCERT_AVAILABLE_SEATS, allEntries = true)
    })
    public Concert createConcert(String title, String artistName, String entertainmentName) {
        return createConcert(title, artistName, entertainmentName, null, null, null, null, null, null, null, null, null);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CACHE_CONCERT_LIST, allEntries = true),
            @CacheEvict(cacheNames = CACHE_CONCERT_OPTIONS, allEntries = true),
            @CacheEvict(cacheNames = CACHE_CONCERT_SEARCH, allEntries = true),
            @CacheEvict(cacheNames = CACHE_CONCERT_AVAILABLE_SEATS, allEntries = true)
    })
    public Concert createConcert(String title,
                                 String artistName,
                                 String entertainmentName,
                                 String artistDisplayName,
                                 String artistGenre,
                                 LocalDate artistDebutDate,
                                 String entertainmentCountryCode,
                                 String entertainmentHomepageUrl) {
        return createConcert(
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

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CACHE_CONCERT_LIST, allEntries = true),
            @CacheEvict(cacheNames = CACHE_CONCERT_OPTIONS, allEntries = true),
            @CacheEvict(cacheNames = CACHE_CONCERT_SEARCH, allEntries = true),
            @CacheEvict(cacheNames = CACHE_CONCERT_AVAILABLE_SEATS, allEntries = true)
    })
    public Concert createConcert(String title,
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
                                 String youtubeVideoUrl) {
        String normalizedTitle = normalizeRequired(title, "title");
        Artist artist = resolveArtistByNames(
                artistName,
                entertainmentName,
                artistDisplayName,
                artistGenre,
                artistDebutDate,
                entertainmentCountryCode,
                entertainmentHomepageUrl
        );

        Promoter promoter = resolvePromoter(promoterName, promoterCountryCode, promoterHomepageUrl);
        return concertRepository.save(new Concert(normalizedTitle, artist, promoter, youtubeVideoUrl));
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CACHE_CONCERT_LIST, allEntries = true),
            @CacheEvict(cacheNames = CACHE_CONCERT_OPTIONS, allEntries = true),
            @CacheEvict(cacheNames = CACHE_CONCERT_SEARCH, allEntries = true),
            @CacheEvict(cacheNames = CACHE_CONCERT_AVAILABLE_SEATS, allEntries = true)
    })
    public ConcertResult createConcertResult(
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

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CACHE_CONCERT_LIST, allEntries = true),
            @CacheEvict(cacheNames = CACHE_CONCERT_OPTIONS, allEntries = true),
            @CacheEvict(cacheNames = CACHE_CONCERT_SEARCH, allEntries = true),
            @CacheEvict(cacheNames = CACHE_CONCERT_AVAILABLE_SEATS, allEntries = true)
    })
    public Concert updateConcert(Long concertId,
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
                                 String youtubeVideoUrl) {
        Concert concert = getConcert(concertId);
        String normalizedTitle = normalizeRequired(title, "title");
        Artist artist = resolveArtistByNames(
                artistName,
                entertainmentName,
                artistDisplayName,
                artistGenre,
                artistDebutDate,
                entertainmentCountryCode,
                entertainmentHomepageUrl
        );
        Promoter promoter = normalize(promoterName) == null
                ? concert.getPromoter()
                : resolvePromoter(promoterName, promoterCountryCode, promoterHomepageUrl);
        concert.updateBasicInfo(normalizedTitle, artist, promoter);
        concert.updateYoutubeVideoUrl(youtubeVideoUrl);
        return concert;
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CACHE_CONCERT_LIST, allEntries = true),
            @CacheEvict(cacheNames = CACHE_CONCERT_OPTIONS, allEntries = true),
            @CacheEvict(cacheNames = CACHE_CONCERT_SEARCH, allEntries = true),
            @CacheEvict(cacheNames = CACHE_CONCERT_AVAILABLE_SEATS, allEntries = true)
    })
    public ConcertResult updateConcertResult(
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

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CACHE_CONCERT_LIST, allEntries = true),
            @CacheEvict(cacheNames = CACHE_CONCERT_OPTIONS, allEntries = true),
            @CacheEvict(cacheNames = CACHE_CONCERT_SEARCH, allEntries = true),
            @CacheEvict(cacheNames = CACHE_CONCERT_AVAILABLE_SEATS, allEntries = true)
    })
    public Concert createConcertByReferences(String title, Long artistId, Long promoterId, String youtubeVideoUrl) {
        String normalizedTitle = normalizeRequired(title, "title");
        if (artistId == null) {
            throw new IllegalArgumentException("artistId is required");
        }

        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> new IllegalArgumentException("Artist not found: " + artistId));
        Promoter promoter = resolvePromoterById(promoterId);
        return concertRepository.save(new Concert(normalizedTitle, artist, promoter, youtubeVideoUrl));
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CACHE_CONCERT_LIST, allEntries = true),
            @CacheEvict(cacheNames = CACHE_CONCERT_OPTIONS, allEntries = true),
            @CacheEvict(cacheNames = CACHE_CONCERT_SEARCH, allEntries = true),
            @CacheEvict(cacheNames = CACHE_CONCERT_AVAILABLE_SEATS, allEntries = true)
    })
    public ConcertResult createConcertByReferencesResult(String title, Long artistId, Long promoterId, String youtubeVideoUrl) {
        return ConcertResult.from(createConcertByReferences(title, artistId, promoterId, youtubeVideoUrl));
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CACHE_CONCERT_LIST, allEntries = true),
            @CacheEvict(cacheNames = CACHE_CONCERT_OPTIONS, allEntries = true),
            @CacheEvict(cacheNames = CACHE_CONCERT_SEARCH, allEntries = true),
            @CacheEvict(cacheNames = CACHE_CONCERT_AVAILABLE_SEATS, allEntries = true)
    })
    public Concert updateConcertByReferences(
            Long concertId,
            String title,
            Long artistId,
            Long promoterId,
            String youtubeVideoUrl
    ) {
        Concert concert = getConcert(concertId);
        String normalizedTitle = normalizeRequired(title, "title");
        if (artistId == null) {
            throw new IllegalArgumentException("artistId is required");
        }
        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> new IllegalArgumentException("Artist not found: " + artistId));
        Promoter promoter = promoterId == null ? concert.getPromoter() : resolvePromoterById(promoterId);
        concert.updateBasicInfo(normalizedTitle, artist, promoter);
        concert.updateYoutubeVideoUrl(youtubeVideoUrl);
        return concert;
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CACHE_CONCERT_LIST, allEntries = true),
            @CacheEvict(cacheNames = CACHE_CONCERT_OPTIONS, allEntries = true),
            @CacheEvict(cacheNames = CACHE_CONCERT_SEARCH, allEntries = true),
            @CacheEvict(cacheNames = CACHE_CONCERT_AVAILABLE_SEATS, allEntries = true)
    })
    public ConcertResult updateConcertByReferencesResult(
            Long concertId,
            String title,
            Long artistId,
            Long promoterId,
            String youtubeVideoUrl
    ) {
        return ConcertResult.from(updateConcertByReferences(concertId, title, artistId, promoterId, youtubeVideoUrl));
    }

    @Override
    @Transactional(readOnly = true)
    public Concert getConcert(Long concertId) {
        return concertRepository.findById(concertId)
                .orElseThrow(() -> new IllegalArgumentException("Concert not found: " + concertId));
    }

    @Override
    @Transactional(readOnly = true)
    public ConcertResult getConcertResult(Long concertId) {
        return ConcertResult.from(getConcert(concertId));
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CACHE_CONCERT_LIST, allEntries = true),
            @CacheEvict(cacheNames = CACHE_CONCERT_OPTIONS, allEntries = true),
            @CacheEvict(cacheNames = CACHE_CONCERT_SEARCH, allEntries = true),
            @CacheEvict(cacheNames = CACHE_CONCERT_AVAILABLE_SEATS, allEntries = true)
    })
    public ConcertOption addOption(Long concertId, LocalDateTime date) {
        return addOption(concertId, date, null, null);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CACHE_CONCERT_LIST, allEntries = true),
            @CacheEvict(cacheNames = CACHE_CONCERT_OPTIONS, allEntries = true),
            @CacheEvict(cacheNames = CACHE_CONCERT_SEARCH, allEntries = true),
            @CacheEvict(cacheNames = CACHE_CONCERT_AVAILABLE_SEATS, allEntries = true)
    })
    public ConcertOption addOption(Long concertId, LocalDateTime date, Long venueId) {
        return addOption(concertId, date, venueId, null);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CACHE_CONCERT_LIST, allEntries = true),
            @CacheEvict(cacheNames = CACHE_CONCERT_OPTIONS, allEntries = true),
            @CacheEvict(cacheNames = CACHE_CONCERT_SEARCH, allEntries = true),
            @CacheEvict(cacheNames = CACHE_CONCERT_AVAILABLE_SEATS, allEntries = true)
    })
    public ConcertOption addOption(Long concertId, LocalDateTime date, Long venueId, Long ticketPriceAmount) {
        Concert concert = getConcert(concertId);
        Venue venue = resolveVenueById(venueId);
        Long normalizedTicketPriceAmount = normalizeTicketPriceAmount(ticketPriceAmount);
        return concertOptionRepository.save(new ConcertOption(concert, date, venue, normalizedTicketPriceAmount));
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CACHE_CONCERT_LIST, allEntries = true),
            @CacheEvict(cacheNames = CACHE_CONCERT_OPTIONS, allEntries = true),
            @CacheEvict(cacheNames = CACHE_CONCERT_SEARCH, allEntries = true),
            @CacheEvict(cacheNames = CACHE_CONCERT_AVAILABLE_SEATS, allEntries = true)
    })
    public ConcertOptionResult addOptionResult(Long concertId, LocalDateTime date, Long venueId, Long ticketPriceAmount) {
        return ConcertOptionResult.from(addOption(concertId, date, venueId, ticketPriceAmount));
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CACHE_CONCERT_LIST, allEntries = true),
            @CacheEvict(cacheNames = CACHE_CONCERT_OPTIONS, allEntries = true),
            @CacheEvict(cacheNames = CACHE_CONCERT_SEARCH, allEntries = true),
            @CacheEvict(cacheNames = CACHE_CONCERT_AVAILABLE_SEATS, allEntries = true)
    })
    public ConcertOption updateOption(Long optionId, LocalDateTime date, Long venueId) {
        return updateOption(optionId, date, venueId, null);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CACHE_CONCERT_LIST, allEntries = true),
            @CacheEvict(cacheNames = CACHE_CONCERT_OPTIONS, allEntries = true),
            @CacheEvict(cacheNames = CACHE_CONCERT_SEARCH, allEntries = true),
            @CacheEvict(cacheNames = CACHE_CONCERT_AVAILABLE_SEATS, allEntries = true)
    })
    public ConcertOption updateOption(Long optionId, LocalDateTime date, Long venueId, Long ticketPriceAmount) {
        ConcertOption option = concertOptionRepository.findById(optionId)
                .orElseThrow(() -> new IllegalArgumentException("Concert option not found: " + optionId));
        Venue venue = venueId == null ? option.getVenue() : resolveVenueById(venueId);
        Long resolvedTicketPriceAmount = ticketPriceAmount == null
                ? option.getTicketPriceAmount()
                : normalizeTicketPriceAmount(ticketPriceAmount);
        option.updateSchedule(date, venue, resolvedTicketPriceAmount);
        return option;
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CACHE_CONCERT_LIST, allEntries = true),
            @CacheEvict(cacheNames = CACHE_CONCERT_OPTIONS, allEntries = true),
            @CacheEvict(cacheNames = CACHE_CONCERT_SEARCH, allEntries = true),
            @CacheEvict(cacheNames = CACHE_CONCERT_AVAILABLE_SEATS, allEntries = true)
    })
    public ConcertOptionResult updateOptionResult(Long optionId, LocalDateTime date, Long venueId, Long ticketPriceAmount) {
        return ConcertOptionResult.from(updateOption(optionId, date, venueId, ticketPriceAmount));
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CACHE_CONCERT_LIST, allEntries = true),
            @CacheEvict(cacheNames = CACHE_CONCERT_OPTIONS, allEntries = true),
            @CacheEvict(cacheNames = CACHE_CONCERT_SEARCH, allEntries = true),
            @CacheEvict(cacheNames = CACHE_CONCERT_AVAILABLE_SEATS, allEntries = true)
    })
    public void deleteOption(Long optionId) {
        if (!concertOptionRepository.existsById(optionId)) {
            throw new IllegalArgumentException("Concert option not found: " + optionId);
        }
        if (seatRepository.existsByConcertOptionId(optionId)) {
            throw new IllegalStateException("Concert option is referenced by seats: " + optionId);
        }
        concertOptionRepository.deleteById(optionId);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CACHE_CONCERT_LIST, allEntries = true),
            @CacheEvict(cacheNames = CACHE_CONCERT_OPTIONS, allEntries = true),
            @CacheEvict(cacheNames = CACHE_CONCERT_SEARCH, allEntries = true),
            @CacheEvict(cacheNames = CACHE_CONCERT_AVAILABLE_SEATS, allEntries = true)
    })
    public void createSeats(Long optionId, int count) {
        ConcertOption option = concertOptionRepository.findById(optionId)
                .orElseThrow(() -> new IllegalArgumentException("Concert option not found"));
        for (int i = 1; i <= count; i++) {
            seatRepository.save(new Seat(option, "A-" + i));
        }
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CACHE_CONCERT_LIST, allEntries = true),
            @CacheEvict(cacheNames = CACHE_CONCERT_OPTIONS, allEntries = true),
            @CacheEvict(cacheNames = CACHE_CONCERT_SEARCH, allEntries = true),
            @CacheEvict(cacheNames = CACHE_CONCERT_AVAILABLE_SEATS, allEntries = true)
    })
    public void deleteConcert(Long concertId) {
        concertRepository.deleteById(concertId);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CACHE_CONCERT_LIST, allEntries = true),
            @CacheEvict(cacheNames = CACHE_CONCERT_SEARCH, allEntries = true)
    })
    public Concert updateThumbnail(Long concertId, byte[] imageBytes, String contentType) {
        Concert concert = getConcert(concertId);
        concert.updateThumbnail(imageBytes, contentType, LocalDateTime.now());
        return concert;
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CACHE_CONCERT_LIST, allEntries = true),
            @CacheEvict(cacheNames = CACHE_CONCERT_SEARCH, allEntries = true)
    })
    public ConcertResult updateThumbnailResult(Long concertId, byte[] imageBytes, String contentType) {
        return ConcertResult.from(updateThumbnail(concertId, imageBytes, contentType));
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CACHE_CONCERT_LIST, allEntries = true),
            @CacheEvict(cacheNames = CACHE_CONCERT_SEARCH, allEntries = true)
    })
    public void deleteThumbnail(Long concertId) {
        Concert concert = getConcert(concertId);
        concert.clearThumbnail();
    }

    @Override
    @Transactional(readOnly = true)
    public ConcertThumbnail getThumbnail(Long concertId) {
        Concert concert = getConcert(concertId);
        if (!concert.hasThumbnail()) {
            throw new IllegalArgumentException("thumbnail not found for concert: " + concertId);
        }
        byte[] imageBytes = concert.getThumbnailBytesCopy();
        if (imageBytes == null || imageBytes.length == 0) {
            throw new IllegalArgumentException("thumbnail not found for concert: " + concertId);
        }
        return new ConcertThumbnail(imageBytes, concert.getThumbnailContentType());
    }

    @Override
    @Transactional(readOnly = true)
    public Seat getSeat(Long seatId) {
        return seatRepository.findById(seatId)
                .orElseThrow(() -> new IllegalArgumentException("Seat not found"));
    }

    @Override
    @Transactional
    public Seat getSeatWithPessimisticLock(Long seatId) {
        return seatRepository.findByIdWithPessimisticLock(seatId)
                .orElseThrow(() -> new IllegalArgumentException("Seat not found"));
    }

    private Map<Long, SalesPolicy> toSalesPolicyMap(Collection<SalesPolicy> policies) {
        Map<Long, SalesPolicy> result = new HashMap<>();
        for (SalesPolicy policy : policies) {
            if (policy.getConcert() == null || policy.getConcert().getId() == null) {
                continue;
            }
            result.put(policy.getConcert().getId(), policy);
        }
        return result;
    }

    private Map<Long, SeatStats> toSeatStatsMap(Collection<ConcertSeatStatsProjection> rows) {
        Map<Long, SeatStats> result = new HashMap<>();
        for (ConcertSeatStatsProjection row : rows) {
            Long concertId = row.getConcertId();
            if (concertId == null) {
                continue;
            }
            result.put(
                    concertId,
                    new SeatStats(
                            row.getTotalSeatCount() == null ? 0L : Math.max(0L, row.getTotalSeatCount()),
                            row.getAvailableSeatCount() == null ? 0L : Math.max(0L, row.getAvailableSeatCount())
                    )
            );
        }
        return result;
    }

    private ConcertHighlightCandidate toHighlightCandidate(
            Concert concert,
            Map<Long, SalesPolicy> salesPolicyByConcertId,
            Map<Long, SeatStats> seatStatsByConcertId,
            LocalDateTime now
    ) {
        Long concertId = concert.getId();
        if (concertId == null) {
            return null;
        }
        SalesPolicy salesPolicy = salesPolicyByConcertId.get(concertId);
        if (salesPolicy == null || salesPolicy.getGeneralSaleStartAt() == null) {
            return null;
        }
        SeatStats seatStats = seatStatsByConcertId.getOrDefault(concertId, SeatStats.EMPTY);
        long totalSeatCount = seatStats.totalSeatCount;
        long availableSeatCount = seatStats.availableSeatCount;
        if (totalSeatCount <= 0 || availableSeatCount <= 0) {
            return null;
        }
        LocalDateTime saleOpensAt = salesPolicy.getGeneralSaleStartAt();
        long saleOpensInSeconds = ChronoUnit.SECONDS.between(now, saleOpensAt);
        double remainingRatio = totalSeatCount == 0 ? 0d : (double) availableSeatCount / (double) totalSeatCount;
        return new ConcertHighlightCandidate(
                concert,
                saleOpensAt,
                saleOpensInSeconds,
                availableSeatCount,
                totalSeatCount,
                remainingRatio
        );
    }

    private boolean isSellOutRisk(long availableSeatCount, long totalSeatCount) {
        if (availableSeatCount <= 0 || totalSeatCount <= 0) {
            return false;
        }
        double remainingRatio = (double) availableSeatCount / (double) totalSeatCount;
        return availableSeatCount <= SELL_OUT_RISK_SEAT_THRESHOLD || remainingRatio <= SELL_OUT_RISK_RATIO_THRESHOLD;
    }

    private boolean isSameKstDate(LocalDateTime target, LocalDate kstDate) {
        return target != null && target.atZone(KST_ZONE_ID).toLocalDate().isEqual(kstDate);
    }

    private ConcertHighlightItemResult toHighlightItem(ConcertHighlightCandidate candidate) {
        long ratioPercent = Math.round(candidate.remainingRatio * 100d);
        int clampedRatioPercent = (int) Math.max(0L, Math.min(100L, ratioPercent));
        return new ConcertHighlightItemResult(
                candidate.concert.getId(),
                candidate.concert.getTitle(),
                candidate.concert.getArtist().getName(),
                candidate.concert.getArtist().getEntertainment() == null
                        ? null
                        : candidate.concert.getArtist().getEntertainment().getName(),
                candidate.saleOpensAt,
                candidate.saleOpensInSeconds,
                candidate.availableSeatCount,
                candidate.totalSeatCount,
                clampedRatioPercent
        );
    }

    private int normalizeHighlightLimit(int candidate) {
        if (candidate <= 0) {
            return DEFAULT_HIGHLIGHT_LIMIT;
        }
        return Math.min(candidate, MAX_HIGHLIGHT_LIMIT);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Set<Seat.SeatStatus> parseSeatStatuses(List<String> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return Set.of();
        }
        Set<Seat.SeatStatus> parsed = new LinkedHashSet<>();
        for (String status : statuses) {
            String normalized = normalize(status);
            if (normalized == null) {
                continue;
            }
            try {
                parsed.add(Seat.SeatStatus.valueOf(normalized.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException error) {
                throw new IllegalArgumentException("Unsupported seat status filter: " + status, error);
            }
        }
        return parsed;
    }

    private String normalizeLower(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return null;
        }
        return normalized.toLowerCase(Locale.ROOT);
    }

    private String toLowerContainsPattern(String value) {
        if (value == null) {
            return null;
        }
        return "%" + value.toLowerCase(Locale.ROOT) + "%";
    }

    private Long parseKeywordId(String keyword) {
        if (keyword == null) {
            return null;
        }
        try {
            return Long.valueOf(keyword);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
    private String normalizeRequired(String value, String fieldName) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return normalized;
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

    private Promoter resolvePromoter(String promoterName, String promoterCountryCode, String promoterHomepageUrl) {
        String normalizedPromoterName = normalize(promoterName);
        if (normalizedPromoterName == null) {
            return null;
        }
        return promoterRepository.findByNameIgnoreCase(normalizedPromoterName)
                .map(existing -> {
                    existing.update(normalizedPromoterName, promoterCountryCode, promoterHomepageUrl);
                    return existing;
                })
                .orElseGet(() -> promoterRepository.save(
                        new Promoter(normalizedPromoterName, promoterCountryCode, promoterHomepageUrl)
                ));
    }

    private Promoter resolvePromoterById(Long promoterId) {
        if (promoterId == null) {
            return null;
        }
        return promoterRepository.findById(promoterId)
                .orElseThrow(() -> new IllegalArgumentException("Promoter not found: " + promoterId));
    }

    private Venue resolveVenueById(Long venueId) {
        if (venueId == null) {
            return null;
        }
        return venueRepository.findById(venueId)
                .orElseThrow(() -> new IllegalArgumentException("Venue not found: " + venueId));
    }

    private Artist resolveArtistByNames(String artistName,
                                        String entertainmentName,
                                        String artistDisplayName,
                                        String artistGenre,
                                        LocalDate artistDebutDate,
                                        String entertainmentCountryCode,
                                        String entertainmentHomepageUrl) {
        String normalizedArtistName = normalizeRequired(artistName, "artistName");
        String normalizedEntertainmentName = normalizeRequired(entertainmentName, "entertainmentName");

        Entertainment entertainment = entertainmentRepository.findByNameIgnoreCase(normalizedEntertainmentName)
                .map(existing -> {
                    existing.updateMetadata(entertainmentCountryCode, entertainmentHomepageUrl);
                    return existing;
                })
                .orElseGet(() -> entertainmentRepository.save(new Entertainment(normalizedEntertainmentName, entertainmentCountryCode, entertainmentHomepageUrl)));

        return artistRepository.findByNameIgnoreCase(normalizedArtistName)
                .map(existing -> {
                    existing.updateProfile(entertainment, artistDisplayName, artistGenre, artistDebutDate);
                    return existing;
                })
                .orElseGet(() -> artistRepository.save(new Artist(normalizedArtistName, entertainment, artistDisplayName, artistGenre, artistDebutDate)));
    }

    private record SeatStats(long totalSeatCount, long availableSeatCount) {
        private static final SeatStats EMPTY = new SeatStats(0L, 0L);
    }

    private record ConcertHighlightCandidate(
            Concert concert,
            LocalDateTime saleOpensAt,
            long saleOpensInSeconds,
            long availableSeatCount,
            long totalSeatCount,
            double remainingRatio
    ) {
    }
}
