# NutriMind — автоматизація UI тестів

Java 17+, Playwright Java, TestNG, Maven. Покриття дев’яти мануальних сценаріїв. Тести працюють із реальним сайтом, без підміни API.

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
| 7 | `recoveryNavigation` | Dev: нова вкладка `/ru/login`, форма авторизації кабінету та посилання `/ru/request-password-reset` |
| 8 | `registrationNavigation` | Нова вкладка `/uk/signup`, поля email і пароля, кнопка відправлення |
| 9 | `registrationPageDetails` | Точний URL реєстрації, поля E-mail і Пароль, кнопка «Реєстрація», заголовок і ланцюжок «Головна / Реєстрація» |

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
- **TC-07: підтверджена dev-поведінка.** За відео користувача «Відновити пароль» відкриває `/ru/login` з формою авторизації кабінету (E-mail, Пароль, «Авторизоваться») та посиланням «сбросить его.» на `/ru/request-password-reset`. Тест перевіряє саме цей перехід і доступність відновлення. Запит на скидання не надсилається.

Жоден тест не створює акаунт і не надсилає запит на скидання пароля.

Підхід до ізоляції TestNG сесій відповідає [офіційній документації Playwright Java](https://playwright.dev/java/docs/test-runners#testng).

## Результат перевірки

Історичний прогін до уточнення dev-вимог: 8 тестів, 7 passed, 1 failed (TC-07 очікував безпосередньо форму скидання). Після підтвердження користувачем сценарію з відео TC-07 оновлено: `/ru/login` є очікуваною сторінкою dev-середовища. Ці старі результати зберігаються в історії та не описують поточні очікування.

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

## Сумісність GitHub Actions

Workflow використовує `ubuntu-24.04`, `actions/checkout@v5`, `actions/setup-java@v5` та `actions/upload-artifact@v6`. У цих версіях actions runtime — Node.js 24. Закріплення Ubuntu 24.04 запобігає автоматичному переходу `ubuntu-latest` на інший образ.

Прогін GitHub Actions [41624860828](https://github.com/AleksVo1/nutrimind/actions/runs/41624860828): 7 passed, 1 failed (`recoveryNavigation`, за попереднім очікуванням прямого переходу до форми скидання). Exit code 1 спричинений тестом; попередження про runtime actions не були причиною падіння. Allure звіт і артефакт успішно створені. Оновлення workflow прибирає зазначені попередження; очікування TC-07 згодом уточнено користувачем для dev-середовища.

## Allure у браузері через GitHub Pages

1. У GitHub відкрийте **Settings → Pages → Build and deployment → Source → GitHub Actions**.
2. Запуште оновлені `.github/workflows/ui-tests.yml`, усі файли `.github/scripts/` і `pom.xml` у `main`.
3. Відкрийте **Actions → NutriMind UI tests**. Після тестів job `deploy-report` опублікує звіт навіть за наявності невдалих тестів.
4. Натисніть URL середовища `github-pages` або **Open report** у summary job `deploy-report`. Стандартна адреса цього репозиторію: https://AleksVo1.github.io/nutrimind/ (за відсутності власного домену).

Посилання відкриває архів усіх збережених прогонів із `main`, а `latest/` — останній звіт. PR і сторонні гілки архів не перезаписують. Публічний звіт містить результати та скриншоти; ZIP trace із даними сесії виключається зі спеціальної копії результатів. Повний звіт та trace зберігаються в `ui-test-results`. GitHub Pages може бути загальнодоступним: не використовуйте для цих тестів акаунти з приватними даними.

Workflow залишає job тестів червоним, якщо тест впав, незалежно від успіху публікації. Налаштування Pages та перша віддалена публікація виконуються після push.

## Логування SLF4J

Додано `slf4j-api` та один provider `slf4j-simple` версії 2.0.20. Логи містять час, потік, клас, початок і результат тесту (PASS/FAIL/SKIP), тривалість, основні переходи та шлях до діагностичних артефактів. Значення логіна й пароля не передаються в логер.

Конфігурація — `src/test/resources/simplelogger.properties`: для класів проєкту INFO, для сторонніх бібліотек WARN. Логи доступні в консолі IDEA та кроці Run TestNG suite у GitHub Actions. Попередження `No SLF4J providers were found` усувається підключеним provider.

Увімкнути DEBUG для проєкту:

```bash
mvn test -Dorg.slf4j.simpleLogger.log.expert.nbt.nutrimind=debug
```

Для прямого запуску TestNG в IDEA ту саму властивість можна задати у VM options.

## Повний архів запусків Allure

Головна адреса Pages відкриває таблицю всіх збережених запусків: номер, спроба, дата, commit, статистика та посилання на логи Actions. Кожний звіт має постійну адресу `runs/<run_id>-<attempt>/`, останній — `latest/`. Повторний запуск Actions створює окремий запис; повторна публікація тієї самої спроби не дублює його.

Повні HTML звіти й `runs.json` зберігаються в окремій гілці `allure-history`. Перед публікацією workflow відновлює цю гілку, додає звіт, зберігає її та публікує весь архів. Звітів не видаляє автоматично. Після закінчення 7-денного строку Actions artifacts HTML архів продовжує працювати. Не видаляйте гілку `allure-history`; вона містить дані архіву. Розмір сховища зростатиме з кількістю запусків, у межах обмежень GitHub repository/Pages.

При наступній публікації скрипт також переглядає доступні `ui-test-results` старих запусків цього workflow у `main` і відновлює звіти з їхніх `allure-results`. Прострочені/видалені артефакти та спроби, результати яких уже перезаписані, відновити неможливо. Для старих запусків імпортується остання доступна спроба; усі нові опубліковані спроби зберігаються окремо. Завершені запуски без результатів тестів не утворюють Allure звіт. PR і сторонні гілки залишаються лише в артефактах, як і раніше.

Перед генерацією нового звіту переноситься `history` попереднього, а `executor.json` містить номер і URL прогону. Це вмикає History та Trends із переходами на архівні звіти. Allure 2 показує останні 20 прогонів у цих вкладках; старіші повні звіти доступні через архів без такого обмеження. [Документація Allure](https://allurereport.org/docs/history-and-retries/).

Workflow потребує `contents: write` тільки в job `deploy-report`, щоб зберігати службову гілку, та `actions: read` для відновлення артефактів. Результат тестів залишається незалежним від публікації. Публікації виконуються послідовно, а push у `allure-history` не запускає тести. За наявності branch rules дозвольте GitHub Actions оновлювати цю службову гілку. Для власного домену Pages задайте repository variable `ALLURE_PAGES_URL` із повною базовою адресою звітів.

Перевірка архівування:

```bash
python3 -m unittest discover -s .github/scripts/tests
```

Для першого ввімкнення запуште оновлений workflow та всі файли `.github/scripts/` у `main`. Віддалена публікація й імпорт старих артефактів виконаються в GitHub Actions; локальні перевірки не змінюють репозиторій на GitHub.

Перевірено локально: 3 автоматичні перевірки архіву (25 прогонів, повторні спроби, фільтрація імпорту), створення/відновлення гілки на тимчасовому Git remote та генерація двох реальних Allure звітів із History/Trends і коректними URL архіву.

## TC-09 — Відкриття сторінки Реєстрація

Додатковий тест `registrationPageDetails` відкриває `https://play.nutrimind.game.nbt.expert/`, встановлює UK, знаходить «Зареєструватися» під формою входу та відкриває посилання. Після завантаження перевіряє точний URL `https://my.nutrimind.game.nbt.expert/uk/signup`, видимі поля «E-mail» та «Пароль» (тип password), видиму активну кнопку «Реєстрація», заголовок сторінки та порядок елементів навігаційного ланцюжка «Головна / Реєстрація» з посиланням на головну.

Користувач підтвердив `nbt.expert` замість початкового `nkt.expert`, який не знаходиться через DNS. Для підтвердженого сайту чинна адреса — `/uk/signup`, а не `/#/signup`. Посилання відкриває нову вкладку; тест чекає її завантаження. Дані у форму не вводяться, акаунт не створюється. TC-08 збережено; новий кейс включений до загальної suite та Allure.

```bash
mvn test -Dtest=AuthenticationTest#registrationPageDetails -Dheadless=true
```

Перевірка TC-09: окремий запуск у Chromium headless завершився BUILD SUCCESS — 1 test, 0 failures, 0 skipped.

## Уточнений TC-07 — очікувана поведінка dev

Підстава: запис користувача `Recording at 2026-11-03 16.36.57.mp4` та підтвердження, що показана поведінка очікувана.

1. Відкрити сторінку входу гри українською мовою.
2. Натиснути «Відновити пароль».
3. Дочекатися завантаження нової вкладки `https://my.nutrimind.game.nbt.expert/ru/login`.
4. Перевірити заголовок «Авторизация», поля E-mail та Пароль, кнопку «Авторизоваться».
5. Перевірити видиме посилання «сбросить его.» з адресою `/ru/request-password-reset`.

Початкова вкладка авторизації гри залишається доступною. Безпосереднє відкриття форми скидання після першого кліку не очікується. Старі невдалі записи в Allure збережуться як історичні; нові прогони використовують уточнені перевірки.

Після уточнення dev-поведінки перевірено `mvn -B clean test -Dheadless=true`: **9 tests, 0 failures, 0 errors, 0 skipped — BUILD SUCCESS**.
