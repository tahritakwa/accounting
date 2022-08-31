package fr.sparkit.accounting.application.controllers;

import fr.sparkit.accounting.dto.JournalDto;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class JournalIT extends ITConfig {
    private static final int ONE = 1;
    private static final int TWO = 2;
    private static final String JOURNAL_URL = "/api/accounting/journal/";
    private static final String SAMPLE_LABEL = "Vente";

    HttpHeaders httpHeaders;

    @BeforeAll
    void setUp() {
        httpHeaders = getHttpHeaders();
    }

    @Test
    void saveIT() {
        JournalDto journalDto = new JournalDto(null, "BNQ", "Banque", LocalDateTime.now(), false);
        HttpEntity entity = new HttpEntity(journalDto, httpHeaders);

        ResponseEntity<JournalDto> journalResponse = getTestRestTemplate().exchange(getRootUrl() + JOURNAL_URL,
                HttpMethod.POST, entity, JournalDto.class);

        assertNotNull(journalResponse);
        assertEquals(HttpStatus.OK, journalResponse.getStatusCode());
    }

    @Test
    void updateJournalIT() {
        JournalDto journalDtoUpdate = new JournalDto(1L, "VTE", SAMPLE_LABEL, LocalDateTime.now(), false);
        HttpEntity entity = new HttpEntity(journalDtoUpdate, httpHeaders);
        ResponseEntity<JournalDto> journalResponse = getTestRestTemplate().exchange(getRootUrl() + JOURNAL_URL,
                HttpMethod.POST, entity, JournalDto.class);
        assertNotNull(journalResponse);
        assertEquals(HttpStatus.OK, journalResponse.getStatusCode());
        assertEquals(SAMPLE_LABEL, journalResponse.getBody().getLabel());
    }

    @Test
    void getJournalIT() {
        HttpEntity entity = new HttpEntity(httpHeaders);

        ResponseEntity<JournalDto> journalResponse = getTestRestTemplate().exchange(getRootUrl() + JOURNAL_URL + ONE,
                HttpMethod.GET, entity, JournalDto.class);

        assertNotNull(journalResponse);
        assertEquals(HttpStatus.OK, journalResponse.getStatusCode());
    }

    @Test
    void deleteIT() {

        HttpEntity entity = new HttpEntity(httpHeaders);
        ResponseEntity<String> response = getTestRestTemplate().exchange(JOURNAL_URL + TWO, HttpMethod.DELETE, entity,
                String.class);
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

}
