package com.ticketrush.domain.concert.service;

import com.ticketrush.domain.agency.Agency;
import com.ticketrush.domain.agency.AgencyRepository;
import com.ticketrush.domain.artist.Artist;
import com.ticketrush.domain.artist.ArtistRepository;
import com.ticketrush.domain.concert.entity.Concert;
import com.ticketrush.domain.concert.entity.ConcertOption;
import com.ticketrush.domain.concert.entity.Seat;
import com.ticketrush.domain.concert.repository.ConcertOptionRepository;
import com.ticketrush.domain.concert.repository.ConcertRepository;
import com.ticketrush.domain.concert.repository.SeatRepository;
import com.ticketrush.domain.reservation.repository.ReservationRepository;
import com.ticketrush.domain.reservation.repository.SalesPolicyRepository;
import com.ticketrush.global.config.PaymentProperties;
import com.ticketrush.global.cache.ConcertCacheNames;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConcertServiceImpl implements ConcertService {
    private static final long OPEN_SOON_ONE_HOUR_SECONDS = 60L * 60L;
    private static final long OPEN_SOON_FIVE_MINUTES_SECONDS = 5L * 60L;

    private final ConcertRepository concertRepository;
    private final ConcertOptionRepository concertOptionRepository;
    private final SeatRepository seatRepository;
    private final SalesPolicyRepository salesPolicyRepository;
    private final ReservationRepository reservationRepository;
    private final AgencyRepository agencyRepository;
    private final ArtistRepository artistRepository;
    private final PaymentProperties paymentProperties;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = ConcertCacheNames.CONCERT_LIST)
    public List<Concert> getConcerts() {
        return concertRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = ConcertCacheNames.CONCERT_SEARCH,
            key = "#keyword + '|' + #artistName + '|' + #agencyName + '|' + #pageable.pageNumber + '|' + #pageable.pageSize + '|' + #pageable.sort.toString()"
    )
    public Page<Concert> searchConcerts(String keyword, String artistName, String agencyName, Pageable pageable) {
        String normalizedKeyword = normalize(keyword);
        String normalizedArtistName = normalize(artistName);
        String normalizedAgencyName = normalize(agencyName);
        return concertRepository.searchPaged(normalizedKeyword, normalizedArtistName, normalizedAgencyName, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = ConcertCacheNames.CONCERT_OPTIONS, key = "#concertId")
    public List<ConcertOption> getConcertOptions(Long concertId) {
        if (!concertRepository.existsById(concertId)) {
            throw new IllegalArgumentException("Concert not found");
        }
        return concertOptionRepository.findByConcertIdOrderByConcertDateAsc(concertId);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = ConcertCacheNames.CONCERT_AVAILABLE_SEATS, key = "#concertOptionId")
    public List<Seat> getAvailableSeats(Long concertOptionId) {
        return seatRepository.findByConcertOptionIdAndStatus(concertOptionId, Seat.SeatStatus.AVAILABLE);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, ConcertSaleSnapshot> getConcertSaleSnapshots(List<Long> concertIds, LocalDateTime now) {
        if (concertIds == null || concertIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, Long> totalSeatCountByConcert = toSeatCountMap(
                seatRepository.countSeatTotalsByConcertIds(concertIds)
        );
        Map<Long, Long> availableSeatCountByConcert = toSeatCountMap(
                seatRepository.countSeatTotalsByConcertIdsAndStatus(concertIds, Seat.SeatStatus.AVAILABLE)
        );
        Map<Long, LocalDateTime> generalSaleOpenAtByConcert = salesPolicyRepository.findByConcertIdIn(concertIds)
                .stream()
                .collect(Collectors.toMap(
                        policy -> policy.getConcert().getId(),
                        policy -> policy.getGeneralSaleStartAt()
                ));

        Map<Long, ConcertSaleSnapshot> result = new HashMap<>();
        for (Long concertId : concertIds) {
            long totalSeatCount = totalSeatCountByConcert.getOrDefault(concertId, 0L);
            long availableSeatCount = availableSeatCountByConcert.getOrDefault(concertId, 0L);
            LocalDateTime generalSaleStartAt = generalSaleOpenAtByConcert.get(concertId);

            result.put(
                    concertId,
                    deriveSaleSnapshot(generalSaleStartAt, now, totalSeatCount, availableSeatCount)
            );
        }
        return result;
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_LIST, allEntries = true),
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_OPTIONS, allEntries = true),
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_SEARCH, allEntries = true),
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_AVAILABLE_SEATS, allEntries = true)
    })
    public Concert createConcert(String title, String artistName, String agencyName) {
        return createConcert(title, artistName, agencyName, null, null, null, null, null);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_LIST, allEntries = true),
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_OPTIONS, allEntries = true),
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_SEARCH, allEntries = true),
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_AVAILABLE_SEATS, allEntries = true)
    })
    public Concert createConcert(String title,
                                 String artistName,
                                 String agencyName,
                                 String artistDisplayName,
                                 String artistGenre,
                                 LocalDate artistDebutDate,
                                 String agencyCountryCode,
                                 String agencyHomepageUrl) {
        return createConcert(
                title,
                artistName,
                agencyName,
                artistDisplayName,
                artistGenre,
                artistDebutDate,
                agencyCountryCode,
                agencyHomepageUrl,
                null
        );
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_LIST, allEntries = true),
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_OPTIONS, allEntries = true),
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_SEARCH, allEntries = true),
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_AVAILABLE_SEATS, allEntries = true)
    })
    public Concert createConcert(String title,
                                 String artistName,
                                 String agencyName,
                                 String artistDisplayName,
                                 String artistGenre,
                                 LocalDate artistDebutDate,
                                 String agencyCountryCode,
                                 String agencyHomepageUrl,
                                 String youtubeVideoUrl) {
        String normalizedTitle = normalizeRequired(title, "title");
        String normalizedArtistName = normalizeRequired(artistName, "artistName");
        String normalizedAgencyName = normalizeRequired(agencyName, "agencyName");

        Agency agency = upsertAgency(normalizedAgencyName, agencyCountryCode, agencyHomepageUrl);
        Artist artist = upsertArtist(normalizedArtistName, agency, artistDisplayName, artistGenre, artistDebutDate);

        return concertRepository.save(new Concert(normalizedTitle, artist, normalize(youtubeVideoUrl)));
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_LIST, allEntries = true),
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_OPTIONS, allEntries = true),
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_SEARCH, allEntries = true),
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_AVAILABLE_SEATS, allEntries = true)
    })
    public ConcertOption addOption(Long concertId, LocalDateTime date) {
        return addOption(concertId, date, null);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_LIST, allEntries = true),
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_OPTIONS, allEntries = true),
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_SEARCH, allEntries = true),
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_AVAILABLE_SEATS, allEntries = true)
    })
    public ConcertOption addOption(Long concertId, LocalDateTime date, Long ticketPriceAmount) {
        Concert concert = concertRepository.findById(concertId)
                .orElseThrow(() -> new IllegalArgumentException("Concert not found"));
        return concertOptionRepository.save(
                new ConcertOption(concert, requireDate(date), normalizeTicketPrice(ticketPriceAmount))
        );
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_LIST, allEntries = true),
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_OPTIONS, allEntries = true),
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_SEARCH, allEntries = true),
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_AVAILABLE_SEATS, allEntries = true)
    })
    public ConcertOption updateOption(Long optionId, LocalDateTime date, Long ticketPriceAmount) {
        ConcertOption option = concertOptionRepository.findById(optionId)
                .orElseThrow(() -> new IllegalArgumentException("Concert option not found"));
        option.update(requireDate(date), normalizeTicketPrice(ticketPriceAmount));
        return option;
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_LIST, allEntries = true),
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_OPTIONS, allEntries = true),
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_SEARCH, allEntries = true),
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_AVAILABLE_SEATS, allEntries = true)
    })
    public void deleteOption(Long optionId) {
        ConcertOption option = concertOptionRepository.findById(optionId)
                .orElseThrow(() -> new IllegalArgumentException("Concert option not found"));
        if (reservationRepository.existsBySeatConcertOptionId(optionId)) {
            throw new IllegalStateException("Cannot delete concert option with reservations.");
        }
        seatRepository.deleteByConcertOptionId(optionId);
        concertOptionRepository.delete(option);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_LIST, allEntries = true),
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_OPTIONS, allEntries = true),
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_SEARCH, allEntries = true),
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_AVAILABLE_SEATS, allEntries = true)
    })
    public void createSeats(Long optionId, int count) {
        ConcertOption option = concertOptionRepository.findById(optionId)
                .orElseThrow(() -> new IllegalArgumentException("Concert option not found"));
        for (int i = 1; i <= count; i++) {
            seatRepository.save(new Seat(option, "A-" + i));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Concert getConcert(Long concertId) {
        return concertRepository.findById(concertId)
                .orElseThrow(() -> new IllegalArgumentException("Concert not found"));
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_LIST, allEntries = true),
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_OPTIONS, allEntries = true),
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_SEARCH, allEntries = true),
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_AVAILABLE_SEATS, allEntries = true)
    })
    public Concert updateConcert(Long concertId,
                                 String title,
                                 String artistName,
                                 String agencyName,
                                 String artistDisplayName,
                                 String artistGenre,
                                 LocalDate artistDebutDate,
                                 String agencyCountryCode,
                                 String agencyHomepageUrl,
                                 String youtubeVideoUrl) {
        Concert concert = concertRepository.findById(concertId)
                .orElseThrow(() -> new IllegalArgumentException("Concert not found"));

        String normalizedTitle = normalizeRequired(title, "title");
        String normalizedArtistName = normalizeRequired(artistName, "artistName");
        String normalizedAgencyName = normalizeRequired(agencyName, "agencyName");

        Agency agency = upsertAgency(normalizedAgencyName, agencyCountryCode, agencyHomepageUrl);
        Artist artist = upsertArtist(normalizedArtistName, agency, artistDisplayName, artistGenre, artistDebutDate);
        concert.updateInfo(normalizedTitle, artist, normalize(youtubeVideoUrl));

        return concert;
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_LIST, allEntries = true),
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_OPTIONS, allEntries = true),
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_SEARCH, allEntries = true),
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_AVAILABLE_SEATS, allEntries = true)
    })
    public Concert updateConcertThumbnail(Long concertId,
                                          String originalFilename,
                                          String originalContentType,
                                          byte[] originalBytes,
                                          String thumbnailContentType,
                                          byte[] thumbnailBytes) {
        Concert concert = concertRepository.findById(concertId)
                .orElseThrow(() -> new IllegalArgumentException("Concert not found"));
        concert.updateThumbnail(originalFilename, originalContentType, originalBytes, thumbnailContentType, thumbnailBytes);
        return concert;
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_LIST, allEntries = true),
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_OPTIONS, allEntries = true),
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_SEARCH, allEntries = true),
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_AVAILABLE_SEATS, allEntries = true)
    })
    public void clearConcertThumbnail(Long concertId) {
        Concert concert = concertRepository.findById(concertId)
                .orElseThrow(() -> new IllegalArgumentException("Concert not found"));
        concert.clearThumbnail();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] getConcertThumbnailBytes(Long concertId) {
        Concert concert = concertRepository.findById(concertId)
                .orElseThrow(() -> new IllegalArgumentException("Concert not found"));
        if (!concert.hasThumbnail()) {
            return null;
        }
        byte[] bytes = concert.getThumbnailBytes();
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        return bytes.clone();
    }

    @Override
    @Transactional(readOnly = true)
    public String getConcertThumbnailContentType(Long concertId) {
        Concert concert = concertRepository.findById(concertId)
                .orElseThrow(() -> new IllegalArgumentException("Concert not found"));
        return normalize(concert.getThumbnailContentType());
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_LIST, allEntries = true),
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_OPTIONS, allEntries = true),
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_SEARCH, allEntries = true),
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_AVAILABLE_SEATS, allEntries = true)
    })
    public void deleteConcert(Long concertId) {
        concertRepository.deleteById(concertId);
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

    private Map<Long, Long> toSeatCountMap(List<SeatRepository.ConcertSeatCountProjection> rows) {
        return rows.stream().collect(Collectors.toMap(
                SeatRepository.ConcertSeatCountProjection::getConcertId,
                row -> row.getSeatCount() == null ? 0L : row.getSeatCount()
        ));
    }

    private ConcertSaleSnapshot deriveSaleSnapshot(LocalDateTime generalSaleStartAt,
                                                   LocalDateTime now,
                                                   long totalSeatCount,
                                                   long availableSeatCount) {
        boolean hasSeats = totalSeatCount > 0;
        boolean soldOut = hasSeats && availableSeatCount == 0;
        if (soldOut) {
            return new ConcertSaleSnapshot(
                    ConcertSaleStatus.SOLD_OUT,
                    generalSaleStartAt,
                    null,
                    true,
                    false,
                    availableSeatCount,
                    totalSeatCount
            );
        }

        if (generalSaleStartAt == null) {
            return new ConcertSaleSnapshot(
                    ConcertSaleStatus.UNSCHEDULED,
                    null,
                    null,
                    false,
                    false,
                    availableSeatCount,
                    totalSeatCount
            );
        }

        if (!now.isBefore(generalSaleStartAt)) {
            return new ConcertSaleSnapshot(
                    ConcertSaleStatus.OPEN,
                    generalSaleStartAt,
                    0L,
                    true,
                    true,
                    availableSeatCount,
                    totalSeatCount
            );
        }

        long opensInSeconds = Math.max(0L, Duration.between(now, generalSaleStartAt).getSeconds());
        if (opensInSeconds <= OPEN_SOON_FIVE_MINUTES_SECONDS) {
            return new ConcertSaleSnapshot(
                    ConcertSaleStatus.OPEN_SOON_5M,
                    generalSaleStartAt,
                    opensInSeconds,
                    true,
                    false,
                    availableSeatCount,
                    totalSeatCount
            );
        }
        if (opensInSeconds <= OPEN_SOON_ONE_HOUR_SECONDS) {
            return new ConcertSaleSnapshot(
                    ConcertSaleStatus.OPEN_SOON_1H,
                    generalSaleStartAt,
                    opensInSeconds,
                    true,
                    false,
                    availableSeatCount,
                    totalSeatCount
            );
        }

        return new ConcertSaleSnapshot(
                ConcertSaleStatus.PREOPEN,
                generalSaleStartAt,
                opensInSeconds,
                false,
                false,
                availableSeatCount,
                totalSeatCount
        );
    }

    private Agency upsertAgency(String agencyName, String agencyCountryCode, String agencyHomepageUrl) {
        return agencyRepository.findByNameIgnoreCase(agencyName)
                .map(existing -> {
                    existing.updateMetadata(agencyCountryCode, agencyHomepageUrl);
                    return existing;
                })
                .orElseGet(() -> agencyRepository.save(new Agency(agencyName, agencyCountryCode, agencyHomepageUrl)));
    }

    private Artist upsertArtist(String artistName,
                                Agency agency,
                                String artistDisplayName,
                                String artistGenre,
                                LocalDate artistDebutDate) {
        return artistRepository.findByNameIgnoreCase(artistName)
                .map(existing -> {
                    existing.updateProfile(agency, artistDisplayName, artistGenre, artistDebutDate);
                    return existing;
                })
                .orElseGet(() -> artistRepository.save(new Artist(artistName, agency, artistDisplayName, artistGenre, artistDebutDate)));
    }

    private LocalDateTime requireDate(LocalDateTime date) {
        if (date == null) {
            throw new IllegalArgumentException("concertDate is required");
        }
        return date;
    }

    private Long normalizeTicketPrice(Long ticketPriceAmount) {
        long resolved = ticketPriceAmount != null ? ticketPriceAmount : paymentProperties.getDefaultTicketPriceAmount();
        if (resolved <= 0) {
            throw new IllegalArgumentException("ticketPriceAmount must be positive");
        }
        return resolved;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeRequired(String value, String fieldName) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return normalized;
    }
}
