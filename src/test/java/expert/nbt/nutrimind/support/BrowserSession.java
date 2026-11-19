package expert.nbt.nutrimind.support;

import com.microsoft.playwright.*;
import expert.nbt.nutrimind.config.TestConfig;
import expert.nbt.nutrimind.reporting.ArtifactCollector;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.setDefaultAssertionTimeout;

public final class BrowserSession implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(BrowserSession.class);
    private final TestConfig config;
    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;
    private boolean tracing;
    private boolean sensitive;

    public BrowserSession(TestConfig config) { this.config = config; }
    public void start() {
        log.info("Starting Chromium (headless={})", config.headless());
        playwright = Playwright.create(new Playwright.CreateOptions().setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")));
        try { browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(config.headless())); }
        catch (RuntimeException e) { playwright.close(); playwright = null; throw e; }
        setDefaultAssertionTimeout(config.timeout());
    }
    // Ownership of this context and its pages remains with this session.
    @SuppressWarnings("resource")
    public Page newPage() {
        sensitive = false;
        tracing = false;
        context = browser.newContext(new Browser.NewContextOptions().setLocale("uk-UA").setViewportSize(1440, 1000));
        context.setDefaultTimeout(config.timeout());
        context.setDefaultNavigationTimeout(30000);
        context.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true).setSources(true));
        tracing = true;
        Page page = context.newPage();
        page.onResponse(response -> {
            if (response.status() >= 500 && response.request().method().equals("POST")
                    && java.net.URI.create(response.url()).getPath().endsWith("/user/login")) {
                log.warn("Authentication endpoint returned HTTP {}", response.status());
            }
        });
        return page;
    }
    public void protect() {
        // Stop recording BEFORE either credential is passed to Playwright.
        sensitive = true;
        if (tracing) {
            tracing = false;
            try { context.tracing().stop(); }
            catch (RuntimeException e) { log.warn("Unable to stop private recording ({})", e.getClass().getSimpleName()); }
        }
    }
    public void finish(ArtifactCollector collector, String testId, boolean failed) {
        try { collector.finish(context, tracing, sensitive, testId, failed); }
        finally {
            tracing = false;
            if (context != null) {
                try { context.close(); }
                catch (RuntimeException e) { log.warn("Context cleanup failed ({})", e.getClass().getSimpleName()); }
                finally { context = null; }
            }
        }
    }
    @Override public void close() {
        try {
            if (browser != null) browser.close();
        } catch (RuntimeException e) { log.warn("Browser cleanup failed ({})", e.getClass().getSimpleName()); }
        finally {
            if (playwright != null) {
                try { playwright.close(); }
                catch (RuntimeException e) { log.warn("Playwright cleanup failed ({})", e.getClass().getSimpleName()); }
            }
        }
    }
}
