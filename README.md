# Практика 4 — Работа с сетью

Музыкальный каталог на Kotlin и Jetpack Compose. Практика 3 (список, детали и нижняя навигация) уже объединена с `master`; сетевые изменения находятся в ветке `Practice4`.

## Что реализовано

- Реальная загрузка треков из [iTunes Search API](https://performance-partners.apple.com/search-api), без API-ключа.
- Поиск по исполнителю или названию: `GET /search?term=Daft%20Punk&limit=50&entity=song&media=music&country=US`.
- Детали выбранного трека: `GET /lookup?id=697195462&entity=song&country=US`.
- Retrofit 3, OkHttp, `suspend`-запросы и `Dispatchers.IO` в repository; управление запросами через `viewModelScope`.
- `StateFlow` и `collectAsStateWithLifecycle` для состояния экранов. Отображаются загрузка, результат, пустой ответ и ошибка с повтором запроса.
- Новый поиск отменяет предыдущий. Повтор использует последний отправленный запрос, даже если текст поля уже изменился.
- Раздельные ошибки соединения, таймаута, HTTP, ограничения частоты запросов и некорректного ответа. Отмена корутины не превращается в ошибку интерфейса.
- ID деталей хранится в `SavedStateHandle`; экран может повторно загрузить трек после восстановления процесса.
- Chucker собирает запросы в debug-сборке. Кнопка **«Сетевые запросы»** открывает инспектор; разрешение на уведомления для этого не требуется. В release используется `library-no-op`, кнопка скрыта.

## Архитектура

```text
ui / navigation / viewmodel       — представление, события и StateFlow
             ↓
domain/usecase                   — SearchTracksUseCase, GetTrackDetailsUseCase
domain/repository                — контракт TrackRepository
domain/model                     — Track, TrackError, TrackException
             ↑
data/NetworkTrackRepository      — запросы на IO, преобразование ошибок
data/remote                      — ItunesApi, DTO и преобразование DTO → Track
di/AppContainer                  — сборка зависимостей в Application
```

Доменный слой не зависит от Android, Retrofit или Gson. Экраны не вызывают API напрямую. Для деталей используется отдельная ViewModel, связанная с записью в back stack. Моковые данные остались только в `androidTest` для воспроизводимых тестов навигации. Изображения и аудио пока не загружаются; обложка остаётся декоративной заглушкой из предыдущей практики. Кэш запросов в этой практике не реализован.

## Проверка

Нужны Android SDK 37 и JDK 25 для Gradle daemon (настройка toolchain уже в проекте). В `local.properties` задайте свой путь `sdk.dir`.

```powershell
.\gradlew.bat :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest :app:lintDebug
# При запущенном эмуляторе или подключённом устройстве:
.\gradlew.bat :app:connectedDebugAndroidTest
```

Unit-тесты проверяют параметры HTTP-запросов через MockWebServer, преобразование ответов, HTTP 503/429, повреждённый JSON, таймаут, отсутствие соединения, состояния ViewModel, отмену старого поиска и повтор запроса. Инструментальные тесты проверяют список, детали, системную кнопку «Назад», нижнюю навигацию и сохранение позиции списка.

Проверено 12.10.2026: 18 unit-тестов и 6 инструментальных тестов прошли на Pixel 8 (API 37); debug/release-сборки и Lint завершились успешно. У Lint остались предупреждения о версиях зависимостей и неиспользуемых ресурсах исходного проекта.

## Материалы для сдачи

- [Запись работы приложения](docs/practice4/network-demo.webm): загрузка списка, детали, изменение поискового запроса, отключение интернета, ошибка, повтор после восстановления соединения и просмотр запросов в Chucker.
- [Скриншот запроса поиска в Chucker](docs/practice4/chucker-search.png).
- [Скриншот запроса деталей в Chucker](docs/practice4/chucker-lookup.png).
- [Скриншот ошибки без интернета](docs/practice4/offline-error.png).
- [Скриншот состояния загрузки](docs/practice4/loading.png).

В записи сокращены паузы между действиями. Интернет на эмуляторе отключался командами `adb shell svc wifi disable` и `adb shell svc data disable`, после чего был восстановлен командами с `enable`.

Чтобы повторить демонстрацию: запустите debug-сборку, дождитесь списка, откройте трек и вернитесь. Найдите другого исполнителя. Отключите **Wi-Fi и мобильные данные**, нажмите «Обновить», дождитесь ошибки. Верните соединение и нажмите «Повторить». Откройте «Сетевые запросы» и выберите `search` или `lookup`: в деталях Chucker видны URL, query-параметры, статус и тело ответа.

Pull request создаётся из `Practice4` в `master` по [общей схеме сдачи](https://github.com/MobileDevelopmentUrfuAutumn2024/ConsecutivePracticesReadme). Его ссылку нужно добавить в соответствующий столбец ведомости; объединять PR следует после принятия практики преподавателем.
