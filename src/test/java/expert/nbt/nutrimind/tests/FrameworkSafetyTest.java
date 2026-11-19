package expert.nbt.nutrimind.tests;

import com.microsoft.playwright.*;
import expert.nbt.nutrimind.config.Credentials;
import expert.nbt.nutrimind.reporting.ArtifactCollector;
import expert.nbt.nutrimind.reporting.AllureArtifactSink;
import expert.nbt.nutrimind.support.SecretRedactor;
import expert.nbt.nutrimind.support.LoginProgressObserver;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public final class FrameworkSafetyTest {
    // These Page proxies do not allocate browser resources.
    @Test public void diagnosticErrorsDoNotInterruptTraceCleanup() throws Exception {
        AtomicInteger stopped = new AtomicInteger();
        AtomicInteger healthyScreenshot = new AtomicInteger();
        Page broken = fake(Page.class, (proxy, method, args) -> switch (method.getName()) {
            case "isClosed" -> false;
            case "screenshot" -> throw new PlaywrightException("simulated screenshot failure");
            default -> unusedResult(method);
        });
        Page healthy = fake(Page.class, (proxy, method, args) -> switch (method.getName()) {
            case "isClosed" -> false;
            case "screenshot" -> { healthyScreenshot.incrementAndGet(); yield new byte[]{1}; }
            case "url" -> "https://example.invalid/";
            default -> unusedResult(method);
        });
        assertNotEquals(broken, healthy);
        assertEquals(broken.hashCode(), System.identityHashCode(broken));
        Tracing trace = fake(Tracing.class, (proxy, method, args) -> {
            if (method.getName().equals("stop")) {
                stopped.incrementAndGet();
                if (args != null && args[0] instanceof Tracing.StopOptions options && options.path != null) {
                    Files.write(options.path, new byte[]{1});
                }
            }
            return unusedResult(method);
        });
        BrowserContext context = fake(BrowserContext.class, (proxy, method, args) -> switch (method.getName()) {
            case "pages" -> List.of(broken, healthy);
            case "tracing" -> trace;
            default -> unusedResult(method);
        });
        new ArtifactCollector(new AllureArtifactSink() { @Override public void attach(String name, String type, byte[] bytes, String extension) { throw new IllegalStateException("simulated sink failure"); } },
                Files.createTempDirectory("nutrimind-artifact-safety-"))
                .finish(context, true, false, "safety-check", true);
        assertEquals(stopped.get(), 1);
        assertEquals(healthyScreenshot.get(), 1, "A broken tab must not prevent collecting the next tab");
    }
    @Test public void privateSessionsProduceNoScreenshotsOrTraceAttachments() {
        BrowserContext context = fake(BrowserContext.class, (proxy, method, args) -> {
            throw new AssertionError("Private context must not be inspected by artifact collection");
        });
        new ArtifactCollector(new AllureArtifactSink() { @Override public void attach(String name, String type, byte[] bytes, String extension) { throw new AssertionError("Private attachment"); } })
                .finish(context, false, true, "private-safety-check", true);
    }
    @Test public void credentialsAreSafeForReportParameters() {
        var credentials = new Credentials("secondary", "private@example.invalid", "fake-password");
        assertEquals(credentials.toString(), "secondary");
    }
    @Test public void failuresRedactPlainAndEncodedSecretsWithoutRetainingCauses() {
        var credentials = new Credentials("secondary", "private@example.invalid", "fake-password");
        var redactor = new SecretRedactor(List.of(credentials));
        var original = new AssertionError("fill private@example.invalid fake-password private%40example.invalid",
                new RuntimeException("fake-password"));
        Throwable safe = redactor.sanitize(original);
        assertFalse(safe.getMessage().contains(credentials.username()));
        assertFalse(safe.getMessage().contains(credentials.password()));
        assertFalse(safe.getMessage().contains("private%40example.invalid"));
        assertNull(safe.getCause());
        assertEquals(safe.getSuppressed().length, 0);
    }
    @Test public void remembersProgressAfterTheSpinnerHasDisappeared() {
        try (Playwright playwright = Playwright.create(new Playwright.CreateOptions()
                .setEnv(java.util.Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")));
             Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
             BrowserContext context = browser.newContext()) {
            Page page = context.newPage();
            page.setContent("""
                <button onclick="document.querySelector('span').hidden=false;
                  requestAnimationFrame(()=>requestAnimationFrame(()=>{
                    document.querySelector('span').hidden=true;window.finished=true}))">Submit</button>
                <span hidden>Вхід у систему...</span>
                """);
            try (var progress = new LoginProgressObserver(page)) {
                page.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON,
                        new Page.GetByRoleOptions().setName("Submit").setExact(true)).click();
                page.waitForFunction("window.finished === true");
                assertFalse(page.getByText("Вхід у систему...").isVisible());
                progress.expectObserved();
            }
        }
    }
    // Java 17 ExecutorService is closed explicitly in finally (AutoCloseable starts in Java 19).
    @SuppressWarnings("resource")
    @Test public void parallelBrowsersKeepTheirSessionsIsolated() throws Exception {
        var workers = java.util.concurrent.Executors.newFixedThreadPool(2);
        var ready = new java.util.concurrent.CyclicBarrier(2);
        var config = expert.nbt.nutrimind.config.TestConfig.load();
        try {
            var tasks = java.util.stream.IntStream.range(0, 2).mapToObj(index ->
                    workers.submit(() -> {
                        try (var session = new expert.nbt.nutrimind.support.BrowserSession(config)) {
                            session.start();
                            Page page = session.newPage();
                            String value = "worker-" + index;
                            page.context().addCookies(List.of(new com.microsoft.playwright.options.Cookie("isolation", value)
                                    .setUrl("https://example.invalid/")));
                            page.setContent("<h1>" + value + "</h1>");
                            ready.await(30, java.util.concurrent.TimeUnit.SECONDS);
                            assertEquals(page.context().cookies().get(0).value, value);
                            assertEquals(page.getByRole(com.microsoft.playwright.options.AriaRole.HEADING).innerText(), value);
                            return Thread.currentThread().getId();
                        }
                    })).toList();
            assertNotEquals(tasks.get(0).get(45, java.util.concurrent.TimeUnit.SECONDS),
                    tasks.get(1).get(45, java.util.concurrent.TimeUnit.SECONDS));
        } finally { workers.shutdownNow(); }
    }
    private static Object unusedResult(java.lang.reflect.Method method) {
        if (method.getReturnType() == void.class) return null;
        throw new UnsupportedOperationException("Unexpected test-double call: " + method.getName());
    }
    private static <T> T fake(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                return switch (method.getName()) {
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "toString" -> type.getSimpleName() + " test double";
                    default -> throw new UnsupportedOperationException(method.getName());
                };
            }
            return handler.invoke(proxy, method, args);
        }));
    }
}
