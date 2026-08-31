package dev.iakunin.callcalendar.storage;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iakunin.callcalendar.contract.model.Booking;
import dev.iakunin.callcalendar.contract.model.EventType;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class InMemoryRepositoryTest {

  private static EventType eventType(String id) {
    return EventType.builder()
        .id(id)
        .title("Title " + id)
        .description("d")
        .durationMinutes(30)
        .build();
  }

  @Test
  void savesAndFindsAnEventTypeById() {
    InMemoryEventTypeRepository repository = new InMemoryEventTypeRepository();

    assertThat(repository.saveIfAbsent(eventType("a"))).isTrue();

    assertThat(repository.findById("a")).map(EventType::getTitle).hasValue("Title a");
    assertThat(repository.findById("missing")).isEmpty();
  }

  @Test
  void refusesToOverwriteAnExistingId() {
    InMemoryEventTypeRepository repository = new InMemoryEventTypeRepository();
    repository.saveIfAbsent(eventType("a"));

    EventType other =
        EventType.builder().id("a").title("Other").description("d").durationMinutes(30).build();

    assertThat(repository.saveIfAbsent(other)).isFalse();
    assertThat(repository.findById("a")).map(EventType::getTitle).hasValue("Title a");
  }

  @Test
  void keepsEventTypesInCreationOrder() {
    InMemoryEventTypeRepository repository = new InMemoryEventTypeRepository();
    repository.saveIfAbsent(eventType("c"));
    repository.saveIfAbsent(eventType("a"));
    repository.saveIfAbsent(eventType("b"));

    assertThat(repository.findAll()).extracting(EventType::getId).containsExactly("c", "a", "b");
  }

  @Test
  void storesBookings() {
    InMemoryBookingRepository repository = new InMemoryBookingRepository();
    OffsetDateTime start = OffsetDateTime.of(2026, 9, 7, 6, 0, 0, 0, ZoneOffset.UTC);

    repository.save(
        Booking.builder()
            .id("b-1")
            .eventTypeId("a")
            .start(start)
            .end(start.plusMinutes(30))
            .build());

    assertThat(repository.findAll()).extracting(Booking::getId).containsExactly("b-1");
  }
}
