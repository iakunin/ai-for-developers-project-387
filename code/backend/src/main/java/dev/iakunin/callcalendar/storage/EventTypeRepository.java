package dev.iakunin.callcalendar.storage;

import dev.iakunin.callcalendar.contract.model.EventType;
import java.util.List;
import java.util.Optional;

/** Storage boundary: swapping in a database means one new implementation and nothing else. */
public interface EventTypeRepository {

  /** All event types in creation order. */
  List<EventType> findAll();

  Optional<EventType> findById(String id);

  /** Stores the event type, or returns false when the id is already taken. */
  boolean saveIfAbsent(EventType eventType);

  // Deliberately no update or delete. BookingService reads an event type outside its booking
  // lock and relies on a stored event type never changing; adding a mutating method requires
  // moving that read inside the lock.
}
