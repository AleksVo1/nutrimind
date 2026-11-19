package expert.nbt.nutrimind.support;

import expert.nbt.nutrimind.config.TestConfig;
import org.testng.*;

/** Invoked before TestNG/Allure test-result listeners serialize failures. */
public final class SecretSanitizingListener implements IInvokedMethodListener {
    @Override public void afterInvocation(IInvokedMethod method, ITestResult result) {
        if (result.getThrowable() != null) {
            try { result.setThrowable(new SecretRedactor(TestConfig.load().users()).sanitize(result.getThrowable())); }
            catch (RuntimeException unavailable) {
                result.setThrowable(new IllegalStateException("Failure details hidden: credential filter unavailable"));
            }
        }
    }
}
