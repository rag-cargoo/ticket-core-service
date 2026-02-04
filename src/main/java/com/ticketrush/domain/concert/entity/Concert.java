package com.ticketrush.domain.concert.entity;

import com.ticketrush.domain.artist.Artist;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    @OneToMany(mappedBy = "concert", cascade = CascadeType.ALL)
    private List<ConcertOption> options = new ArrayList<>();

    public Concert(String title, Artist artist) {
        this.title = title;
        this.artist = artist;
    }
}
