package dev.iakunin.callcalendar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.iakunin.callcalendar.contract.model.Booking;
import dev.iakunin.callcalendar.contract.model.ErrorCode;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// Asserts the contract's wire format over the real HTTP/Jackson path (the app's actual
// message converter), not against a directly-injected ObjectMapper that the app never uses.
// TestController is @Import-ed explicitly rather than relying on component scanning to pick up
// a nested class declared inside a test source file.
@SpringBootTest
@AutoConfigureMockMvc
@Import(ContractModelsTest.TestController.class)
class ContractModelsTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void contextLoads() {
    assertThat(mockMvc).isNotNull();
  }

  @Test
  void errorCodeSerializesToTheContractWireValue() throws Exception {
    mockMvc
        .perform(get("/test/error-code"))
        .andExpect(status().isOk())
        .andExpect(content().string("\"slot_taken\""));
  }

  @Test
  void instantsSerializeAsUtcIso8601() throws Exception {
    mockMvc
        .perform(get("/test/booking"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.start").value("2026-09-07T06:00:00Z"))
        .andExpect(jsonPath("$.end").value("2026-09-07T06:30:00Z"));
  }

  @RestController
  static class TestController {

    @GetMapping("/test/booking")
    Booking booking() {
      return Booking.builder()
          .id("b-1")
          .eventTypeId("intro-call")
          .start(OffsetDateTime.of(2026, 9, 7, 6, 0, 0, 0, ZoneOffset.UTC))
          .end(OffsetDateTime.of(2026, 9, 7, 6, 30, 0, 0, ZoneOffset.UTC))
          .guestName("Гость")
          .guestEmail("guest@example.com")
          .createdAt(OffsetDateTime.of(2026, 9, 1, 12, 0, 0, 0, ZoneOffset.UTC))
          .build();
    }

    @GetMapping("/test/error-code")
    ErrorCode errorCode() {
      return ErrorCode.SLOT_TAKEN;
    }
  }
}
