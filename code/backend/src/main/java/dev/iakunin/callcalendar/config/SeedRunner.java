package dev.iakunin.callcalendar.config;

import dev.iakunin.callcalendar.contract.model.EventType;
import dev.iakunin.callcalendar.domain.EventTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Storage is in memory, so the demo event types are written again on every start. */
@Component
@RequiredArgsConstructor
public class SeedRunner implements ApplicationRunner {

  private final CalendarProperties properties;
  private final EventTypeService eventTypes;

  @Override
  public void run(ApplicationArguments args) {
    for (CalendarProperties.EventTypeSeed seed : properties.seed().eventTypes()) {
      eventTypes.create(
          EventType.builder()
              .id(seed.id())
              .title(seed.title())
              .description(seed.description())
              .durationMinutes(seed.durationMinutes())
              .build());
    }
  }
}
