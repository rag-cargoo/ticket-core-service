package com.ticketrush.application.concert.model;

import com.ticketrush.domain.artist.Artist;
import com.ticketrush.domain.concert.entity.Concert;
import com.ticketrush.domain.entertainment.Entertainment;
import com.ticketrush.domain.promoter.Promoter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ConcertResultTest {

    @Test
    @DisplayName("썸네일 업로드가 없으면 유튜브 URL에서 썸네일 URL을 생성한다")
    void buildsYoutubeThumbnailUrlWhenNoUploadedThumbnail() {
        Concert concert = newConcert("https://www.youtube.com/watch?v=3JZ_D3ELwOQ");

        ConcertResult result = ConcertResult.from(concert);

        assertThat(result.getThumbnailUrl()).isEqualTo("https://img.youtube.com/vi/3JZ_D3ELwOQ/hqdefault.jpg");
    }

    @Test
    @DisplayName("업로드 썸네일이 있으면 유튜브 썸네일 대신 내부 썸네일 URL을 우선 사용한다")
    void prefersUploadedThumbnailOverYoutubeThumbnail() {
        Concert concert = newConcert("https://youtu.be/OPf0YbXqDm0");
        ReflectionTestUtils.setField(concert, "id", 77L);
        concert.updateThumbnail(new byte[]{1, 2, 3}, "image/png", LocalDateTime.of(2026, 2, 26, 14, 30));

        ConcertResult result = ConcertResult.from(concert);

        assertThat(result.getThumbnailUrl()).startsWith("/api/concerts/77/thumbnail?ts=");
    }

    @Test
    @DisplayName("유튜브 URL이 아니면 썸네일 URL을 생성하지 않는다")
    void returnsNullWhenYoutubeUrlIsInvalid() {
        Concert concert = newConcert("https://example.com/video/abc");

        ConcertResult result = ConcertResult.from(concert);

        assertThat(result.getThumbnailUrl()).isNull();
    }

    private Concert newConcert(String youtubeVideoUrl) {
        Entertainment entertainment = new Entertainment("Sample Entertainment", "KR", "https://sample-ent.example.com");
        Artist artist = new Artist("Sample Artist", entertainment);
        Promoter promoter = new Promoter("Sample Promoter", "KR", "https://sample-promoter.example.com");
        return new Concert("Sample Concert", artist, promoter, youtubeVideoUrl);
    }
}
