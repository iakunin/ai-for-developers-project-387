package dev.iakunin.callcalendar.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

/** Every operation and every error code the contract names. */
@SpringBootTest
@AutoConfigureMockMvc
// Rule: any Spring test that writes to the in-memory repositories must dirty the context, or
// it will pollute its siblings.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ApiContractTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void returnsTheOwnerProfile() throws Exception {
    mockMvc
        .perform(get("/api/owner"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("owner"))
        .andExpect(jsonPath("$.name").isNotEmpty());
  }

  @Test
  void listsTheSeededEventTypes() throws Exception {
    mockMvc
        .perform(get("/api/event-types"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(3))
        .andExpect(jsonPath("$[0].id").value("intro-call"))
        .andExpect(jsonPath("$[0].durationMinutes").value(15));
  }

  @Test
  void returnsOneEventType() throws Exception {
    mockMvc
        .perform(get("/api/event-types/consultation"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.durationMinutes").value(30));
  }

  @Test
  void returnsNotFoundForAnUnknownEventType() throws Exception {
    mockMvc
        .perform(get("/api/event-types/missing"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("event_type_not_found"))
        .andExpect(jsonPath("$.message").isNotEmpty());
  }

  @Test
  void returnsSlotsSortedAscending() throws Exception {
    String json =
        mockMvc
            .perform(get("/api/event-types/consultation/slots"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(com.jayway.jsonpath.JsonPath.<java.util.List<String>>read(json, "$[*].start"))
        .isSorted();
  }

  @Test
  void returnsNotFoundForSlotsOfAnUnknownEventType() throws Exception {
    mockMvc
        .perform(get("/api/event-types/missing/slots"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("event_type_not_found"));
  }

  @Test
  void createsAnEventType() throws Exception {
    mockMvc
        .perform(
            post("/api/event-types")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"id":"pair-programming","title":"Парное программирование",
                     "description":"Совместная работа над кодом","durationMinutes":90}
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value("pair-programming"));
  }

  @Test
  void rejectsADuplicateEventTypeId() throws Exception {
    mockMvc
        .perform(
            post("/api/event-types")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"id":"intro-call","title":"Другое","description":"d","durationMinutes":15}
                    """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("event_type_id_taken"));
  }

  @Test
  void rejectsAnInvalidEventTypeBody() throws Exception {
    mockMvc
        .perform(
            post("/api/event-types")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"id":"x","title":"","description":"d","durationMinutes":0}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("validation_failed"));
  }

  @Test
  void rejectsMalformedJson() throws Exception {
    mockMvc
        .perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON).content("{not json"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("validation_failed"));
  }

  @Test
  void reportsAnUnknownEventTypeOnBookingAsBadRequest() throws Exception {
    mockMvc
        .perform(
            post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"eventTypeId":"missing","start":"2030-01-01T09:00:00Z",
                     "guestName":"Гость","guestEmail":"guest@example.com"}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("event_type_not_found"));
  }

  @Test
  void rejectsABookingOutsideTheWindow() throws Exception {
    mockMvc
        .perform(
            post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"eventTypeId":"consultation","start":"2030-01-01T09:00:00Z",
                     "guestName":"Гость","guestEmail":"guest@example.com"}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("outside_booking_window"));
  }

  @Test
  void rejectsABookingOffTheSlotGrid() throws Exception {
    String slotStart =
        com.jayway.jsonpath.JsonPath.read(
            mockMvc
                .perform(get("/api/event-types/consultation/slots"))
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$[0].start");
    String offGridStart = java.time.OffsetDateTime.parse(slotStart).plusMinutes(10).toString();

    mockMvc
        .perform(
            post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"eventTypeId":"consultation","start":"%s",
                     "guestName":"Гость","guestEmail":"guest@example.com"}
                    """
                        .formatted(offGridStart)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("slot_not_available"));
  }

  @Test
  void createsABookingOnTheFirstFreeSlotAndThenReportsItTaken() throws Exception {
    String start =
        com.jayway.jsonpath.JsonPath.read(
            mockMvc
                .perform(get("/api/event-types/consultation/slots"))
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$[0].start");

    String body =
        """
        {"eventTypeId":"consultation","start":"%s",
         "guestName":"Гость","guestEmail":"guest@example.com"}
        """
            .formatted(start);

    mockMvc
        .perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNotEmpty())
        .andExpect(jsonPath("$.eventTypeId").value("consultation"))
        .andExpect(jsonPath("$.createdAt").isNotEmpty());

    mockMvc
        .perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("slot_taken"));

    mockMvc
        .perform(get("/api/bookings"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));
  }

  @Test
  void listsBookingsSortedAscendingByStart() throws Exception {
    String slotsJson =
        mockMvc
            .perform(get("/api/event-types/consultation/slots"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    String firstSlotStart = com.jayway.jsonpath.JsonPath.read(slotsJson, "$[0].start");
    String secondSlotStart = com.jayway.jsonpath.JsonPath.read(slotsJson, "$[1].start");

    // Book the later slot first so a passing test proves the endpoint sorts the
    // result, not merely that it returns bookings in creation order.
    mockMvc
        .perform(
            post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"eventTypeId":"consultation","start":"%s",
                     "guestName":"Гость","guestEmail":"guest@example.com"}
                    """
                        .formatted(secondSlotStart)))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"eventTypeId":"consultation","start":"%s",
                     "guestName":"Гость","guestEmail":"guest@example.com"}
                    """
                        .formatted(firstSlotStart)))
        .andExpect(status().isCreated());

    String bookingsJson =
        mockMvc
            .perform(get("/api/bookings"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(
            com.jayway.jsonpath.JsonPath.<java.util.List<String>>read(bookingsJson, "$[*].start"))
        .isSorted();
  }

  @Test
  void cancelsABookingAndRemovesItFromTheList() throws Exception {
    String start =
        com.jayway.jsonpath.JsonPath.read(
            mockMvc
                .perform(get("/api/event-types/consultation/slots"))
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$[0].start");

    String createdJson =
        mockMvc
            .perform(
                post("/api/bookings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"eventTypeId":"consultation","start":"%s",
                         "guestName":"Гость","guestEmail":"guest@example.com"}
                        """
                            .formatted(start)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String bookingId = com.jayway.jsonpath.JsonPath.read(createdJson, "$.id");

    mockMvc.perform(delete("/api/bookings/{id}", bookingId)).andExpect(status().isNoContent());

    mockMvc
        .perform(get("/api/bookings"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));

    // The freed slot is bookable again.
    mockMvc
        .perform(
            post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"eventTypeId":"consultation","start":"%s",
                     "guestName":"Гость","guestEmail":"guest@example.com"}
                    """
                        .formatted(start)))
        .andExpect(status().isCreated());
  }

  @Test
  void returnsNotFoundWhenCancellingAnUnknownBooking() throws Exception {
    mockMvc
        .perform(delete("/api/bookings/missing"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("booking_not_found"))
        .andExpect(jsonPath("$.message").isNotEmpty());
  }
}
