package dev.iakunin.callcalendar.domain;

import dev.iakunin.callcalendar.contract.model.ErrorCode;
import dev.iakunin.callcalendar.contract.model.EventType;
import dev.iakunin.callcalendar.contract.model.Slot;
import dev.iakunin.callcalendar.storage.EventTypeRepository;
import dev.iakunin.callcalendar.web.ApiException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventTypeService {

  private final EventTypeRepository repository;
  private final SlotService slots;

  public List<EventType> findAll() {
    return repository.findAll();
  }

  public EventType getById(String id) {
    return repository
        .findById(id)
        .orElseThrow(
            () ->
                ApiException.notFound(
                    ErrorCode.EVENT_TYPE_NOT_FOUND, "Типа события «" + id + "» не существует."));
  }

  public EventType create(EventType eventType) {
    validate(eventType);
    if (!repository.saveIfAbsent(eventType)) {
      throw ApiException.conflict(
          ErrorCode.EVENT_TYPE_ID_TAKEN,
          "Тип события с идентификатором «" + eventType.getId() + "» уже существует.");
    }
    return eventType;
  }

  /** Free slots of the event type; 404 when it does not exist. */
  public List<Slot> slotsOf(String eventTypeId) {
    return slots.freeSlots(getById(eventTypeId));
  }

  private static void validate(EventType eventType) {
    if (eventType.getId() == null || eventType.getId().isBlank()) {
      throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "Не указан идентификатор.");
    }
    if (eventType.getTitle() == null || eventType.getTitle().isBlank()) {
      throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "Не указано название.");
    }
    if (eventType.getDescription() == null) {
      throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "Не указано описание.");
    }
    if (eventType.getDurationMinutes() == null || eventType.getDurationMinutes() < 1) {
      throw ApiException.badRequest(
          ErrorCode.VALIDATION_FAILED, "Длительность должна быть не меньше одной минуты.");
    }
  }
}
