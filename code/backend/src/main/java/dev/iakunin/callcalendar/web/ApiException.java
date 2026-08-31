package dev.iakunin.callcalendar.web;

import dev.iakunin.callcalendar.contract.model.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/** Every failure the contract names. Clients decide on the code, never on the message. */
@Getter
public class ApiException extends RuntimeException {

  private final ErrorCode code;
  private final HttpStatus status;

  private ApiException(ErrorCode code, HttpStatus status, String message) {
    super(message);
    this.code = code;
    this.status = status;
  }

  public static ApiException badRequest(ErrorCode code, String message) {
    return new ApiException(code, HttpStatus.BAD_REQUEST, message);
  }

  public static ApiException notFound(ErrorCode code, String message) {
    return new ApiException(code, HttpStatus.NOT_FOUND, message);
  }

  public static ApiException conflict(ErrorCode code, String message) {
    return new ApiException(code, HttpStatus.CONFLICT, message);
  }
}
