package fr.sparkit.accounting.auditing;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import fr.sparkit.accounting.constants.AccountingConstants;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class AuthorizationImpl {

    @Value("${auth.url}")
    private String authUrl;

    public Integer authorize(String authorization, String[] permissions) {
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Object> response;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authorization);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<List<String>> entity = new HttpEntity<>(Arrays.asList(permissions), headers);
        try {
            response = restTemplate.exchange(authUrl, HttpMethod.POST, entity, Object.class);
        } catch (Exception exception) {
            log.error(exception.getMessage());
            return 0;
        }
        if (response.getBody() instanceof Boolean) {
            return Boolean.TRUE.equals(response.getBody()) ? AccountingConstants.AUTHORIZED_CODE
                    : AccountingConstants.UNAUTHORIZED_CODE;
        } else {
            Map<String, Object> result = (Map<String, Object>) response.getBody();
            return Integer.parseInt(result.get("errorCode").toString());
        }
    }
}
