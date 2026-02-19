package com.ticketrush.domain.concert.entity;

import com.ticketrush.domain.artist.Artist;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.ArrayList;
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

    @Column(name = "youtube_video_url", length = 1000)
    private String youtubeVideoUrl;

    @Column(name = "thumbnail_original_filename", length = 255)
    private String thumbnailOriginalFilename;

    @Column(name = "thumbnail_original_content_type", length = 100)
    private String thumbnailOriginalContentType;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "thumbnail_original_bytes")
    private byte[] thumbnailOriginalBytes;

    @Column(name = "thumbnail_content_type", length = 100)
    private String thumbnailContentType;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "thumbnail_bytes")
    private byte[] thumbnailBytes;

    @OneToMany(mappedBy = "concert", cascade = CascadeType.ALL)
    private List<ConcertOption> options = new ArrayList<>();

    public Concert(String title, Artist artist) {
        this(title, artist, null);
    }

    public Concert(String title, Artist artist, String youtubeVideoUrl) {
        this.title = title;
        this.artist = artist;
        this.youtubeVideoUrl = normalizeNullable(youtubeVideoUrl);
    }

    public void updateInfo(String title, Artist artist, String youtubeVideoUrl) {
        this.title = title;
        this.artist = artist;
        this.youtubeVideoUrl = normalizeNullable(youtubeVideoUrl);
    }

    public void updateThumbnail(
            String originalFilename,
            String originalContentType,
            byte[] originalBytes,
            String thumbnailContentType,
            byte[] thumbnailBytes
    ) {
        if (originalBytes == null || originalBytes.length == 0) {
            throw new IllegalArgumentException("thumbnail original image is required");
        }
        if (thumbnailBytes == null || thumbnailBytes.length == 0) {
            throw new IllegalArgumentException("thumbnail image is required");
        }

        this.thumbnailOriginalFilename = normalizeNullable(originalFilename);
        this.thumbnailOriginalContentType = normalizeNullable(originalContentType);
        this.thumbnailOriginalBytes = Arrays.copyOf(originalBytes, originalBytes.length);
        this.thumbnailContentType = normalizeNullable(thumbnailContentType);
        this.thumbnailBytes = Arrays.copyOf(thumbnailBytes, thumbnailBytes.length);
    }

    public void clearThumbnail() {
        this.thumbnailOriginalFilename = null;
        this.thumbnailOriginalContentType = null;
        this.thumbnailOriginalBytes = null;
        this.thumbnailContentType = null;
        this.thumbnailBytes = null;
    }

    public boolean hasThumbnail() {
        return thumbnailBytes != null && thumbnailBytes.length > 0;
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
