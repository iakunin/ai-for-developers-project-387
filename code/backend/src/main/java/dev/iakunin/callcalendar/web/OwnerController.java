package dev.iakunin.callcalendar.web;

import dev.iakunin.callcalendar.config.CalendarProperties;
import dev.iakunin.callcalendar.contract.model.Owner;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/owner")
@RequiredArgsConstructor
public class OwnerController {

  private final CalendarProperties properties;

  /** The single predefined profile; there is no registration in this application. */
  @GetMapping
  public Owner get() {
    return Owner.builder().id(properties.owner().id()).name(properties.owner().name()).build();
  }
}
