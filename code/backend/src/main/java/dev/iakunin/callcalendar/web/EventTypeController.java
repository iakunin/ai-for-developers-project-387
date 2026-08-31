package dev.iakunin.callcalendar.web;

import dev.iakunin.callcalendar.contract.model.EventType;
import dev.iakunin.callcalendar.contract.model.Slot;
import dev.iakunin.callcalendar.domain.EventTypeService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/event-types")
@RequiredArgsConstructor
public class EventTypeController {

  private final EventTypeService eventTypes;

  @GetMapping
  public List<EventType> list() {
    return eventTypes.findAll();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public EventType create(@RequestBody EventType eventType) {
    return eventTypes.create(eventType);
  }

  @GetMapping("/{id}")
  public EventType get(@PathVariable String id) {
    return eventTypes.getById(id);
  }

  /** Free slots only; busy intervals come from GET /api/bookings. */
  @GetMapping("/{id}/slots")
  public List<Slot> listSlots(@PathVariable String id) {
    return eventTypes.slotsOf(id);
  }
}
