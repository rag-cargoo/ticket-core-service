package com.ticketrush.domain.concert.entity;

import com.ticketrush.domain.artist.Artist;
import com.ticketrush.domain.promoter.Promoter;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "concerts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Concert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_id")
    private Artist artist;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promoter_id")
    private Promoter promoter;

    @Column(name = "youtube_video_url", length = 500)
    private String youtubeVideoUrl;

    @Column(name = "thumbnail_content_type", length = 100)
    private String thumbnailContentType;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Getter(AccessLevel.NONE)
    @Column(name = "thumbnail_bytes")
    private byte[] thumbnailBytes;

    @Column(name = "thumbnail_updated_at")
    private LocalDateTime thumbnailUpdatedAt;

    @OneToMany(mappedBy = "concert", cascade = CascadeType.ALL)
    private List<ConcertOption> options = new ArrayList<>();

    public Concert(String title, Artist artist) {
        this(title, artist, null, null);
    }

    public Concert(String title, Artist artist, Promoter promoter) {
        this(title, artist, promoter, null);
    }

    public Concert(String title, Artist artist, Promoter promoter, String youtubeVideoUrl) {
        this.title = title;
        this.artist = artist;
        this.promoter = promoter;
        this.youtubeVideoUrl = normalizeOptional(youtubeVideoUrl);
    }

    public void updateBasicInfo(String title, Artist artist, Promoter promoter) {
        if (title != null && !title.trim().isEmpty()) {
            this.title = title.trim();
        }
        if (artist != null) {
            this.artist = artist;
        }
        this.promoter = promoter;
    }

    public void updateYoutubeVideoUrl(String youtubeVideoUrl) {
        this.youtubeVideoUrl = normalizeOptional(youtubeVideoUrl);
    }

    public boolean hasThumbnail() {
        return thumbnailBytes != null && thumbnailBytes.length > 0 && thumbnailContentType != null;
    }

    public byte[] getThumbnailBytesCopy() {
        if (thumbnailBytes == null) {
            return null;
        }
        return Arrays.copyOf(thumbnailBytes, thumbnailBytes.length);
    }

    public void updateThumbnail(byte[] thumbnailBytes, String thumbnailContentType, LocalDateTime thumbnailUpdatedAt) {
        if (thumbnailBytes == null || thumbnailBytes.length == 0) {
            throw new IllegalArgumentException("thumbnail image is empty");
        }
        String normalizedContentType = normalizeRequired(thumbnailContentType, "thumbnailContentType");
        this.thumbnailBytes = Arrays.copyOf(thumbnailBytes, thumbnailBytes.length);
        this.thumbnailContentType = normalizedContentType;
        this.thumbnailUpdatedAt = thumbnailUpdatedAt == null ? LocalDateTime.now() : thumbnailUpdatedAt;
    }

    public void clearThumbnail() {
        this.thumbnailBytes = null;
        this.thumbnailContentType = null;
        this.thumbnailUpdatedAt = null;
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeRequired(String value, String fieldName) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return normalized;
    }
}
