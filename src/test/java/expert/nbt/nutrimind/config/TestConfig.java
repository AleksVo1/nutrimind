package expert.nbt.nutrimind.config;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.*;

public final class TestConfig {
    private final URI game;
    private final boolean headless;
    private final double timeout;
    private final List<Credentials> users;

    private TestConfig(Properties local) {
        game = base(value(local, "baseUrl", "BASE_URL", "https://play.nutrimind.game.nbt.expert/"));
        headless = Boolean.parseBoolean(value(local, "headless", "HEADLESS", "false"));
        timeout = Double.parseDouble(value(local, "timeout", "TEST_TIMEOUT", "15000"));
        if (!Double.isFinite(timeout) || timeout <= 0) throw new IllegalArgumentException("TEST_TIMEOUT must be positive");
        var configured = new ArrayList<Credentials>();
        configured.add(new Credentials("primary", value(local, "username", "TEST_EMAIL", ""),
                value(local, "password", "TEST_PASSWORD", "")));
        String email2 = value(local, "user2.email", "TEST_EMAIL2", "");
        String password2 = value(local, "user2.password", "TEST_PASSWORD2", "");
        if (!email2.isBlank() || !password2.isBlank()) {
            configured.add(new Credentials("secondary", email2, password2));
        }
        users = List.copyOf(configured);
    }
    public static TestConfig load() {
        Properties local = new Properties();
        Path file = Path.of(".secrets", "users.properties");
        if (Files.isRegularFile(file)) {
            try (var input = Files.newInputStream(file)) { local.load(input); }
            catch (IOException e) { throw new IllegalStateException("Unable to load local credentials"); }
        }
        return new TestConfig(local);
    }
    private static String value(Properties local, String property, String env, String fallback) {
        String value = System.getProperty(property);
        if (value == null || value.isBlank()) value = System.getenv(env);
        if (value == null || value.isBlank()) value = local.getProperty(env);
        return value == null || value.isBlank() ? fallback : value;
    }
    private static URI base(String value) {
        URI uri = URI.create(value.endsWith("/") ? value : value + "/");
        if (!Set.of("https", "http").contains(uri.getScheme()) || uri.getHost() == null) {
            throw new IllegalArgumentException("Application URL must be HTTP(S)");
        }
        return uri;
    }
    public String game(String path) { return game.resolve(path).toString(); }
    public boolean headless() { return headless; }
    public double timeout() { return timeout; }
    public Credentials primary() { return users.get(0); }
    public List<Credentials> users() { return users; }
}
