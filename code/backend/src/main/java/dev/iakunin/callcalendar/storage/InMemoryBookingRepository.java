package dev.iakunin.callcalendar.storage;

import dev.iakunin.callcalendar.contract.model.Booking;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Repository;

/** Reads dominate; writes are rare and already serialized by BookingService's lock. */
@Repository
public class InMemoryBookingRepository implements BookingRepository {

  private final List<Booking> bookings = new CopyOnWriteArrayList<>();

  @Override
  public List<Booking> findAll() {
    return List.copyOf(bookings);
  }

  @Override
  public void save(Booking booking) {
    bookings.add(booking);
  }

  @Override
  public boolean deleteById(String id) {
    return bookings.removeIf(b -> b.getId().equals(id));
  }
}
