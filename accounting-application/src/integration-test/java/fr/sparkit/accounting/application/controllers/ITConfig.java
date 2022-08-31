package fr.sparkit.accounting.application.controllers;

import fr.sparkit.accounting.application.MainApplication;
import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;

@Getter
@Setter
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = MainApplication.class)
@TestPropertySource("/application-test.properties")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ITConfig {
        @Autowired
        private TestRestTemplate testRestTemplate;

        @LocalServerPort
        private int port;

        public String getRootUrl() {
                return "http://localhost:" + port;
        }

        protected HttpHeaders getHttpHeaders() {
                HttpHeaders headers = new HttpHeaders();
                headers.add("Company", "c-test");
                return headers;
        }

}
