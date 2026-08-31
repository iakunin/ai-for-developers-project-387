package dev.iakunin.callcalendar.web;

import dev.iakunin.callcalendar.contract.model.Booking;
import dev.iakunin.callcalendar.contract.model.BookingCreate;
import dev.iakunin.callcalendar.domain.BookingService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

  private final BookingService bookings;

  /** Upcoming meetings across all event types. The owner's page. */
  @GetMapping
  public List<Booking> list() {
    return bookings.upcoming();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Booking create(@RequestBody BookingCreate booking) {
    return bookings.create(booking);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void cancel(@PathVariable String id) {
    bookings.cancel(id);
  }
}
