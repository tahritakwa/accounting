package fr.sparkit.accounting.application.controllers;

import fr.sparkit.accounting.dao.FiscalYearDao;
import fr.sparkit.accounting.dto.FiscalYearDto;
import fr.sparkit.accounting.entities.FiscalYear;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class FiscalYearIT extends ITConfig {

        private static final int TWO = 2;
        private static final int ONE = 1;
        private static final String FISCAL_YEAR_NAME_2018 = "2018";
        private static final String FISCAL_YEAR_NAME_2019 = "2019";
        private static final String FISCAL_YEAR_URL = "/api/accounting/fiscalyear";
        HttpHeaders httpHeaders;
        @Autowired
        private FiscalYearDao fiscalYearDao;
        private LocalDateTime conclusionDateOfFiscalYear2018 = LocalDateTime.of(2019, 3, 5, ONE, ONE);
        private LocalDateTime closingDateOfFiscalYear2018 = LocalDateTime.of(2019, 3, 5, ONE, ONE);
        private LocalDateTime satrtDateOfFiscalYear2018 = LocalDate.of(2018, Month.JANUARY, 1).atStartOfDay();
        private LocalDateTime endtDateOfFiscalYear2018 = LocalDate.of(2018, Month.DECEMBER, 31).atTime(LocalTime.MAX);
        private LocalDateTime satrtDateOfFiscalYear2019 = LocalDate.of(2019, Month.JANUARY, 1).atStartOfDay();
        private LocalDateTime endtDateOfFiscalYear2019 = LocalDate.of(2019, Month.DECEMBER, 31).atTime(LocalTime.MAX);

        @BeforeAll
        void setUp() {
                httpHeaders = getHttpHeaders();
                FiscalYear fiscalYearOld = new FiscalYear(null, FISCAL_YEAR_NAME_2018, satrtDateOfFiscalYear2018,
                        endtDateOfFiscalYear2018, closingDateOfFiscalYear2018, conclusionDateOfFiscalYear2018, 1, false,
                        null);
                fiscalYearDao.save(fiscalYearOld);
        }

        @Test
        void saveOrUpdateT() {
                FiscalYearDto fiscalYearDto = new FiscalYearDto(null, FISCAL_YEAR_NAME_2019, satrtDateOfFiscalYear2019,
                        endtDateOfFiscalYear2019, null, null, 0);

                HttpEntity entity = new HttpEntity(fiscalYearDto, httpHeaders);

                ResponseEntity<FiscalYearDto> fiscalYearResponse = getTestRestTemplate()
                        .exchange(getRootUrl() + FISCAL_YEAR_URL, HttpMethod.POST, entity, FiscalYearDto.class);

                assertNotNull(fiscalYearResponse);
                assertEquals(HttpStatus.OK, fiscalYearResponse.getStatusCode());
                assertEquals(TWO, fiscalYearResponse.getBody().getId());
        }

        @Test
        void existsByIdT() {
                HttpEntity entity = new HttpEntity(httpHeaders);

                ResponseEntity<FiscalYearDto> fiscalYearResponse = getTestRestTemplate()
                        .exchange(getRootUrl() + FISCAL_YEAR_URL + "/1", HttpMethod.GET, entity, FiscalYearDto.class);

                assertNotNull(fiscalYearResponse);
                assertEquals(HttpStatus.OK, fiscalYearResponse.getStatusCode());
                assertEquals("2018", fiscalYearResponse.getBody().getName());
        }

        @Test
        void findPreviousFiscalYearT() {
                HttpEntity entity = new HttpEntity(httpHeaders);

                ResponseEntity<FiscalYearDto> fiscalYearResponse = getTestRestTemplate()
                        .exchange(getRootUrl() + FISCAL_YEAR_URL + "/previous-fiscal-year/2", HttpMethod.GET, entity,
                                FiscalYearDto.class);
                assertNotNull(fiscalYearResponse);
                assertEquals(HttpStatus.OK, fiscalYearResponse.getStatusCode());
        }

        @Test
        void firstFiscalYearNotConcludedT() {
                HttpEntity entity = new HttpEntity(httpHeaders);

                ResponseEntity<FiscalYearDto> fiscalYearResponse = getTestRestTemplate()
                        .exchange(getRootUrl() + FISCAL_YEAR_URL + "/first-fiscal-year-not-concluded", HttpMethod.GET,
                                entity, FiscalYearDto.class);
                assertNotNull(fiscalYearResponse);
                assertEquals(conclusionDateOfFiscalYear2018, fiscalYearResponse.getBody().getConclusionDate());
        }

        @Test
        void startDateFiscalYearT() {
                HttpEntity entity = new HttpEntity(httpHeaders);

                ResponseEntity<LocalDateTime> fiscalYearResponse = getTestRestTemplate()
                        .exchange(getRootUrl() + FISCAL_YEAR_URL + "/start-date-fiscalyear", HttpMethod.POST, entity,
                                LocalDateTime.class);

                assertNotNull(fiscalYearResponse);
                assertEquals(endtDateOfFiscalYear2018.plusDays(ONE), fiscalYearResponse.getBody());
        }

        @Test
        void findClosedFiscalYearsT() {
                HttpEntity entity = new HttpEntity(httpHeaders);

                ResponseEntity<FiscalYear[]> fiscalYearResponse = getTestRestTemplate()
                        .exchange(getRootUrl() + FISCAL_YEAR_URL + "/closed-fiscal-years", HttpMethod.GET, entity,
                                FiscalYear[].class);

                assertNotNull(fiscalYearResponse);
                assertEquals(FISCAL_YEAR_NAME_2018, fiscalYearResponse.getBody()[0].getName());
        }
}
