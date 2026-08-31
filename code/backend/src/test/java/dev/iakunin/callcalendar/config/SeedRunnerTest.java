package dev.iakunin.callcalendar.config;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iakunin.callcalendar.contract.model.EventType;
import dev.iakunin.callcalendar.storage.EventTypeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SeedRunnerTest {

  @Autowired private EventTypeRepository repository;

  @Test
  void seedsTheConfiguredEventTypesInOrder() {
    assertThat(repository.findAll())
        .extracting(EventType::getId)
        .containsExactly("intro-call", "consultation", "deep-dive");
  }

  @Test
  void seedsCompleteEventTypes() {
    EventType first = repository.findById("intro-call").orElseThrow();

    assertThat(first.getTitle()).isEqualTo("Знакомство");
    assertThat(first.getDescription()).isNotBlank();
    assertThat(first.getDurationMinutes()).isEqualTo(15);
  }
}
