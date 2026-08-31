package dev.iakunin.callcalendar.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CalendarPropertiesTest {

  @Autowired private CalendarProperties properties;

  @Test
  void bindsTheOwnerProfile() {
    assertThat(properties.owner().id()).isEqualTo("owner");
    assertThat(properties.owner().name()).isNotBlank();
  }

  @Test
  void bindsTheWorkingSchedule() {
    assertThat(properties.schedule().timezone()).isEqualTo(ZoneId.of("Europe/Moscow"));
    assertThat(properties.schedule().open()).isEqualTo(LocalTime.of(9, 0));
    assertThat(properties.schedule().close()).isEqualTo(LocalTime.of(18, 0));
    assertThat(properties.schedule().workingDays())
        .containsExactlyInAnyOrder(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY);
  }

  @Test
  void bindsTheFourteenDayBookingWindow() {
    assertThat(properties.booking().windowDays()).isEqualTo(14);
  }

  @Test
  void bindsThreeSeededEventTypes() {
    assertThat(properties.seed().eventTypes()).hasSize(3);
    assertThat(properties.seed().eventTypes().getFirst().id()).isEqualTo("intro-call");
    assertThat(properties.seed().eventTypes().getFirst().durationMinutes()).isEqualTo(15);
  }

  @Test
  void bindsTheDevelopmentCorsOrigin() {
    assertThat(properties.cors().allowedOrigins()).contains("http://localhost:5173");
  }
}
