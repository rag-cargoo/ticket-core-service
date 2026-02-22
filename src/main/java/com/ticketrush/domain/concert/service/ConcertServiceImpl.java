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
import com.ticketrush.domain.promoter.Promoter;
import com.ticketrush.domain.promoter.PromoterRepository;
import com.ticketrush.domain.venue.Venue;
import com.ticketrush.domain.venue.VenueRepository;
import com.ticketrush.global.cache.ConcertCacheNames;
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
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConcertServiceImpl implements ConcertService {
    private final ConcertRepository concertRepository;
    private final ConcertOptionRepository concertOptionRepository;
    private final SeatRepository seatRepository;
    private final AgencyRepository agencyRepository;
    private final ArtistRepository artistRepository;
    private final PromoterRepository promoterRepository;
    private final VenueRepository venueRepository;

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
        Concert concert = concertRepository.findById(concertId)
                .orElseThrow(() -> new IllegalArgumentException("Concert not found"));
        return concert.getOptions();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = ConcertCacheNames.CONCERT_AVAILABLE_SEATS, key = "#concertOptionId")
    public List<Seat> getAvailableSeats(Long concertOptionId) {
        return seatRepository.findByConcertOptionIdAndStatus(concertOptionId, Seat.SeatStatus.AVAILABLE);
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
        return createConcert(title, artistName, agencyName, null, null, null, null, null, null, null, null, null);
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
                null,
                null,
                null,
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
                                 String promoterName,
                                 String promoterCountryCode,
                                 String promoterHomepageUrl,
                                 String youtubeVideoUrl) {
        String normalizedTitle = normalizeRequired(title, "title");
        Artist artist = resolveArtistByNames(
                artistName,
                agencyName,
                artistDisplayName,
                artistGenre,
                artistDebutDate,
                agencyCountryCode,
                agencyHomepageUrl
        );

        Promoter promoter = resolvePromoter(promoterName, promoterCountryCode, promoterHomepageUrl);
        return concertRepository.save(new Concert(normalizedTitle, artist, promoter, youtubeVideoUrl));
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
                                 String promoterName,
                                 String promoterCountryCode,
                                 String promoterHomepageUrl,
                                 String youtubeVideoUrl) {
        Concert concert = getConcert(concertId);
        String normalizedTitle = normalizeRequired(title, "title");
        Artist artist = resolveArtistByNames(
                artistName,
                agencyName,
                artistDisplayName,
                artistGenre,
                artistDebutDate,
                agencyCountryCode,
                agencyHomepageUrl
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
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_LIST, allEntries = true),
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_OPTIONS, allEntries = true),
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_SEARCH, allEntries = true),
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_AVAILABLE_SEATS, allEntries = true)
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
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_LIST, allEntries = true),
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_OPTIONS, allEntries = true),
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_SEARCH, allEntries = true),
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_AVAILABLE_SEATS, allEntries = true)
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
    @Transactional(readOnly = true)
    public Concert getConcert(Long concertId) {
        return concertRepository.findById(concertId)
                .orElseThrow(() -> new IllegalArgumentException("Concert not found: " + concertId));
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
        return addOption(concertId, date, null, null);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_LIST, allEntries = true),
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_OPTIONS, allEntries = true),
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_SEARCH, allEntries = true),
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_AVAILABLE_SEATS, allEntries = true)
    })
    public ConcertOption addOption(Long concertId, LocalDateTime date, Long venueId) {
        return addOption(concertId, date, venueId, null);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_LIST, allEntries = true),
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_OPTIONS, allEntries = true),
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_SEARCH, allEntries = true),
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_AVAILABLE_SEATS, allEntries = true)
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
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_LIST, allEntries = true),
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_OPTIONS, allEntries = true),
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_SEARCH, allEntries = true),
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_AVAILABLE_SEATS, allEntries = true)
    })
    public ConcertOption updateOption(Long optionId, LocalDateTime date, Long venueId) {
        return updateOption(optionId, date, venueId, null);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_LIST, allEntries = true),
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_OPTIONS, allEntries = true),
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_SEARCH, allEntries = true),
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_AVAILABLE_SEATS, allEntries = true)
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
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_LIST, allEntries = true),
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_OPTIONS, allEntries = true),
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_SEARCH, allEntries = true),
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_AVAILABLE_SEATS, allEntries = true)
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
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_LIST, allEntries = true),
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_SEARCH, allEntries = true)
    })
    public Concert updateThumbnail(Long concertId, byte[] imageBytes, String contentType) {
        Concert concert = getConcert(concertId);
        concert.updateThumbnail(imageBytes, contentType, LocalDateTime.now());
        return concert;
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_LIST, allEntries = true),
            @CacheEvict(cacheNames = ConcertCacheNames.CONCERT_SEARCH, allEntries = true)
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
                                        String agencyName,
                                        String artistDisplayName,
                                        String artistGenre,
                                        LocalDate artistDebutDate,
                                        String agencyCountryCode,
                                        String agencyHomepageUrl) {
        String normalizedArtistName = normalizeRequired(artistName, "artistName");
        String normalizedAgencyName = normalizeRequired(agencyName, "agencyName");

        Agency agency = agencyRepository.findByNameIgnoreCase(normalizedAgencyName)
                .map(existing -> {
                    existing.updateMetadata(agencyCountryCode, agencyHomepageUrl);
                    return existing;
                })
                .orElseGet(() -> agencyRepository.save(new Agency(normalizedAgencyName, agencyCountryCode, agencyHomepageUrl)));

        return artistRepository.findByNameIgnoreCase(normalizedArtistName)
                .map(existing -> {
                    existing.updateProfile(agency, artistDisplayName, artistGenre, artistDebutDate);
                    return existing;
                })
                .orElseGet(() -> artistRepository.save(new Artist(normalizedArtistName, agency, artistDisplayName, artistGenre, artistDebutDate)));
    }
}
