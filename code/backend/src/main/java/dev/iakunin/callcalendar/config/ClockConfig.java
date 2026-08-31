package dev.iakunin.callcalendar.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfig {

  /** Injected wherever time is read, so tests can freeze the 14-day window. */
  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }
}
