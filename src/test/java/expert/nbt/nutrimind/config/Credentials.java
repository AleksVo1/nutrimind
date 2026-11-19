package expert.nbt.nutrimind.config;

/** toString is safe for TestNG/Allure data-provider parameters. */
public final class Credentials {
    private final String alias;
    private final String username;
    private final String password;

    public Credentials(String alias, String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new IllegalArgumentException("Both credential fields must be configured");
        }
        this.alias = alias;
        this.username = username;
        this.password = password;
    }
    public String username() { return username; }
    public String password() { return password; }
    @Override public String toString() { return alias; }
}
