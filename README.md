# Practice8 — Многомодульность

Ветка создана от `Practice7`. Функциональность просмотра и редактирования профиля
вынесена из `app` в Android Library `feature:profile`.

## Модули и зависимости

```text
:app ────────────> :feature:profile
  │                       │
  └─────────> :core:ui <───┘
```

| Модуль | Ответственность |
| --- | --- |
| `app` | Application, Activity, общая навигация, экраны треков, избранного и фильтров; создание `ProfileComponent` и передача его экранам профиля. |
| `feature:profile` | Просмотр и редактирование профиля, ViewModel и их фабрика, модели и интерфейсы репозиториев, валидация и сохранение, JSON-хранилище, галерея и камера, аватар, скачивание резюме, напоминания, собственные ресурсы и манифест. |
| `core:ui` | Общая Compose-тема и строки действий «Назад», «Готово», «Повторить», «Понятно», используемые несколькими модулями. |

Зависимости объявлены через `implementation(project(...))`. Новые модули
не зависят от `app` и не используют его классы или ресурсы. Такое направление
зависимостей соответствует [рекомендациям Android по модульности](https://developer.android.com/topic/modularization/patterns).

## Подключение профиля

`AppContainer` создаёт один `ProfileComponent` на время жизни приложения.
`AppNavigation` передаёт его в `ProfileRoute` и `ProfileEditRoute`; выход
из редактирования передаётся через `onBack`. Экраны не приводят `Context`
к `PracticeApplication` и не знают о навигации приложения.

Компонент внутри `feature:profile` создаёт общее хранилище профиля,
репозитории медиа и резюме, планировщик напоминаний и фабрику ViewModel.
Изменения после сохранения по-прежнему поступают на экран профиля через `StateFlow`.
Черновик редактирования сохраняется в `SavedStateHandle`.

Разрешения, FileProvider, XML с путями файлов и BroadcastReceiver принадлежат
манифесту `feature:profile` и объединяются с манифестом приложения при сборке.
В authority используется `${applicationId}.files`, а не фиксированное имя пакета.
Напоминание открывает стартовую Activity через `PackageManager`, без зависимости
от `MainActivity`. Формат `profile.json`, каталоги файлов и authority сохранены.
Имя компонента `ClassReminderReceiver` также сохранено: он находится в модуле
профиля с прежним package, чтобы уже установленные напоминания продолжили работать.

## Проверка

```powershell
.\gradlew.bat :app:testDebugUnitTest :feature:profile:testDebugUnitTest
.\gradlew.bat :app:assembleDebug :app:assembleRelease
.\gradlew.bat :app:lintDebug :feature:profile:lintDebug :core:ui:lintDebug
.\gradlew.bat :app:connectedDebugAndroidTest :feature:profile:connectedDebugAndroidTest
```

Для инструментальных тестов нужен запущенный эмулятор или устройство.
`ProfileFlowTest` проверяет системные разрешения, галерею, камеру, сохранение
и восстановление экрана; ему нужна чистая установка приложения.
`ProfileModuleIntegrationTest` проверяет объединение манифестов и authority
FileProvider в итоговом приложении. Тесты ViewModel и хранилища профиля находятся
в `feature:profile`; проверки навигации между функциями остаются в `app`.

Самостоятельная сборка библиотеки профиля:

```powershell
.\gradlew.bat :feature:profile:assembleRelease
```

Эта задача собирает профиль и его зависимость `core:ui` без компиляции `app`.
