package dev.iakunin.callcalendar.domain;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Set;

/** Opening hours of the calendar owner, expressed in the owner's own timezone. */
public record WorkingSchedule(
    ZoneId zone, Set<DayOfWeek> workingDays, LocalTime open, LocalTime close) {

  public boolean isWorkingDay(LocalDate day) {
    return workingDays.contains(day.getDayOfWeek());
  }

  public Instant openInstant(LocalDate day) {
    return day.atTime(open).atZone(zone).toInstant();
  }

  public Instant closeInstant(LocalDate day) {
    return day.atTime(close).atZone(zone).toInstant();
  }
}
