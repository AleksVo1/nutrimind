package expert.nbt.nutrimind.support;

import expert.nbt.nutrimind.config.Credentials;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class SecretRedactor {
    private final List<String> secrets;
    public SecretRedactor(List<Credentials> users) {
        var values = new HashSet<String>();
        for (Credentials user : users) {
            for (String value : List.of(user.username(), user.password())) {
                values.add(value);
                values.add(URLEncoder.encode(value, StandardCharsets.UTF_8));
            }
        }
        secrets = values.stream().filter(v -> !v.isEmpty()).sorted(Comparator.comparingInt(String::length).reversed()).toList();
    }
    public String redact(String text) {
        if (text == null) return null;
        for (String secret : secrets) text = text.replace(secret, "[REDACTED]");
        return text;
    }
    public Throwable sanitize(Throwable original) {
        if (original == null) return null;
        // Never retain the original cause/suppressed exceptions, which may contain locator values.
        String message = redact(original.getClass().getSimpleName() + ": " + original.getMessage());
        Throwable safe = original instanceof AssertionError ? new AssertionError(message)
                : original instanceof org.testng.SkipException ? new org.testng.SkipException(message)
                : new RuntimeException(message);
        safe.setStackTrace(original.getStackTrace());
        return safe;
    }
}
