package dev.iakunin.callcalendar.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.iakunin.callcalendar.config.CalendarProperties;
import dev.iakunin.callcalendar.contract.model.Booking;
import dev.iakunin.callcalendar.contract.model.BookingCreate;
import dev.iakunin.callcalendar.contract.model.ErrorCode;
import dev.iakunin.callcalendar.contract.model.EventType;
import dev.iakunin.callcalendar.contract.model.Slot;
import dev.iakunin.callcalendar.storage.InMemoryBookingRepository;
import dev.iakunin.callcalendar.storage.InMemoryEventTypeRepository;
import dev.iakunin.callcalendar.web.ApiException;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BookingServiceTest {

  private static final ZoneId MSK = ZoneId.of("Europe/Moscow");
  private static final Instant MONDAY_BEFORE_OPEN = Instant.parse("2026-09-07T05:00:00Z");
  private static final OffsetDateTime FREE_SLOT = OffsetDateTime.parse("2026-09-07T06:00:00Z");

  private InMemoryEventTypeRepository eventTypes;
  private InMemoryBookingRepository bookings;
  private BookingService service;

  @BeforeEach
  void setUp() {
    eventTypes = new InMemoryEventTypeRepository();
    bookings = new InMemoryBookingRepository();
    eventTypes.saveIfAbsent(
        EventType.builder()
            .id("intro")
            .title("Intro")
            .description("d")
            .durationMinutes(30)
            .build());

    CalendarProperties.Schedule schedule =
        new CalendarProperties.Schedule(
            MSK,
            EnumSet.of(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY),
            LocalTime.of(9, 0),
            LocalTime.of(18, 0));
    Clock clock = Clock.fixed(MONDAY_BEFORE_OPEN, ZoneOffset.UTC);
    SlotService slots = new SlotService(bookings, schedule, 14, clock);
    service = new BookingService(bookings, eventTypes, slots, clock);
  }

  private static BookingCreate request(
      String eventTypeId, OffsetDateTime start, String guestName, String guestEmail) {
    return BookingCreate.builder()
        .eventTypeId(eventTypeId)
        .start(start)
        .guestName(guestName)
        .guestEmail(guestEmail)
        .build();
  }

  private static BookingCreate request(String eventTypeId, OffsetDateTime start) {
    return request(eventTypeId, start, "Гость", "guest@example.com");
  }

  @Test
  void createsABookingOnAFreeSlot() {
    Booking booking = service.create(request("intro", FREE_SLOT));

    assertThat(booking.getId()).isNotBlank();
    assertThat(booking.getEventTypeId()).isEqualTo("intro");
    assertThat(booking.getStart()).isEqualTo(FREE_SLOT);
    assertThat(booking.getEnd()).isEqualTo(FREE_SLOT.plusMinutes(30));
    assertThat(booking.getCreatedAt()).isNotNull();
    assertThat(bookings.findAll()).hasSize(1);
  }

  @Test
  void rejectsABlankGuestName() {
    assertThatThrownBy(() -> service.create(request("intro", FREE_SLOT, " ", "guest@example.com")))
        .isInstanceOfSatisfying(
            ApiException.class,
            e -> assertThat(e.getCode()).isEqualTo(ErrorCode.VALIDATION_FAILED));
  }

  @Test
  void rejectsAMalformedGuestEmail() {
    assertThatThrownBy(() -> service.create(request("intro", FREE_SLOT, "Гость", "not-an-email")))
        .isInstanceOfSatisfying(
            ApiException.class,
            e -> assertThat(e.getCode()).isEqualTo(ErrorCode.VALIDATION_FAILED));
  }

  @Test
  void rejectsAMissingStart() {
    assertThatThrownBy(() -> service.create(request("intro", null)))
        .isInstanceOfSatisfying(
            ApiException.class,
            e -> assertThat(e.getCode()).isEqualTo(ErrorCode.VALIDATION_FAILED));
  }

  @Test
  void reportsTheUnknownEventTypeBeforeTheWindow() {
    assertThatThrownBy(
            () -> service.create(request("missing", OffsetDateTime.parse("2026-09-30T06:00:00Z"))))
        .isInstanceOfSatisfying(
            ApiException.class,
            e -> assertThat(e.getCode()).isEqualTo(ErrorCode.EVENT_TYPE_NOT_FOUND));
  }

  @Test
  void reportsTheMisalignedStartBeforeTheOverlap() {
    service.create(request("intro", FREE_SLOT));
    assertThatThrownBy(
            () -> service.create(request("intro", OffsetDateTime.parse("2026-09-07T06:10:00Z"))))
        .isInstanceOfSatisfying(
            ApiException.class,
            e -> assertThat(e.getCode()).isEqualTo(ErrorCode.SLOT_NOT_AVAILABLE));
  }

  @Test
  void reportsAnUnknownEventTypeAsABadRequestNotANotFound() {
    assertThatThrownBy(() -> service.create(request("missing", FREE_SLOT)))
        .isInstanceOfSatisfying(
            ApiException.class,
            e -> {
              assertThat(e.getCode()).isEqualTo(ErrorCode.EVENT_TYPE_NOT_FOUND);
              assertThat(e.getStatus().value()).isEqualTo(400);
            });
  }

  @Test
  void rejectsAStartBeyondTheBookingWindow() {
    assertThatThrownBy(
            () -> service.create(request("intro", OffsetDateTime.parse("2026-09-21T06:00:00Z"))))
        .isInstanceOfSatisfying(
            ApiException.class,
            e -> assertThat(e.getCode()).isEqualTo(ErrorCode.OUTSIDE_BOOKING_WINDOW));
  }

  @Test
  void rejectsAStartInThePast() {
    assertThatThrownBy(
            () -> service.create(request("intro", OffsetDateTime.parse("2026-09-04T06:00:00Z"))))
        .isInstanceOfSatisfying(
            ApiException.class,
            e -> assertThat(e.getCode()).isEqualTo(ErrorCode.OUTSIDE_BOOKING_WINDOW));
  }

  @Test
  void rejectsAStartThatIsNotOnTheGrid() {
    assertThatThrownBy(
            () -> service.create(request("intro", OffsetDateTime.parse("2026-09-07T06:10:00Z"))))
        .isInstanceOfSatisfying(
            ApiException.class,
            e -> assertThat(e.getCode()).isEqualTo(ErrorCode.SLOT_NOT_AVAILABLE));
  }

  @Test
  void rejectsAStartOnAWeekend() {
    assertThatThrownBy(
            () -> service.create(request("intro", OffsetDateTime.parse("2026-09-12T06:00:00Z"))))
        .isInstanceOfSatisfying(
            ApiException.class,
            e -> assertThat(e.getCode()).isEqualTo(ErrorCode.SLOT_NOT_AVAILABLE));
  }

  @Test
  void rejectsATimeAlreadyBookedByAnotherEventType() {
    eventTypes.saveIfAbsent(
        EventType.builder().id("long").title("Long").description("d").durationMinutes(60).build());
    service.create(request("long", FREE_SLOT));

    assertThatThrownBy(() -> service.create(request("intro", FREE_SLOT.plusMinutes(30))))
        .isInstanceOfSatisfying(
            ApiException.class,
            e -> {
              assertThat(e.getCode()).isEqualTo(ErrorCode.SLOT_TAKEN);
              assertThat(e.getStatus().value()).isEqualTo(409);
            });
  }

  @Test
  void removesABookedSlotFromTheFreeList() {
    service.create(request("intro", FREE_SLOT));

    CalendarProperties.Schedule schedule =
        new CalendarProperties.Schedule(
            MSK,
            EnumSet.of(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY),
            LocalTime.of(9, 0),
            LocalTime.of(18, 0));
    SlotService slots =
        new SlotService(bookings, schedule, 14, Clock.fixed(MONDAY_BEFORE_OPEN, ZoneOffset.UTC));

    assertThat(slots.freeSlots(eventTypes.findById("intro").orElseThrow()))
        .extracting(Slot::getStart)
        .doesNotContain(FREE_SLOT)
        .contains(FREE_SLOT.plusMinutes(30));
  }

  @Test
  void listsOnlyUpcomingBookingsSortedByStart() {
    service.create(request("intro", OffsetDateTime.parse("2026-09-08T06:00:00Z")));
    service.create(request("intro", FREE_SLOT));
    // A meeting that has already finished must not appear.
    bookings.save(
        Booking.builder()
            .id("old")
            .eventTypeId("intro")
            .start(OffsetDateTime.parse("2026-09-01T06:00:00Z"))
            .end(OffsetDateTime.parse("2026-09-01T06:30:00Z"))
            .guestName("Гость")
            .guestEmail("guest@example.com")
            .createdAt(OffsetDateTime.parse("2026-09-01T05:00:00Z"))
            .build());

    assertThat(service.upcoming())
        .extracting(Booking::getStart)
        .containsExactly(FREE_SLOT, OffsetDateTime.parse("2026-09-08T06:00:00Z"));
  }

  @Test
  void letsExactlyOneOfManyConcurrentGuestsWinTheSameSlot() throws Exception {
    int guests = 32;
    List<Callable<Boolean>> attempts =
        IntStream.range(0, guests)
            .mapToObj(
                i ->
                    (Callable<Boolean>)
                        () -> {
                          try {
                            service.create(request("intro", FREE_SLOT));
                            return true;
                          } catch (ApiException e) {
                            assertThat(e.getCode()).isEqualTo(ErrorCode.SLOT_TAKEN);
                            return false;
                          }
                        })
            .toList();

    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
      List<Future<Boolean>> results = executor.invokeAll(attempts);
      long winners = 0;
      for (Future<Boolean> result : results) {
        if (result.get()) {
          winners++;
        }
      }
      Assertions.assertThat(winners).isEqualTo(1);
    }

    assertThat(bookings.findAll()).hasSize(1);
  }
}
