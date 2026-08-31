package dev.iakunin.callcalendar.storage;

import dev.iakunin.callcalendar.contract.model.EventType;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** In-memory storage. Data resets on restart, which step 4 explicitly allows. */
@Repository
public class InMemoryEventTypeRepository implements EventTypeRepository {

  // LinkedHashMap keeps creation order, which the UI relies on for a stable list.
  private final Map<String, EventType> eventTypes =
      Collections.synchronizedMap(new LinkedHashMap<>());

  @Override
  public List<EventType> findAll() {
    synchronized (eventTypes) {
      return List.copyOf(eventTypes.values());
    }
  }

  @Override
  public Optional<EventType> findById(String id) {
    return Optional.ofNullable(eventTypes.get(id));
  }

  @Override
  public boolean saveIfAbsent(EventType eventType) {
    return eventTypes.putIfAbsent(eventType.getId(), eventType) == null;
  }
}
