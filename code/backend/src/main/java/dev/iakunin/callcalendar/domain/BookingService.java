package dev.iakunin.callcalendar.domain;

import dev.iakunin.callcalendar.contract.model.Booking;
import dev.iakunin.callcalendar.contract.model.BookingCreate;
import dev.iakunin.callcalendar.contract.model.ErrorCode;
import dev.iakunin.callcalendar.contract.model.EventType;
import dev.iakunin.callcalendar.storage.BookingRepository;
import dev.iakunin.callcalendar.storage.EventTypeRepository;
import dev.iakunin.callcalendar.web.ApiException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Enforces the booking rules the contract fixes. */
@Service
@RequiredArgsConstructor
public class BookingService {

  private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

  private final BookingRepository bookings;
  private final EventTypeRepository eventTypes;
  private final SlotService slots;
  private final Clock clock;

  // A virtual thread per request means the check-then-write sequence needs an explicit lock;
  // ReentrantLock rather than synchronized, as the spec requires. It is initialized here, so
  // @RequiredArgsConstructor leaves it out — that annotation only takes uninitialized finals.
  private final ReentrantLock lock = new ReentrantLock();

  /** Upcoming meetings across all event types, ascending by start. */
  public List<Booking> upcoming() {
    Instant now = clock.instant();
    return bookings.findAll().stream()
        .filter(booking -> booking.getEnd().toInstant().isAfter(now))
        .sorted(Comparator.comparing(Booking::getStart))
        .toList();
  }

  /**
   * Creates a booking. The checks run in the order the spec fixes, so the same request always
   * produces the same error code.
   */
  public Booking create(BookingCreate request) {
    validate(request);

    EventType eventType =
        eventTypes
            .findById(request.getEventTypeId())
            // 400, not 404: the contract lists this code under BadRequest for this operation.
            .orElseThrow(
                () ->
                    ApiException.badRequest(
                        ErrorCode.EVENT_TYPE_NOT_FOUND,
                        "Типа события «" + request.getEventTypeId() + "» не существует."));

    // eventType and end are derived outside the lock, while isGridStart re-derives the duration
    // from eventType inside it below. That is safe only because no code path mutates a stored
    // event type, so the two derivations cannot disagree; see EventTypeRepository for why. If
    // event-type mutation or deletion is ever added, this lookup and the end computation must
    // move inside the critical section.
    Instant start = request.getStart().toInstant();
    Instant end = start.plus(Duration.ofMinutes(eventType.getDurationMinutes()));

    lock.lock();
    try {
      if (!slots.isWithinWindow(start)) {
        throw ApiException.badRequest(
            ErrorCode.OUTSIDE_BOOKING_WINDOW, "Выбранное время выходит за окно записи.");
      }
      if (!slots.isGridStart(eventType, start)) {
        throw ApiException.badRequest(
            ErrorCode.SLOT_NOT_AVAILABLE,
            "Выбранное время не совпадает с началом свободного слота.");
      }
      if (slots.overlapsAnyBooking(start, end)) {
        throw ApiException.conflict(ErrorCode.SLOT_TAKEN, "На это время уже есть бронирование.");
      }

      Booking booking =
          Booking.builder()
              .id(UUID.randomUUID().toString())
              .eventTypeId(eventType.getId())
              .start(start.atOffset(ZoneOffset.UTC))
              .end(end.atOffset(ZoneOffset.UTC))
              .guestName(request.getGuestName().trim())
              .guestEmail(request.getGuestEmail().trim())
              .createdAt(OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC))
              .build();
      bookings.save(booking);
      return booking;
    } finally {
      lock.unlock();
    }
  }

  /** Cancel (delete) a booking by its id. */
  public void cancel(String id) {
    lock.lock();
    try {
      if (!bookings.deleteById(id)) {
        throw ApiException.notFound(
            ErrorCode.BOOKING_NOT_FOUND, "Бронирование «" + id + "» не найдено.");
      }
    } finally {
      lock.unlock();
    }
  }

  /**
   * Checked here rather than through the generated bean-validation annotations, so behaviour does
   * not depend on what the generator emitted.
   */
  private static void validate(BookingCreate request) {
    if (request.getEventTypeId() == null || request.getEventTypeId().isBlank()) {
      throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "Не указан тип события.");
    }
    if (request.getStart() == null) {
      throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "Не указано время начала.");
    }
    if (request.getGuestName() == null || request.getGuestName().isBlank()) {
      throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "Не указано имя гостя.");
    }
    String email = request.getGuestEmail();
    if (email == null || !EMAIL.matcher(email.trim()).matches()) {
      throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "Некорректный email гостя.");
    }
  }
}
