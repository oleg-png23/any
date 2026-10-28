# Изменения

## californium 0.2.1

- Simplified the Flow home screen and clarified that californium is independent and Google sign-in happens inside the app.
- Renamed the checks destination to status and distinguished a loaded page from verified access to generation. Adapter labels describe the setting separately from component readiness.
- Made the current browser domain more prominent, separated stop from close and moved secondary actions into the page menu.
- Added UI coverage for the disclosure, status, browser host, disabled adapter and the primary action at large text sizes. Feature-branch CI does not publish a release.

## californium 0.2.0

- Renamed the app, launcher icon, diagnostic exports and APK to californium.
- Removed all services except Flow, along with catalog search and favorites. Added a dedicated Flow home and migrated legacy preferences.
- Restricted adapter documents to canonical and legacy Flow routes. Existing response transforms remain covered by regression tests.
- Added expressive press feedback, browser open/close transitions, setting and status motion, animated confirmation dialogs and motion captures. Progress updates retain a single browser host.


## 0.1.8

- Replaced the tall card feed with a compact two-column service grid inspired by Pixel quick access controls; large text uses a full-width column.
- Adopted Material 3 Expressive, its spring motion scheme, short navigation bar, floating toolbar and wavy page progress. Settings use connected surfaces and visual theme choices.
- Added full-tile vector backgrounds, bundled Material Symbols Rounded, spring shape changes, filter reflow, favorite transitions and directional page motion.
- Added viewport, navigation-state and motion-capture UI checks while preserving browser and Flow adapter behavior.

## 0.1.7

- Added native loading cancellation and explicit stopped/retry UI without discarding the page or browser profile.
- Reload can resume a cancelled initial open even before the extension has finished installing. Manual stops do not produce network-error or successful-page records.
- Fixed Back while waiting for the browser extension; cancelling the open also prevents late preparation from reopening it. Browser Back now works without page history.
- Added UI/native cancellation regressions and a device screenshot of the page-actions sheet.


## 0.1.6

- Added local vector backgrounds to catalog card headers, adaptive grid layout and a spring shape transition on press.
- Active navigation destinations use filled icons. All status text now uses the surface's own content color, fixing low contrast after successful opening in dark mode.
- Replaced page-info dialogs with a scrollable Material 3 bottom sheet: export diagnostics, switch desktop mode and open externally without leaving the current service first.
- AI Studio access work is deferred by request; Flow's compatibility adapter is unchanged.


## 0.1.5

- Rebuilt the catalog around Material 3 tonal cards, display typography, segmented selection and a single prominent service action.
- Cards derive colors from MaterialTheme roles in both the fixed and wallpaper palettes; settings use grouped surfaces and the browser uses a tonal floating toolbar.
- AI Studio eligibility redirects now produce an access-restricted status, native explanation and a diagnostic event. The shared Google destination does not identify whether region, age or account is the cause.
- Diagnostic exports include desktop mode and supported adapter settings, without page URLs, account identifiers, cookies or response bodies.
- Flow's 0.1.4 compatibility adapter is unchanged. No AI Studio access fix is claimed without device evidence.


## 0.1.4

- Парсер RPC принимает пробелы и переносы вокруг JSON, LF/CRLF и длины UTF-16 без включённого перевода строки.
- Исходные разделители сохраняются, длина меняется на разницу полезного содержимого; количество буквальных Unicode-символов сохраняется.
- Уточнены причины ошибок кадров в журнале; повреждённые потоки отклоняются целиком, размер результата ограничен.
- Прямой APK публикуется в GitHub Releases после успешных проверок Android и адаптера.

## 0.1.3

- Яркая базовая палитра Google, более выразительные фоны карточек, круглый поиск и облегчённая типографика Material You.
- Цвета обоев на Android 12+ применяются и к карточкам сервисов; переключатель сохраняется в настройках.
- Центрированный значок aBypass с тематическим слоем Android 13+ и круглые значки сервисов фиксированного размера.
- При крупном шрифте и узком экране карточки перестраиваются в колонку, чтобы текст не зажимался между значками и кнопками.
- Проверки выравнивания при узком экране и увеличенном шрифте, палитр и безопасной области значка.

## 0.1.2

- Добавлена обработка четырёх известных RPC статуса доступности и сокращённых protobuf-массивов с отсутствующими или null-полями по умолчанию.
- Обрабатываются числовые/нулевые географические флаги и явно региональные значения availabilityState. Неизвестные общие строковые статусы не заменяются.
- Журнал показывает включение режима на странице и конкретную причину отказа парсера без содержимого запросов и ответов.
- Требуется повторная проверка Flow на устройстве: автоматические проверки не подтверждают доступ к генерации.

## 0.1.1

- Исправлены ранние fetch/XHR, отмена активных запросов из-за повторной настройки и обработка UTF-16-кадров. Географический флаг обрабатывается без требования соседних нецелевых полей.
- У каждой карточки сервиса свой градиентный фон и встроенный векторный рисунок: волны, сетка, орбиты или звёзды.
- Иконки сервисов вместо букв, иконки у фильтров, разделов, открытия и повторной загрузки.
- Анимации переходов, темы, избранного, нажатий, списка, подробностей и прогресса страницы.
- Поиск и положение списка сохраняются при переключении разделов.
- Более компактный заголовок каталога и сжатые нативные библиотеки для меньшего APK.
- Фоны и иконки работают без сети.

## 0.1.0

- Нативная Material 3 оболочка, адаптивная навигация, светлая/тёмная тема и динамические цвета.
- Каталог Flow, AI Studio, Google Labs и Gemini с поиском и избранным.
- GeckoView, общий профиль сайтов, навигация, всплывающие окна, выбор файлов, скачивание и локальная диагностика.
- Оригинальный ограниченный региональный адаптер для Flow/Labs.
- Автоматические тесты адаптера, JVM-проверки, Compose UI-тесты и сборка ARM64 APK в GitHub Actions.



