package dev.iakunin.callcalendar.web;

import dev.iakunin.callcalendar.contract.model.ApiError;
import dev.iakunin.callcalendar.contract.model.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Turns every failure into the contract's ApiError body. */
@RestControllerAdvice
@Slf4j
public class ApiExceptionHandler {

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ApiError> handleApiException(ApiException exception) {
    return ResponseEntity.status(exception.getStatus())
        .body(ApiError.builder().code(exception.getCode()).message(exception.getMessage()).build());
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiError> handleUnreadableRequest(
      HttpMessageNotReadableException exception) {
    return ResponseEntity.badRequest()
        .body(
            ApiError.builder()
                .code(ErrorCode.VALIDATION_FAILED)
                .message("Тело запроса не проходит валидацию.")
                .build());
  }

  @ExceptionHandler(Exception.class)
  public void handleUnexpected(Exception exception) throws Exception {
    if (!(exception instanceof ErrorResponse)) {
      log.error("Unhandled error", exception);
    }
    throw exception; // let Spring produce its own status (404, 405, 415, 500, …)
  }
}
