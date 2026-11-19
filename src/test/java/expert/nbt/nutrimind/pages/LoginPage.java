package expert.nbt.nutrimind.pages;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import expert.nbt.nutrimind.config.TestConfig;
import expert.nbt.nutrimind.config.Credentials;
import expert.nbt.nutrimind.support.*;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.testng.Assert.assertEquals;

// Pages are borrowed from BrowserSession; closing the context releases all tabs.
@SuppressWarnings("resource")
public final class LoginPage extends BasePage {
    private static final Logger log = LoggerFactory.getLogger(LoginPage.class);
    private static final String ERROR = "Ім'я користувача та пароль не знайдено. Перевірте ще раз!";
    private final Runnable privacy;
    private String language = "UK";
    private String text(String uk, String ru, String en) {
        return switch (language) { case "RU" -> ru; case "EN" -> en; default -> uk; };
    }
    private String passwordText() { return text("Пароль", "Пароль", "Password"); }
    private String submitText() { return text("Увійти", "Войти", "Enter"); }
    private String recoverText() { return text("Відновити пароль", "Восстановить пароль", "Recover password"); }
    private String registerText() { return text("Зареєструватися", "Зарегистрироваться", "Register"); }

    public LoginPage(Page page, TestConfig routes, Runnable privacy) {
        super(page, routes); this.privacy = privacy;
    }
    private Locator email() { return page().getByLabel("E-mail", new Page.GetByLabelOptions().setExact(true)); }
    private Locator password() { return page().getByLabel(passwordText(), new Page.GetByLabelOptions().setExact(true)); }
    private Locator submit() { return button(submitText()); }
    private Locator recoverLink() { return link(recoverText()); }
    private Locator registerLink() { return link(registerText()); }
    public Response open() { log.info("Opening login page"); return page().navigate(routes().game("")); }
    @Override public void expectLoaded() {
        assertThat(email()).isVisible(); assertThat(password()).isVisible(); assertThat(submit()).isVisible();
    }
    public void selectLanguage(String language) {
        if (!java.util.Set.of("UK", "RU", "EN").contains(language)) throw new IllegalArgumentException("Unsupported language code");
        log.info("Selecting interface language: {}", language);
        Locator switcher = page().getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName(Pattern.compile("^(UK|RU|EN)$", Pattern.CASE_INSENSITIVE)));
        assertThat(switcher).isVisible();
        if (!switcher.innerText().trim().equalsIgnoreCase(language)) {
            switcher.click();
            page().getByRole(AriaRole.MENUITEM, new Page.GetByRoleOptions()
                    .setName(Pattern.compile("^" + language + "$", Pattern.CASE_INSENSITIVE))).click();
        }
        this.language = language;
        assertThat(switcher).hasText(Pattern.compile(language, Pattern.CASE_INSENSITIVE));
        expectLoaded();
    }
    private void submitCredentials(Credentials credentials) {
        privacy.run();
        email().fill(credentials.username()); password().fill(credentials.password()); submit().click();
    }
    public void expectRejectedLogin(Credentials credentials, boolean checkColor) {
        String original = url();
        submitCredentials(credentials);
        Locator errors = page().getByText(ERROR, new Page.GetByTextOptions().setExact(true))
                .filter(new Locator.FilterOptions().setVisible(true));
        assertThat(errors).hasCount(2);
        assertThat(errors.first()).isVisible(); assertThat(errors.last()).isVisible();
        if (checkColor) {
            assertEquals(errors.first().evaluate("el => { const c = getComputedStyle(el).color.match(/[\\d.]+/g).map(Number); return c[0] > c[1] * 1.5 && c[0] > c[2] * 1.5; }"), Boolean.TRUE, "Login error must be red");
        }
        expectLoaded(); assertThat(page()).hasURL(original);
    }
    public GameSelectionPage login(Credentials credentials) {
        submitCredentials(credentials);
        return loaded(new GameSelectionPage(page(), routes(), privacy));
    }
    public void loginExpectingProgress(Credentials credentials) {
        try (var progress = new LoginProgressObserver(page())) {
            submitCredentials(credentials);
            progress.expectObserved();
            loaded(new GameSelectionPage(page(), routes(), privacy));
            assertThat(email()).not().isVisible();
        }
    }
    public void expectTranslations() {
        expectLoaded();
        assertThat(email()).hasAttribute("placeholder", "E-mail");
        assertThat(password()).hasAttribute("placeholder", passwordText());
        assertThat(submit()).hasText(submitText());
        assertThat(recoverLink()).isVisible(); assertThat(registerLink()).isVisible();
        assertThat(recoverLink()).hasText(recoverText()); assertThat(registerLink()).hasText(registerText());
        // Body is the document-level text fallback; no component structure is referenced.
        assertThat(page().locator("body")).not().containsText(Pattern.compile("loginForm\\.|homePage\\.|buttons\\.|links\\."));
    }
    public void expectBrandAndLinks() {
        expectLoaded();
        assertThat(page().getByText(Pattern.compile("^Nutri\\s*Mind$"))).isVisible();
        assertThat(recoverLink()).isVisible(); assertThat(registerLink()).isVisible();
    }
    public void expectRegistrationPrompt() {
        assertThat(page().getByText("Немає аккаунта?", new Page.GetByTextOptions().setExact(false))).isVisible();
        assertThat(registerLink()).hasText(registerText());
    }
    public void openRecovery() {
        assertThat(recoverLink()).hasText(recoverText());
        log.info("Opening recovery entry page");
        loaded(new RecoveryPage(page().waitForPopup(() -> recoverLink().click()), routes()));
    }
    public RegistrationPage openRegistration() {
        log.info("Opening registration page");
        return loaded(new RegistrationPage(page().waitForPopup(() -> registerLink().click()), routes()));
    }
    public void expectSignedOut() { assertThat(page()).hasURL(routes().game("")); expectLoaded(); }
    public void openProtectedRoute() { page().navigate(routes().game("step-one")); expectSignedOut(); }
}
