package dev.iakunin.callcalendar.config;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Every rule the contract leaves to the backend lives here, never as a constant in code. */
@ConfigurationProperties(prefix = "call-calendar")
public record CalendarProperties(
    Owner owner, Schedule schedule, Booking booking, Cors cors, Seed seed) {

  public record Owner(String id, String name) {}

  /** Working hours of the calendar owner, in the owner's own timezone. */
  public record Schedule(
      ZoneId timezone, Set<DayOfWeek> workingDays, LocalTime open, LocalTime close) {}

  public record Booking(int windowDays) {}

  public record Cors(List<String> allowedOrigins) {}

  public record Seed(List<EventTypeSeed> eventTypes) {}

  public record EventTypeSeed(String id, String title, String description, int durationMinutes) {}
}
