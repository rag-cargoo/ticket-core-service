package com.ticketrush.domain.artist;

import com.ticketrush.domain.entertainment.Entertainment;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@Table(name = "artists")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Artist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name; // e.g., "BTS", "NewJeans"

    @Column(name = "display_name", length = 100)
    private String displayName;

    @Column(length = 100)
    private String genre;

    @Column(name = "debut_date")
    private LocalDate debutDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entertainment_id")
    private Entertainment entertainment;

    public Artist(String name, Entertainment entertainment) {
        this.name = name;
        this.entertainment = entertainment;
    }

    public Artist(String name, Entertainment entertainment, String displayName, String genre, LocalDate debutDate) {
        this.name = name;
        this.entertainment = entertainment;
        this.displayName = trimToNull(displayName);
        this.genre = trimToNull(genre);
        this.debutDate = debutDate;
    }

    public void rename(String name) {
        String normalized = trimToNull(name);
        if (normalized == null) {
            throw new IllegalArgumentException("name is required");
        }
        this.name = normalized;
    }

    public void updateProfile(Entertainment entertainment, String displayName, String genre, LocalDate debutDate) {
        if (entertainment != null) {
            this.entertainment = entertainment;
        }
        if (displayName != null) {
            this.displayName = trimToNull(displayName);
        }
        if (genre != null) {
            this.genre = trimToNull(genre);
        }
        if (debutDate != null) {
            this.debutDate = debutDate;
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
