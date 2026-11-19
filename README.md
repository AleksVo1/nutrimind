# NutriMind UI Tests

## Technologies and frameworks

| Technology | Version | Purpose |
|---|---|---|
| Java | 17+ | Programming language |
| Maven | 3.9+ | Dependency management and test execution |
| Playwright Java | 1.64.0 | Chromium browser automation |
| TestNG | 7.11.0 | Test framework |
| Allure TestNG | 2.35.5 | Test result collection |
| Allure Report | 2.46.1 | HTML reports and history |
| Allure Maven Plugin | 2.18.0 | Report generation and viewing |
| SLF4J + slf4j-simple | 2.0.20 | Logging |
| GitHub Actions / Pages | — | CI and report hosting |

The tests use OOP, SOLID, and Page Objects. Pages implement the `LoadablePage` contract; browser sessions and 
artifact collection are separate components, with reporting injected through `ArtifactSink`. Dependency versions 
are defined in `pom.xml`.

## Installation and execution

Requires JDK 17+ and Maven 3.9+. Run commands from the project root.

```bash
mvn exec:java -Dexec.args="install chromium"
mvn clean test
```

On Linux, install Chromium with its system dependencies:

```bash
mvn exec:java -Dexec.args="install --with-deps chromium"
```

Run without a browser window:

```bash
mvn clean test -Dheadless=true
```

Run a single test:

```bash
mvn test -Dtest=AuthenticationTest#successfulLogin -Dheadless=true
```

## IntelliJ IDEA

1. Open `pom.xml` as a Maven project and load the dependencies.
2. Set Project SDK to JDK 17+ and configure the JDK for Maven Runner.
3. Run the Chromium installation command in the terminal.
4. Run the saved **NutriMind UI** configuration, a TestNG class or method, or `src/test/resources/testng.xml`.

Set the working directory to the project root. For direct TestNG execution, add `-D...` parameters to VM options.
The **NutriMind UI** configuration runs tests with a visible browser window.

## Credentials and configuration

Provide `TEST_USERNAME` and `TEST_PASSWORD` for the primary account. Add `TEST_USER_2_EMAIL` and `TEST_USER_2_PASSWORD` 
together to configure the secondary account for future two-player game tests. Actual values must not be committed.

Local execution also reads `.secrets/users.properties` using those environment-variable names as property keys. 
This directory is ignored by Git and the local file has owner-only permissions. Environment variables take precedence 
over the file. Keep secrets out of Maven command-line arguments.

Optional overrides: `BASE_URL`, `HEADLESS`, and `TEST_TIMEOUT`. Authentication and navigation scenarios use only the primary account.

## Allure and diagnostics

After running the tests:

```bash
mvn allure:report
mvn allure:serve
```

`allure:serve` opens the report through a local server. Press Ctrl+C to stop it. The Maven plugin downloads the Allure CLI automatically.

| Path | Contents |
|---|---|
| `target/allure-results/` | Allure results |
| `target/allure-report/` | HTML report |
| `target/surefire-reports/` | TestNG / Maven reports |
| `target/artifacts/<method>-<account>/` | Screenshots and traces for failed tests |

Credential objects appear as `primary` or `secondary` in TestNG/Allure parameters. Login and logout scenarios run only for the primary account. Before credentials are entered, tracing is stopped 
and screenshots are disabled for that session. Failure messages are redacted before TestNG/Allure serialize them.

Allure configuration: `src/test/resources/allure.properties`. Running `mvn clean` deletes local results in `target`.

View a Playwright trace:

```bash
mvn exec:java -Dexec.args="show-trace target/artifacts/recoveryNavigation-default/trace.zip"
```

## Logging

Configuration: `src/test/resources/simplelogger.properties`. Project classes use INFO; third-party libraries use WARN.
Logs appear in the IntelliJ IDEA console and GitHub Actions output.

Enable DEBUG logging for project classes:

```bash
mvn test -Dorg.slf4j.simpleLogger.log.expert.nbt.nutrimind=debug
```

## GitHub Actions and Pages

Workflow: `.github/workflows/ui-tests.yml`. It runs on pushes (except to `allure-history`), pull requests, or manually
through **Actions → NutriMind UI tests → Run workflow**. The runner uses Ubuntu 24.04, Temurin JDK 17, and headless Chromium.

Configure credentials under **Settings → Secrets and variables → Actions**:

- `TEST_USERNAME` and `TEST_PASSWORD` — primary account (required).
- `TEST_USER_2_EMAIL` and `TEST_USER_2_PASSWORD` — secondary account reserved for two-player game tests.

Use repository variables for optional `BASE_URL` and `ALLURE_PAGES_URL` overrides.

To enable publishing, select **Settings → Pages → Build and deployment → Source → GitHub Actions**.
The repository and GitHub plan must support Pages. If branch rules are configured, allow GitHub Actions to update the `allure-history` branch.

The `deploy-report` job requires `contents: write`, `actions: read`, `pages: write`, and `id-token: write`;
these permissions are defined in the workflow. Reports are published from `main`, including runs with failed tests.
The `ui-test-results` artifact is retained for 7 days and contains full reports and diagnostics.
The public report excludes ZIP traces containing session data. Credential-bearing sessions produce no screenshots or traces, including in private artifacts.

## Allure archive

- [All saved runs](https://AleksVo1.github.io/nutrimind/).
- [Latest report](https://AleksVo1.github.io/nutrimind/latest/).
- Individual report: `runs/<run_id>-<attempt>/` relative to the Pages base URL.

The archive is stored in the `allure-history` branch without automatic report deletion.
Each newly published attempt has a separate entry. Available artifacts from earlier runs are imported automatically;
expired or deleted results cannot be recovered. Allure 2 History/Trends show the latest 20 runs,
while complete older reports remain accessible in the archive.

Local runs and runs from other branches or pull requests are not automatically added to this archive.
Running `mvn clean` locally does not affect the GitHub archive.

Run the framework safety checks:

```bash
mvn test -Dtest=FrameworkSafetyTest
```

Run the archive script checks:

```bash
python3 -m unittest discover -s .github/scripts/tests
```
