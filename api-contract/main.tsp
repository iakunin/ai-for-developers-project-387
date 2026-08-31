import "@typespec/http";
import "@typespec/openapi";

using Http;
using OpenAPI;

@doc("""
  Календарь звонков.

  Контракт описывает внешнее поведение приложения и является единым источником
  правды для фронтенда и бэкенда.

  Роли:
  - Владелец календаря — единственный заранее заданный профиль. Регистрации и
    авторизации в приложении нет, поэтому админские операции не требуют
    аутентификации.
  - Гость — бронирует слоты без создания аккаунта и без входа в систему.

  Правила бронирования:
  - Окно записи — ${bookingWindowDays} дней, начиная с текущей даты.
  - На одно и то же время нельзя создать две записи, даже если это разные типы
    событий: занятость календаря общая для всех типов событий.
  """)
@service(#{ title: "Календарь звонков" })
@info(#{ version: "1.0.0" })
@route("/api")
namespace CallCalendar;

/** Длина окна записи в днях, начиная с текущей даты. */
const bookingWindowDays = 14;

// -----------------------------------------------------------------------------
// Доменные сущности
// -----------------------------------------------------------------------------

/** Владелец календаря — единственный заранее заданный профиль. */
model Owner {
  /** Идентификатор владельца. */
  id: string;

  /** Отображаемое имя владельца. */
  @minLength(1)
  name: string;
}

/** Тип события, который владелец предлагает гостям для бронирования. */
model EventType {
  /** Идентификатор типа события. Задается владельцем при создании. */
  @minLength(1)
  id: string;

  /** Название типа события. Показывается гостю на странице видов брони. */
  @minLength(1)
  title: string;

  /** Описание типа события. Показывается гостю на странице видов брони. */
  description: string;

  /** Длительность встречи в минутах. */
  @minValue(1)
  durationMinutes: int32;
}

/**
 * Свободный интервал в окне записи, на который гость может создать бронирование.
 * Слоты, пересекающиеся с любым существующим бронированием, свободными не
 * считаются и в выдачу не попадают.
 */
model Slot {
  /** Начало слота, UTC. */
  start: utcDateTime;

  /** Конец слота, UTC. Равен `start` плюс `durationMinutes` типа события. */
  end: utcDateTime;
}

/** Бронирование, созданное гостем на конкретный слот. */
model Booking {
  /** Идентификатор бронирования. Назначается сервером. */
  id: string;

  /** Идентификатор забронированного типа события. */
  eventTypeId: string;

  /** Начало встречи, UTC. */
  start: utcDateTime;

  /** Конец встречи, UTC. Равен `start` плюс `durationMinutes` типа события. */
  end: utcDateTime;

  /** Имя гостя. */
  guestName: string;

  /** Email гостя. */
  @format("email")
  guestEmail: string;

  /** Момент создания бронирования, UTC. */
  createdAt: utcDateTime;
}

/** Данные, которые гость отправляет для создания бронирования. */
model BookingCreate {
  /** Идентификатор выбранного типа события. */
  eventTypeId: string;

  /** Начало выбранного свободного слота, UTC. */
  start: utcDateTime;

  /** Имя гостя. */
  @minLength(1)
  guestName: string;

  /** Email гостя. */
  @minLength(1)
  @format("email")
  guestEmail: string;
}

// -----------------------------------------------------------------------------
// Ошибки
// -----------------------------------------------------------------------------

/**
 * Машиночитаемые коды ошибок. Клиент принимает решения по `code`, а не по
 * тексту `message`.
 */
enum ErrorCode {
  /** Тело запроса не проходит валидацию. */
  ValidationFailed: "validation_failed",

  /** Типа события с указанным идентификатором не существует. */
  EventTypeNotFound: "event_type_not_found",

  /** Тип события с таким `id` уже существует. */
  EventTypeIdTaken: "event_type_id_taken",

  /** `start` не совпадает с началом свободного слота. */
  SlotNotAvailable: "slot_not_available",

  /** `start` выходит за окно записи. */
  OutsideBookingWindow: "outside_booking_window",

  /** На это время уже есть бронирование любого типа события. */
  SlotTaken: "slot_taken",

  /** Бронирования с указанным идентификатором не существует. */
  BookingNotFound: "booking_not_found",
}

/** Тело ответа при ошибке. */
model ApiError {
  /** Машиночитаемый код ошибки. */
  code: ErrorCode;

  /** Человекочитаемое описание ошибки. */
  message: string;
}

/**
 * Данные запроса не проходят валидацию либо противоречат правилам бронирования.
 * Коды: `validation_failed`, `event_type_not_found`, `slot_not_available`,
 * `outside_booking_window`.
 */
@error
model BadRequest {
  @statusCode statusCode: 400;
  @body error: ApiError;
}

/**
 * Запрошенный ресурс не найден. Коды: `event_type_not_found`,
 * `booking_not_found`.
 */
@error
model NotFound {
  @statusCode statusCode: 404;
  @body error: ApiError;
}

/**
 * Запрос конфликтует с текущим состоянием: тип события с таким `id` уже есть
 * либо выбранное время занято другим бронированием.
 * Коды: `event_type_id_taken`, `slot_taken`.
 */
@error
model Conflict {
  @statusCode statusCode: 409;
  @body error: ApiError;
}

// -----------------------------------------------------------------------------
// API
// -----------------------------------------------------------------------------

@tag("Владелец")
@route("/owner")
interface OwnerProfile {
  /** Возвращает заранее заданный профиль владельца календаря. */
  @get
  get(): Owner;
}

@tag("Типы событий")
@route("/event-types")
interface EventTypes {
  /**
   * Список типов событий. Используется и владельцем, и публичной страницей
   * видов брони.
   */
  @get
  list(): EventType[];

  /** Создает тип события. Идентификатор задает владелец. */
  @post
  create(@body eventType: EventType): {
    @statusCode statusCode: 201;
    @body created: EventType;
  } | BadRequest | Conflict;

  /** Возвращает тип события по идентификатору. */
  @route("{id}")
  @get
  get(@path id: string): EventType | NotFound;

  @doc("""
    Свободные слоты выбранного типа события в окне записи: ${bookingWindowDays}
    дней, начиная с текущей даты. Слоты отсортированы по возрастанию `start`.
    """)
  @route("{id}/slots")
  @get
  listSlots(@path id: string): Slot[] | NotFound;
}

@tag("Бронирования")
@route("/bookings")
interface Bookings {
  /**
   * Предстоящие встречи по всем типам событий одним списком, отсортированные по
   * возрастанию `start`. Страница владельца.
   */
  @get
  list(): Booking[];

  /**
   * Создает бронирование гостя на выбранный свободный слот.
   *
   * Возвращает 400, если типа события не существует (`event_type_not_found`),
   * `start` не совпадает с началом слота (`slot_not_available`) или выходит за
   * окно записи (`outside_booking_window`); 409, если на это время уже есть
   * бронирование любого типа события (`slot_taken`).
   */
  @post
  create(@body booking: BookingCreate): {
    @statusCode statusCode: 201;
    @body created: Booking;
  } | BadRequest | Conflict;

  /**
   * Отменяет (удаляет) бронирование по идентификатору. Страница владельца.
   */
  @route("{id}")
  @delete
  delete(@path id: string): void | NotFound;
}
