# NutriMind — автоматизація UI тестів

Java 17+, Playwright Java, TestNG, Maven. Покриття восьми наданих мануальних сценаріїв. Тести працюють із реальним сайтом, без підміни API.

## Швидкий запуск

Потрібні JDK 17 або новіший та Maven 3.9+:

```bash
mvn exec:java -Dexec.args="install chromium"
mvn test
```

На Linux для встановлення системних залежностей:

```bash
mvn exec:java -Dexec.args="install --with-deps chromium"
```

Запуск із видимим браузером:

```bash
mvn test -Dheadless=false
```

Один тест:

```bash
mvn test -Dtest=AuthenticationTest#successfulLogin -Dheadless=false
```

## IntelliJ IDEA

1. Відкрити `pom.xml` як Maven проєкт і виконати Load/Reload Maven Project.
2. Вибрати Project SDK 17+ та відповідний JDK у Settings → Build Tools → Maven → Runner.
3. У Terminal IDEA один раз виконати команду встановлення Chromium вище.
4. Вибрати збережену конфігурацію **NutriMind UI** і натиснути Run. Вона виконує `mvn test -Dheadless=false`.
5. Альтернатива: Run біля класу/методу `AuthenticationTest` або на `src/test/resources/testng.xml`. Для прямого запуску TestNG задати VM option `-Dheadless=false`, якщо потрібне вікно браузера. Working directory — корінь проєкту.

## Конфігурація

Пріоритет: Java system property → змінна середовища → значення за замовчуванням.

| Property | Environment | Значення за замовчуванням |
|---|---|---|
| `baseUrl` | `BASE_URL` | `https://play.nutrimind.game.nbt.expert/` |
| `username` | `TEST_USERNAME` | `demo` |
| `password` | `TEST_PASSWORD` | `demo` |
| `headless` | `HEADLESS` | `true` |
| `timeout` | `TEST_TIMEOUT` | `15000` мс |

Для іншого акаунта задавайте `TEST_USERNAME` і `TEST_PASSWORD` через середовище. Значення `demo/demo` — тестові дані, уточнені користувачем. URL має завершуватися `/`. Кейс №1 використовує налаштований валідний пароль із `invalid@test.com`; кейс №2 — налаштований логін із паролем `1`.

## GitHub Actions

Workflow `.github/workflows/ui-tests.yml` запускається на push, pull request та вручну через Actions → NutriMind UI tests → Run workflow. Він встановлює Temurin JDK 17, Maven dependencies і Chromium із системними залежностями, виконує `mvn test` та завантажує звіти навіть при падінні.

За потреби додайте Repository secrets `TEST_USERNAME`, `TEST_PASSWORD` та Repository variable `BASE_URL`. Без них використовується наданий demo акаунт і цільовий URL. Зовнішні PR без доступу до secrets також використовують demo. Публікація в GitHub не виконувалася: workflow запрацює після додавання цього проєкту до репозиторію.

Артефакт `ui-test-results` зберігається 7 днів. Невдалий тест завершує job помилкою; автоматичні повтори не приховують результат.

## Покриття

| Кейс | Метод TestNG | Основні перевірки |
|---|---|---|
| 1 | `invalidEmail` | Дві видимі помилки під полями, текст, червоний колір, форма та незмінний URL |
| 2 | `invalidPassword` | Відмова у вході, текст помилок, форма та незмінний URL |
| 3 | `successfulLogin` | Індикатор «Вхід у систему...», `/step-one`, «Оберіть тип гри» |
| 4 | `logout` | Меню, підтвердження «Так», форма входу, захищений маршрут недоступний після виходу |
| 5 | `languageSwitching` | Послідовність RU → EN → UK, переклад полів, кнопки, обох посилань; відсутність видимих ключів перекладу |
| 6 | `loginPageLoaded` | HTTP 200 документа, логотип Nutri Mind, поля, кнопка і посилання |
| 7 | `recoveryNavigation` | Нова вкладка, URL скидання пароля та спеціалізована форма email |
| 8 | `registrationNavigation` | Нова вкладка `/uk/signup`, поля email і пароля, кнопка відправлення |

Кейс №5 перевіряє конкретні ключові елементи форми та їх видимість. Повну оцінку форматування всіх фрагментів і піксельної верстки слід доповнювати візуальним рев'ю. HTTP 200 у кейсі №6 стосується HTML документа, а не всіх сторонніх ресурсів.

## Структура та OOP

- `config/TestConfig` — параметри запуску.
- `pages/BasePage` — базова абстракція сторінки, спільний стан і поведінка.
- `pages/LoginPage`, `GameSelectionPage`, `RecoveryPage`, `RegistrationPage` — інкапсуляція локаторів та дій сторінок, переходи повертають відповідні Page Objects.
- `tests/BaseTest` — життєвий цикл Playwright, браузера, ізольованих контекстів та артефактів.
- `tests/AuthenticationTest` — сценарії та перевірки.

Кожен тест починає з чистих cookies/localStorage/sessionStorage, явно обирає UK та не залежить від порядку запуску. Suite виконується послідовно: Playwright об'єкти не передаються між потоками. Використовуються auto-wait і assertions Playwright, без `Thread.sleep`.

## Звіти та діагностика

- `target/surefire-reports/index.html` — звіт TestNG.
- `target/surefire-reports/` — XML, текстові та HTML звіти.
- `target/artifacts/<method>/page-*.png` — скриншоти всіх відкритих вкладок невдалого тесту.
- `target/artifacts/<method>/trace.zip` — trace невдалого тесту.

Відкрити trace локально:

```bash
mvn exec:java -Dexec.args="show-trace target/artifacts/recoveryNavigation/trace.zip"
```

Trace може містити дані сесії тестового акаунта; поширюйте його лише серед відповідних учасників проєкту.

## Виявлені розбіжності на сайті

Перевірено 3 листопада 2026 року:

- Початкова мова залежить від середовища; спостерігалася EN, тому UK встановлюється явно.
- Актуальна помилка входу: «Ім'я користувача та пароль не знайдено. Перевірте ще раз!». Це відповідає змісту прикладу з вимог.
- Реєстрація та відновлення відкриваються в новій вкладці на `my.nutrimind.game.nbt.expert`.
- **TC-07: дефект продукту.** Посилання «Відновити пароль» веде на `/ru/login` — звичайну форму входу з паролем. Очікується `/…/request-password-reset` та email форма скидання. Тест навмисно перевіряє вимогу і має падати до виправлення сайту. Він не натискає додаткове посилання «скинути пароль», щоб не приховати неправильний перехід.

Жоден тест не створює акаунт і не надсилає запит на скидання пароля.

Підхід до ізоляції TestNG сесій відповідає [офіційній документації Playwright Java](https://playwright.dev/java/docs/test-runners#testng).

## Результат перевірки

Локальний `mvn -B clean test` від 3 листопада 2026 року: **8 тестів, 7 passed, 1 failed, 0 skipped** (Chromium, headless; JDK 25 із compilation release 17). Успішні кейси: 1–6 та 8, включно з авторизацією `demo/demo`, підтвердженим виходом і захистом маршруту після виходу. TC-07 падає через описаний вище неправильний URL. Команда повертає exit code 1, тож GitHub Actions також залишатиметься червоним до виправлення продукту. Запуск у віддаленому GitHub Actions та GUI IntelliJ IDEA у цій сесії не виконувався.

## Allure

Allure TestNG автоматично записує результати як під час Maven запуску, так і під час прямого TestNG запуску в IDEA (working directory — корінь проєкту). Залежність реєструє listener через ServiceLoader; додатково в `testng.xml` його реєструвати не потрібно. Метадані групують сценарії в NutriMind → Авторизація та навігація. У невдалих тестах секція After fixtures → finishSession містить скриншоти відкритих вкладок, їх URL і Playwright trace ZIP.

```bash
mvn clean test
# Виконати також після невдалих тестів:
mvn allure:report
# Відкрити інтерактивний звіт у браузері:
mvn allure:serve
```

`allure:report` генерує `target/allure-report`, сирі результати — `target/allure-results`. `allure:serve` запускає локальний сервер до Ctrl+C; окремо встановлювати Allure CLI не потрібно, його завантажує Maven plugin. Для IDEA команди можна виконати у Terminal або через Maven tool window → Plugins → allure. Перед новим повним прогоном використовуйте `clean`, щоб не змішувати результати різних прогонів. Використано Allure Java 2.35.5, Maven plugin 2.18.0 та Allure Report 2.46.1; AspectJ не потрібен, вкладення додаються через runtime API.

GitHub Actions генерує HTML звіт навіть після падіння тестів та додає `allure-results` і `allure-report` до артефакту `ui-test-results`. Статус невдалих тестів залишається failed. Після завантаження артефакту сирі результати можна скопіювати в локальний `target/allure-results` і відкрити через `mvn allure:serve`.

[Документація Allure TestNG](https://allurereport.org/docs/testng/).

Перевірка Allure: `mvn -B clean test` сформував 8 результатів (7 passed, 1 failed — TC-07); `mvn -B allure:report` завершився BUILD SUCCESS. Перевірено наявність HTML звіту та всіх п’яти вкладень невдалого тесту.
