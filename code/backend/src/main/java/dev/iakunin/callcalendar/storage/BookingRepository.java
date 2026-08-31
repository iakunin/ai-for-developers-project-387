package dev.iakunin.callcalendar.storage;

import dev.iakunin.callcalendar.contract.model.Booking;
import java.util.List;

public interface BookingRepository {

  List<Booking> findAll();

  void save(Booking booking);
}
