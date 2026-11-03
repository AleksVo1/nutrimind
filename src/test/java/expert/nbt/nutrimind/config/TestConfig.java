package expert.nbt.nutrimind.config;

public final class TestConfig {
    private TestConfig() {}
    public static String value(String property, String env, String fallback) {
        String value = System.getProperty(property);
        if (value == null || value.isBlank()) value = System.getenv(env);
        return value == null || value.isBlank() ? fallback : value;
    }
    public static String baseUrl() { return value("baseUrl", "BASE_URL", "https://play.nutrimind.game.nbt.expert/"); }
    public static String username() { return value("username", "TEST_USERNAME", "demo"); }
    public static String password() { return value("password", "TEST_PASSWORD", "demo"); }
    public static boolean headless() { return Boolean.parseBoolean(value("headless", "HEADLESS", "false")); }
    public static double timeout() { return Double.parseDouble(value("timeout", "TEST_TIMEOUT", "15000")); }
}
