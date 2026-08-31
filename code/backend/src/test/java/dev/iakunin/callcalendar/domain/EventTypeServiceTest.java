package dev.iakunin.callcalendar.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.iakunin.callcalendar.config.CalendarProperties;
import dev.iakunin.callcalendar.contract.model.ErrorCode;
import dev.iakunin.callcalendar.contract.model.EventType;
import dev.iakunin.callcalendar.storage.InMemoryBookingRepository;
import dev.iakunin.callcalendar.storage.InMemoryEventTypeRepository;
import dev.iakunin.callcalendar.web.ApiException;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.EnumSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EventTypeServiceTest {

  private InMemoryEventTypeRepository repository;
  private EventTypeService service;

  @BeforeEach
  void setUp() {
    repository = new InMemoryEventTypeRepository();
    CalendarProperties.Schedule schedule =
        new CalendarProperties.Schedule(
            ZoneId.of("Europe/Moscow"),
            EnumSet.of(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY),
            LocalTime.of(9, 0),
            LocalTime.of(18, 0));
    SlotService slots =
        new SlotService(
            new InMemoryBookingRepository(),
            schedule,
            14,
            Clock.fixed(Instant.parse("2026-09-07T05:00:00Z"), ZoneOffset.UTC));
    service = new EventTypeService(repository, slots);
  }

  private static EventType eventType(String id) {
    return EventType.builder().id(id).title("Title").description("d").durationMinutes(30).build();
  }

  private static EventType eventType(String id, String title, int durationMinutes) {
    return EventType.builder()
        .id(id)
        .title(title)
        .description("d")
        .durationMinutes(durationMinutes)
        .build();
  }

  @Test
  void createsAnEventType() {
    assertThat(service.create(eventType("intro")).getId()).isEqualTo("intro");
    assertThat(service.findAll()).hasSize(1);
  }

  @Test
  void rejectsADuplicateId() {
    service.create(eventType("intro"));

    assertThatThrownBy(() -> service.create(eventType("intro")))
        .isInstanceOfSatisfying(
            ApiException.class,
            e -> {
              assertThat(e.getCode()).isEqualTo(ErrorCode.EVENT_TYPE_ID_TAKEN);
              assertThat(e.getStatus().value()).isEqualTo(409);
            });
  }

  @Test
  void rejectsAnInvalidBody() {
    assertThatThrownBy(() -> service.create(eventType("intro", "Title", 0)))
        .isInstanceOfSatisfying(
            ApiException.class,
            e -> assertThat(e.getCode()).isEqualTo(ErrorCode.VALIDATION_FAILED));

    assertThatThrownBy(() -> service.create(eventType("intro", " ", 30)))
        .isInstanceOfSatisfying(
            ApiException.class,
            e -> assertThat(e.getCode()).isEqualTo(ErrorCode.VALIDATION_FAILED));
  }

  @Test
  void reportsAnUnknownIdAsNotFound() {
    assertThatThrownBy(() -> service.getById("missing"))
        .isInstanceOfSatisfying(
            ApiException.class,
            e -> {
              assertThat(e.getCode()).isEqualTo(ErrorCode.EVENT_TYPE_NOT_FOUND);
              assertThat(e.getStatus().value()).isEqualTo(404);
            });

    assertThatThrownBy(() -> service.slotsOf("missing"))
        .isInstanceOfSatisfying(
            ApiException.class,
            e -> {
              assertThat(e.getCode()).isEqualTo(ErrorCode.EVENT_TYPE_NOT_FOUND);
              assertThat(e.getStatus().value()).isEqualTo(404);
            });
  }

  @Test
  void returnsSlotsForAKnownEventType() {
    service.create(eventType("intro"));

    assertThat(service.slotsOf("intro")).isNotEmpty();
  }
}
