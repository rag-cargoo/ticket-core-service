package com.ticketrush.application.concert.model;

import com.ticketrush.domain.concert.entity.Concert;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.net.URI;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.regex.Pattern;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ConcertResult {
    private static final Pattern YOUTUBE_VIDEO_ID_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{11}$");

    private Long id;
    private String title;
    private String artistName;
    private Long artistId;
    private String artistDisplayName;
    private String artistGenre;
    private LocalDate artistDebutDate;
    private String entertainmentName;
    private String entertainmentCountryCode;
    private String entertainmentHomepageUrl;
    private String youtubeVideoUrl;
    private String thumbnailUrl;
    private Long promoterId;
    private String promoterName;
    private String promoterCountryCode;
    private String promoterHomepageUrl;

    public static ConcertResult from(Concert concert) {
        var artist = concert.getArtist();
        var entertainment = artist.getEntertainment();
        var promoter = concert.getPromoter();
        return new ConcertResult(
                concert.getId(),
                concert.getTitle(),
                artist.getName(),
                artist.getId(),
                artist.getDisplayName(),
                artist.getGenre(),
                artist.getDebutDate(),
                entertainment != null ? entertainment.getName() : null,
                entertainment != null ? entertainment.getCountryCode() : null,
                entertainment != null ? entertainment.getHomepageUrl() : null,
                concert.getYoutubeVideoUrl(),
                resolveThumbnailUrl(concert),
                promoter != null ? promoter.getId() : null,
                promoter != null ? promoter.getName() : null,
                promoter != null ? promoter.getCountryCode() : null,
                promoter != null ? promoter.getHomepageUrl() : null
        );
    }

    private static String resolveThumbnailUrl(Concert concert) {
        if (concert.hasThumbnail()) {
            String base = "/api/concerts/" + concert.getId() + "/thumbnail";
            if (concert.getThumbnailUpdatedAt() == null) {
                return base;
            }
            long ts = concert.getThumbnailUpdatedAt().atZone(ZoneOffset.UTC).toInstant().toEpochMilli();
            return base + "?ts=" + ts;
        }
        return resolveYoutubeThumbnailUrl(concert.getYoutubeVideoUrl());
    }

    private static String resolveYoutubeThumbnailUrl(String youtubeVideoUrl) {
        String videoId = extractYoutubeVideoId(youtubeVideoUrl);
        if (videoId == null) {
            return null;
        }
        return "https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg";
    }

    private static String extractYoutubeVideoId(String youtubeVideoUrl) {
        if (youtubeVideoUrl == null || youtubeVideoUrl.isBlank()) {
            return null;
        }
        try {
            URI uri = URI.create(youtubeVideoUrl.trim());
            String host = uri.getHost();
            if (host == null) {
                return null;
            }
            String normalizedHost = host.toLowerCase(Locale.ROOT);
            if (normalizedHost.startsWith("www.")) {
                normalizedHost = normalizedHost.substring(4);
            }

            String path = uri.getPath() == null ? "" : uri.getPath();
            if ("youtu.be".equals(normalizedHost)) {
                return validateYoutubeVideoId(firstPathSegment(path));
            }

            if (!normalizedHost.endsWith("youtube.com") && !normalizedHost.endsWith("youtube-nocookie.com")) {
                return null;
            }

            if ("/watch".equals(path)) {
                String value = extractQueryParam(uri.getQuery(), "v");
                return validateYoutubeVideoId(value);
            }
            if (path.startsWith("/embed/")) {
                return validateYoutubeVideoId(firstPathSegment(path.substring("/embed/".length())));
            }
            if (path.startsWith("/shorts/")) {
                return validateYoutubeVideoId(firstPathSegment(path.substring("/shorts/".length())));
            }
            if (path.startsWith("/live/")) {
                return validateYoutubeVideoId(firstPathSegment(path.substring("/live/".length())));
            }
        } catch (RuntimeException ignored) {
            return null;
        }
        return null;
    }

    private static String firstPathSegment(String path) {
        String normalized = path == null ? "" : path.trim();
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        int slashIndex = normalized.indexOf('/');
        return slashIndex >= 0 ? normalized.substring(0, slashIndex) : normalized;
    }

    private static String extractQueryParam(String query, String key) {
        if (query == null || query.isBlank()) {
            return null;
        }
        for (String part : query.split("&")) {
            int separatorIndex = part.indexOf('=');
            if (separatorIndex <= 0) {
                continue;
            }
            String paramKey = part.substring(0, separatorIndex);
            if (!paramKey.equals(key)) {
                continue;
            }
            return part.substring(separatorIndex + 1);
        }
        return null;
    }

    private static String validateYoutubeVideoId(String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return null;
        }
        String normalized = candidate;
        int ampersandIndex = normalized.indexOf('&');
        if (ampersandIndex >= 0) {
            normalized = normalized.substring(0, ampersandIndex);
        }
        int questionIndex = normalized.indexOf('?');
        if (questionIndex >= 0) {
            normalized = normalized.substring(0, questionIndex);
        }
        if (!YOUTUBE_VIDEO_ID_PATTERN.matcher(normalized).matches()) {
            return null;
        }
        return normalized;
    }
}
