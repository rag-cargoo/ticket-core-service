package com.ticketrush.domain.artist;

import com.ticketrush.domain.agency.Agency;
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
    @JoinColumn(name = "agency_id")
    private Agency agency;

    public Artist(String name, Agency agency) {
        this.name = name;
        this.agency = agency;
    }

    public Artist(String name, Agency agency, String displayName, String genre, LocalDate debutDate) {
        this.name = name;
        this.agency = agency;
        this.displayName = trimToNull(displayName);
        this.genre = trimToNull(genre);
        this.debutDate = debutDate;
    }

    public void updateProfile(Agency agency, String displayName, String genre, LocalDate debutDate) {
        if (agency != null) {
            this.agency = agency;
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
