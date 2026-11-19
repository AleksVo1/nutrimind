package expert.nbt.nutrimind.tests;

import expert.nbt.nutrimind.config.*;
import expert.nbt.nutrimind.pages.LoginPage;
import expert.nbt.nutrimind.reporting.*;
import expert.nbt.nutrimind.support.*;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestResult;
import org.testng.annotations.*;

@Listeners(SecretSanitizingListener.class)
public abstract class BaseTest {
    private static final Logger log = LoggerFactory.getLogger(BaseTest.class);
    protected final TestConfig config = TestConfig.load();
    private final ThreadLocal<TestSession> current = new ThreadLocal<>();
    private static final ConcurrentHashMap<String, ReentrantLock> accountLocks = new ConcurrentHashMap<>();
    private final ArtifactCollector artifacts = new ArtifactCollector(new AllureArtifactSink());

    private static final class TestSession {
        BrowserSession browser;
        LoginPage login;
        ReentrantLock accountLock;
    }
    protected final LoginPage login() { return current.get().login; }

    @BeforeMethod(alwaysRun = true)
    public void newSession(Method method, Object[] parameters) {
        TestSession session = new TestSession();
        current.set(session);
        // Logging out must not invalidate another test using the same account.
        Arrays.stream(parameters).filter(Credentials.class::isInstance).map(Credentials.class::cast)
                .findFirst().ifPresent(user -> {
                    session.accountLock = accountLocks.computeIfAbsent(user.username(), ignored -> new ReentrantLock());
                    session.accountLock.lock();
                });
        log.info("START {}.{}", getClass().getSimpleName(), method.getName());
        session.browser = new BrowserSession(config);
        session.browser.start();
        session.login = new LoginPage(session.browser.newPage(), config, session.browser::protect);
    }
    @AfterMethod(alwaysRun = true)
    public void finishSession(ITestResult result) {
        TestSession session = current.get();
        if (session == null) return;
        String name = result.getMethod().getMethodName();
        String alias = Arrays.stream(result.getParameters()).filter(Credentials.class::isInstance)
                .map(Object::toString).findFirst().orElse("default");
        String status = result.isSuccess() ? "PASS" : result.getStatus() == ITestResult.SKIP ? "SKIP" : "FAIL";
        log.info("{} {} [{}] ({} ms)", status, name, alias, result.getEndMillis() - result.getStartMillis());
        try {
            if (session.browser != null) session.browser.finish(artifacts,
                    name + "-" + alias + "-" + java.util.UUID.randomUUID(), !result.isSuccess());
        } finally {
            try { if (session.browser != null) session.browser.close(); }
            finally {
                if (session.accountLock != null) session.accountLock.unlock();
                current.remove();
            }
        }
    }
    protected void openUkrainianLogin() {
        login().open();
        login().selectLanguage("UK");
        login().expectLoaded();
    }
}
