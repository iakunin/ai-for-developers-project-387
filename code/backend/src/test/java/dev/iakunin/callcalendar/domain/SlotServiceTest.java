package dev.iakunin.callcalendar.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.iakunin.callcalendar.config.CalendarProperties;
import dev.iakunin.callcalendar.contract.model.Booking;
import dev.iakunin.callcalendar.contract.model.EventType;
import dev.iakunin.callcalendar.contract.model.Slot;
import dev.iakunin.callcalendar.storage.InMemoryBookingRepository;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * The window is anchored on Monday 2026-09-07, so D0..D13 is 2026-09-07..2026-09-20 and the first
 * day outside the window, 2026-09-21, is itself a working Monday.
 */
class SlotServiceTest {

  private static final ZoneId MSK = ZoneId.of("Europe/Moscow");

  // 05:00Z == 08:00 MSK, an hour before the calendar opens, so D0 is fully available.
  private static final Instant MONDAY_BEFORE_OPEN = Instant.parse("2026-09-07T05:00:00Z");

  private final InMemoryBookingRepository bookings = new InMemoryBookingRepository();

  private SlotService serviceAt(Instant now) {
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
    return new SlotService(bookings, schedule, 14, Clock.fixed(now, ZoneOffset.UTC));
  }

  private static EventType eventType(String id, int durationMinutes) {
    return EventType.builder()
        .id(id)
        .title(id)
        .description("d")
        .durationMinutes(durationMinutes)
        .build();
  }

  private void book(String eventTypeId, String startIso, String endIso) {
    bookings.save(
        Booking.builder()
            .id("b-" + startIso)
            .eventTypeId(eventTypeId)
            .start(OffsetDateTime.parse(startIso))
            .end(OffsetDateTime.parse(endIso))
            .build());
  }

  @Test
  void generatesEighteenHalfHourSlotsOnEachOfTheTenWorkingDays() {
    List<Slot> slots = serviceAt(MONDAY_BEFORE_OPEN).freeSlots(eventType("a", 30));

    // 10 working days in 2026-09-07..2026-09-20, 18 slots each between 09:00 and 18:00 MSK.
    assertThat(slots).hasSize(180);
    assertThat(slots.getFirst().getStart()).isEqualTo(OffsetDateTime.parse("2026-09-07T06:00:00Z"));
    assertThat(slots.getFirst().getEnd()).isEqualTo(OffsetDateTime.parse("2026-09-07T06:30:00Z"));
  }

  @Test
  void returnsSlotsSortedAscendingByStart() {
    List<Slot> slots = serviceAt(MONDAY_BEFORE_OPEN).freeSlots(eventType("a", 30));

    assertThat(slots)
        .isSortedAccordingTo((left, right) -> left.getStart().compareTo(right.getStart()));
  }

  @Test
  void skipsWeekends() {
    List<Slot> slots = serviceAt(MONDAY_BEFORE_OPEN).freeSlots(eventType("a", 30));

    assertThat(slots).isNotEmpty();
    assertThat(slots)
        .extracting(slot -> slot.getStart().atZoneSameInstant(MSK).getDayOfWeek())
        .doesNotContain(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
  }

  @Test
  void neverOffersASlotThatWouldStraddleClosingTime() {
    // 540 minutes of working day / 40 = 13 slots; the 14th would end at 18:20.
    List<Slot> slots = serviceAt(MONDAY_BEFORE_OPEN).freeSlots(eventType("a", 40));

    List<Slot> firstDay =
        slots.stream()
            .filter(slot -> slot.getStart().isBefore(OffsetDateTime.parse("2026-09-08T00:00:00Z")))
            .toList();

    assertThat(firstDay).hasSize(13);
    assertThat(firstDay.getLast().getStart())
        .isEqualTo(OffsetDateTime.parse("2026-09-07T14:00:00Z")); // 17:00 MSK
    assertThat(firstDay.getLast().getEnd())
        .isEqualTo(OffsetDateTime.parse("2026-09-07T14:40:00Z")); // 17:40 MSK
  }

  @Test
  void dropsSlotsThatHaveAlreadyStartedToday() {
    // 07:15Z == 10:15 MSK: the 09:00, 09:30 and 10:00 slots are in the past.
    List<Slot> slots =
        serviceAt(Instant.parse("2026-09-07T07:15:00Z")).freeSlots(eventType("a", 30));

    assertThat(slots.getFirst().getStart()).isEqualTo(OffsetDateTime.parse("2026-09-07T07:30:00Z"));
    assertThat(slots).hasSize(177); // 15 left today + 9 full working days
  }

  @Test
  void includesTheLastWorkingDayInsideTheWindowAndExcludesTheDayAfterIt() {
    List<Slot> slots = serviceAt(MONDAY_BEFORE_OPEN).freeSlots(eventType("a", 30));

    List<java.time.LocalDate> days =
        slots.stream()
            .map(slot -> slot.getStart().atZoneSameInstant(MSK).toLocalDate())
            .distinct()
            .toList();

    assertThat(days).contains(java.time.LocalDate.parse("2026-09-07")); // D0
    assertThat(days)
        .contains(java.time.LocalDate.parse("2026-09-18")); // last working day in window
    assertThat(days).doesNotContain(java.time.LocalDate.parse("2026-09-21")); // D14, a Monday
  }

  @Test
  void aBookingOfAnotherEventTypeBlocksTheOverlappingSlots() {
    // Tuesday 09:00-10:00 MSK booked as a 60-minute type.
    book("other", "2026-09-08T06:00:00Z", "2026-09-08T07:00:00Z");

    List<Slot> slots = serviceAt(MONDAY_BEFORE_OPEN).freeSlots(eventType("a", 30));

    assertThat(slots)
        .extracting(Slot::getStart)
        .doesNotContain(
            OffsetDateTime.parse("2026-09-08T06:00:00Z"),
            OffsetDateTime.parse("2026-09-08T06:30:00Z"))
        .contains(OffsetDateTime.parse("2026-09-08T07:00:00Z"));
  }

  @Test
  void blocksSlotsThatOnlyPartiallyOverlapABooking() {
    // 09:15-09:45 MSK straddles the 09:00 and 09:30 half-hour slots.
    book("other", "2026-09-08T06:15:00Z", "2026-09-08T06:45:00Z");

    List<Slot> slots = serviceAt(MONDAY_BEFORE_OPEN).freeSlots(eventType("a", 30));

    assertThat(slots)
        .extracting(Slot::getStart)
        .doesNotContain(
            OffsetDateTime.parse("2026-09-08T06:00:00Z"),
            OffsetDateTime.parse("2026-09-08T06:30:00Z"))
        .contains(OffsetDateTime.parse("2026-09-08T07:00:00Z"));
  }

  @Test
  void treatsBackToBackIntervalsAsFree() {
    // A booking ending exactly when a slot starts does not block it.
    book("other", "2026-09-08T05:30:00Z", "2026-09-08T06:00:00Z");

    List<Slot> slots = serviceAt(MONDAY_BEFORE_OPEN).freeSlots(eventType("a", 30));

    assertThat(slots)
        .extracting(Slot::getStart)
        .contains(OffsetDateTime.parse("2026-09-08T06:00:00Z"));
  }

  @Test
  void treatsASlotEndingExactlyWhenABookingStartsAsFree() {
    // A booking starting exactly when a slot ends does not block it.
    book("other", "2026-09-08T07:00:00Z", "2026-09-08T07:30:00Z");

    List<Slot> slots = serviceAt(MONDAY_BEFORE_OPEN).freeSlots(eventType("a", 30));

    assertThat(slots)
        .extracting(Slot::getStart)
        .contains(OffsetDateTime.parse("2026-09-08T06:30:00Z"));
  }

  @Test
  void recognisesWindowMembership() {
    SlotService service = serviceAt(MONDAY_BEFORE_OPEN);

    assertThat(service.isWithinWindow(Instant.parse("2026-09-07T06:00:00Z"))).isTrue();
    assertThat(service.isWithinWindow(Instant.parse("2026-09-04T06:00:00Z"))).isFalse(); // past
    assertThat(service.isWithinWindow(Instant.parse("2026-09-21T06:00:00Z"))).isFalse(); // D14
  }

  @Test
  void recognisesValidGridStarts() {
    SlotService service = serviceAt(MONDAY_BEFORE_OPEN);
    EventType type = eventType("a", 30);

    assertThat(service.isGridStart(type, Instant.parse("2026-09-07T06:00:00Z"))).isTrue();
    assertThat(service.isGridStart(type, Instant.parse("2026-09-07T06:10:00Z")))
        .isFalse(); // misaligned
    assertThat(service.isGridStart(type, Instant.parse("2026-09-07T05:30:00Z")))
        .isFalse(); // before opening
    assertThat(service.isGridStart(type, Instant.parse("2026-09-07T15:00:00Z")))
        .isFalse(); // after closing
    assertThat(service.isGridStart(type, Instant.parse("2026-09-12T06:00:00Z")))
        .isFalse(); // Saturday
  }

  @Test
  // SAME_THREAD (the default) cannot preempt a non-interruptible CPU loop; SEPARATE_THREAD
  // actually aborts the test if durationOf ever stops failing fast.
  @Timeout(value = 5, unit = TimeUnit.SECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
  void rejectsANonPositiveDuration() {
    SlotService service = serviceAt(MONDAY_BEFORE_OPEN);

    assertThatThrownBy(() -> service.freeSlots(eventType("a", 0)))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
