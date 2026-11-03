package expert.nbt.nutrimind.tests;

import com.microsoft.playwright.*;
import expert.nbt.nutrimind.config.TestConfig;
import expert.nbt.nutrimind.pages.LoginPage;
import org.testng.ITestResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.reflect.Method;
import io.qameta.allure.Allure;
import java.io.ByteArrayInputStream;
import org.testng.annotations.*;
import java.nio.file.*;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.setDefaultAssertionTimeout;

public abstract class BaseTest {
    private static final Logger log = LoggerFactory.getLogger(BaseTest.class);
    private Playwright playwright;
    private Browser browser;
    protected BrowserContext context;
    protected Page page;
    protected LoginPage login;

    @BeforeClass(alwaysRun = true)
    public void startBrowser() {
        log.info("Starting Chromium (headless={})", TestConfig.headless());
        // Chromium is installed explicitly by the documented setup/CI step.
        playwright = Playwright.create(new Playwright.CreateOptions()
                .setEnv(java.util.Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")));
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(TestConfig.headless()));
        setDefaultAssertionTimeout(TestConfig.timeout());
    }
    @BeforeMethod(alwaysRun = true)
    public void newSession(Method method) {
        log.info("START {}.{}", getClass().getSimpleName(), method.getName());
        context = browser.newContext(new Browser.NewContextOptions().setLocale("uk-UA").setViewportSize(1440, 1000));
        context.setDefaultTimeout(TestConfig.timeout());
        context.setDefaultNavigationTimeout(30000);
        context.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true).setSources(true));
        page = context.newPage();
        login = new LoginPage(page);
    }
    @AfterMethod(alwaysRun = true)
    public void finishSession(ITestResult result) throws Exception {
        String name = result.getMethod().getMethodName();
        long duration = result.getEndMillis() - result.getStartMillis();
        if (result.getStatus() == ITestResult.SUCCESS) {
            log.info("PASS {} ({} ms)", name, duration);
        } else if (result.getStatus() == ITestResult.SKIP) {
            log.warn("SKIP {} ({} ms)", name, duration);
        } else {
            log.error("FAIL {} ({} ms), cause={}", name, duration,
                    result.getThrowable() == null ? "unknown" : result.getThrowable().getClass().getSimpleName());
        }
        if (context == null) return;
        try {
            if (!result.isSuccess()) {
                Path directory = Paths.get("target", "artifacts", result.getMethod().getMethodName());
                Files.createDirectories(directory);
                int index = 0;
                for (Page current : context.pages()) {
                    if (!current.isClosed()) {
                        String attachmentName = "page-" + index++;
                        byte[] screenshot = current.screenshot(new Page.ScreenshotOptions()
                                .setPath(directory.resolve(attachmentName + ".png")).setFullPage(true));
                        Allure.addAttachment(attachmentName, "image/png", new ByteArrayInputStream(screenshot), ".png");
                        Allure.addAttachment(attachmentName + " URL", "text/plain", current.url());
                    }
                }
                Path trace = directory.resolve("trace.zip");
                context.tracing().stop(new Tracing.StopOptions().setPath(trace));
                log.info("Failure evidence saved to {}", directory.toAbsolutePath());
                try (var stream = Files.newInputStream(trace)) {
                    Allure.addAttachment("Playwright trace", "application/zip", stream, ".zip");
                }
            } else context.tracing().stop();
        } finally { context.close(); context = null; }
    }
    @AfterClass(alwaysRun = true)
    public void stopBrowser() {
        log.info("Closing browser and Playwright");
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }
    protected void openUkrainianLogin() {
        login.open();
        login.selectLanguage("UK");
        login.expectLoginForm();
    }
}
