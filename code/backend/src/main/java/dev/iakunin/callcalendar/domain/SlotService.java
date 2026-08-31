package dev.iakunin.callcalendar.domain;

import dev.iakunin.callcalendar.config.CalendarProperties;
import dev.iakunin.callcalendar.contract.model.Booking;
import dev.iakunin.callcalendar.contract.model.EventType;
import dev.iakunin.callcalendar.contract.model.Slot;
import dev.iakunin.callcalendar.storage.BookingRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Generates the free slots of one event type.
 *
 * <p>The contract fixes only that {@code /slots} returns free slots inside a 14-day window; how the
 * grid is laid out is backend policy and comes from configuration. {@code BookingService} validates
 * against this same class, so the two endpoints cannot disagree about what is bookable.
 */
@Service
public class SlotService {

  private final BookingRepository bookings;
  private final WorkingSchedule schedule;
  private final int windowDays;
  private final Clock clock;

  // Explicit constructors on purpose: @RequiredArgsConstructor cannot express building a
  // WorkingSchedule out of configuration, so Lombok stays off this class.
  //
  // This 4-arg constructor has no production caller; it exists so tests can inject a fixed
  // Clock and schedule directly instead of going through CalendarProperties.
  public SlotService(
      BookingRepository bookings,
      CalendarProperties.Schedule schedule,
      int windowDays,
      Clock clock) {
    this.bookings = bookings;
    this.schedule =
        new WorkingSchedule(
            schedule.timezone(), schedule.workingDays(), schedule.open(), schedule.close());
    this.windowDays = windowDays;
    this.clock = clock;
  }

  @Autowired
  public SlotService(BookingRepository bookings, CalendarProperties properties, Clock clock) {
    this(bookings, properties.schedule(), properties.booking().windowDays(), clock);
  }

  /** Free slots of this event type, ascending by start. */
  public List<Slot> freeSlots(EventType eventType) {
    Duration duration = durationOf(eventType);
    List<Booking> existing = bookings.findAll();

    List<Slot> free = new ArrayList<>();
    for (Instant start : gridStarts(eventType)) {
      Instant end = start.plus(duration);
      if (!overlapsAny(existing, start, end)) {
        free.add(Slot.builder().start(toUtc(start)).end(toUtc(end)).build());
      }
    }
    return List.copyOf(free);
  }

  /** Every grid start inside the booking window, ignoring existing bookings. Ascending. */
  public List<Instant> gridStarts(EventType eventType) {
    Duration duration = durationOf(eventType);
    Instant now = clock.instant();
    LocalDate today = LocalDate.ofInstant(now, schedule.zone());

    List<Instant> starts = new ArrayList<>();
    for (int offset = 0; offset < windowDays; offset++) {
      LocalDate day = today.plusDays(offset);
      if (!schedule.isWorkingDay(day)) {
        continue;
      }
      Instant close = schedule.closeInstant(day);
      // Stepping from the day's opening instant keeps the arithmetic correct whatever the
      // timezone rules do; it never mixes local-time and instant arithmetic.
      for (Instant start = schedule.openInstant(day);
          !start.plus(duration).isAfter(close);
          start = start.plus(duration)) {
        if (!start.isBefore(now)) {
          starts.add(start);
        }
      }
    }
    return List.copyOf(starts);
  }

  /** True when the moment is not in the past and falls inside the 14-day window. */
  public boolean isWithinWindow(Instant start) {
    Instant now = clock.instant();
    Instant windowEnd =
        LocalDate.ofInstant(now, schedule.zone())
            .plusDays(windowDays)
            .atStartOfDay(schedule.zone())
            .toInstant();
    return !start.isBefore(now) && start.isBefore(windowEnd);
  }

  /**
   * True when the moment is a legal start for this event type: a working day, at or after opening,
   * aligned to the step, and finishing before closing. Ignores the window and bookings so that the
   * caller can report the precise reason a booking was refused.
   */
  public boolean isGridStart(EventType eventType, Instant start) {
    LocalDate day = LocalDate.ofInstant(start, schedule.zone());
    if (!schedule.isWorkingDay(day)) {
      return false;
    }
    Instant open = schedule.openInstant(day);
    Duration duration = durationOf(eventType);
    if (start.isBefore(open) || start.plus(duration).isAfter(schedule.closeInstant(day))) {
      return false;
    }
    long offsetSeconds = Duration.between(open, start).getSeconds();
    return offsetSeconds % duration.getSeconds() == 0;
  }

  /** Calendar busyness is shared across all event types, so every booking counts. */
  public boolean overlapsAnyBooking(Instant start, Instant end) {
    return overlapsAny(bookings.findAll(), start, end);
  }

  private static boolean overlapsAny(List<Booking> existing, Instant start, Instant end) {
    for (Booking booking : existing) {
      Instant bookingStart = booking.getStart().toInstant();
      Instant bookingEnd = booking.getEnd().toInstant();
      if (start.isBefore(bookingEnd) && bookingStart.isBefore(end)) {
        return true;
      }
    }
    return false;
  }

  private static Duration durationOf(EventType eventType) {
    Integer durationMinutes = eventType.getDurationMinutes();
    if (durationMinutes == null || durationMinutes <= 0) {
      throw new IllegalArgumentException(
          "Event type \""
              + eventType.getId()
              + "\" has a non-positive duration: "
              + durationMinutes);
    }
    return Duration.ofMinutes(durationMinutes);
  }

  private static OffsetDateTime toUtc(Instant instant) {
    return instant.atOffset(ZoneOffset.UTC);
  }
}
