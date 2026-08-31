package dev.iakunin.callcalendar.config;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iakunin.callcalendar.contract.model.ApiError;
import dev.iakunin.callcalendar.contract.model.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Static resource serving and the "/" welcome-page forward only run through a real servlet
 * container, not MockMvc's mock dispatcher — so this test drives an actual embedded server.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class SpaFallbackTest {

  @Autowired private TestRestTemplate restTemplate;

  @Test
  void servesTheSpaAtTheRoot() {
    ResponseEntity<String> response = restTemplate.getForEntity("/", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("spa-root");
  }

  @Test
  void servesTheSpaForClientSideRoutes() {
    ResponseEntity<String> admin = restTemplate.getForEntity("/admin", String.class);
    assertThat(admin.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(admin.getBody()).contains("spa-root");

    ResponseEntity<String> booking = restTemplate.getForEntity("/book/consultation", String.class);
    assertThat(booking.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(booking.getBody()).contains("spa-root");
  }

  @Test
  void neverShadowsAnApiPathWithTheSpa() {
    // Without the exclusion this would return index.html with status 200.
    ResponseEntity<ApiError> eventType =
        restTemplate.getForEntity("/api/event-types/missing", ApiError.class);
    assertThat(eventType.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(eventType.getBody()).isNotNull();
    assertThat(eventType.getBody().getCode()).isEqualTo(ErrorCode.EVENT_TYPE_NOT_FOUND);

    ResponseEntity<String> unmapped =
        restTemplate.getForEntity("/api/bookings/unmapped", String.class);
    assertThat(unmapped.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void allowsCrossOriginPreflightFromTheDevFrontend() {
    HttpHeaders headers = new HttpHeaders();
    headers.setOrigin("http://localhost:5173");
    headers.add(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST");

    ResponseEntity<Void> response =
        restTemplate.exchange(
            "/api/bookings", HttpMethod.OPTIONS, new HttpEntity<>(headers), Void.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getHeaders().getAccessControlAllowOrigin())
        .isEqualTo("http://localhost:5173");
  }
}
