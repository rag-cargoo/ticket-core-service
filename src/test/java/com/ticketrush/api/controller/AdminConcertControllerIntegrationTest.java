package com.ticketrush.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminConcertControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void adminEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(post("/api/admin/concerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCrudAndThumbnailFlowWorks() throws Exception {
        long suffix = System.nanoTime();
        String createBody = """
                {
                  "title":"Admin Concert %d",
                  "artistName":"Admin Artist %d",
                  "agencyName":"Admin Agency %d",
                  "artistDisplayName":"ADMIN ARTIST %d",
                  "artistGenre":"K-POP",
                  "artistDebutDate":"2020-01-01",
                  "agencyCountryCode":"KR",
                  "agencyHomepageUrl":"https://admin-agency.example.com",
                  "youtubeVideoUrl":"https://www.youtube.com/watch?v=U8A5sK5PRCI"
                }
                """.formatted(suffix, suffix, suffix, suffix);

        MvcResult createResult = mockMvc.perform(post("/api/admin/concerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.youtubeVideoUrl").value("https://www.youtube.com/watch?v=U8A5sK5PRCI"))
                .andReturn();
        long concertId = readLongField(createResult, "id");

        String createOptionBody = """
                {
                  "concertDate":"2030-01-01T19:00:00",
                  "seatCount":3,
                  "ticketPriceAmount":330000
                }
                """;
        MvcResult createOptionResult = mockMvc.perform(post("/api/admin/concerts/{concertId}/options", concertId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createOptionBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticketPriceAmount").value(330000))
                .andReturn();
        long optionId = readLongField(createOptionResult, "id");

        String updateOptionBody = """
                {
                  "concertDate":"2030-01-02T20:00:00",
                  "ticketPriceAmount":350000
                }
                """;
        mockMvc.perform(put("/api/admin/concerts/options/{optionId}", optionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateOptionBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketPriceAmount").value(350000))
                .andExpect(jsonPath("$.concertDate").value("2030-01-02T20:00:00"));

        MockMultipartFile image = new MockMultipartFile(
                "image",
                "thumbnail.png",
                "image/png",
                createSampleImagePng()
        );
        mockMvc.perform(multipart("/api/admin/concerts/{concertId}/thumbnail", concertId)
                        .file(image))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.thumbnailUrl").value("/api/concerts/" + concertId + "/thumbnail"));

        mockMvc.perform(get("/api/concerts/{concertId}/thumbnail", concertId))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.IMAGE_JPEG))
                .andExpect(result -> assertThat(result.getResponse().getContentAsByteArray()).isNotEmpty());

        String updateConcertBody = """
                {
                  "title":"Admin Concert Updated %d",
                  "artistName":"Admin Artist %d",
                  "agencyName":"Admin Agency %d",
                  "artistDisplayName":"ADMIN ARTIST UPDATED %d",
                  "artistGenre":"K-POP",
                  "artistDebutDate":"2020-01-01",
                  "agencyCountryCode":"KR",
                  "agencyHomepageUrl":"https://admin-agency.example.com",
                  "youtubeVideoUrl":"https://www.youtube.com/watch?v=mzK6w7AhfN0"
                }
                """.formatted(suffix, suffix, suffix, suffix);

        mockMvc.perform(put("/api/admin/concerts/{concertId}", concertId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateConcertBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Admin Concert Updated " + suffix))
                .andExpect(jsonPath("$.youtubeVideoUrl").value("https://www.youtube.com/watch?v=mzK6w7AhfN0"));

        mockMvc.perform(delete("/api/admin/concerts/options/{optionId}", optionId))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/admin/concerts/{concertId}", concertId))
                .andExpect(status().isNoContent());
    }

    private long readLongField(MvcResult result, String fieldName) throws Exception {
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        return root.path(fieldName).asLong();
    }

    private byte[] createSampleImagePng() throws Exception {
        BufferedImage image = new BufferedImage(1280, 720, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.DARK_GRAY);
        graphics.fillRect(0, 0, 1280, 720);
        graphics.setColor(Color.WHITE);
        graphics.fillRect(40, 40, 1200, 640);
        graphics.dispose();

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", outputStream);
            return outputStream.toByteArray();
        }
    }
}
