package expert.nbt.nutrimind.pages;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import expert.nbt.nutrimind.config.TestConfig;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public final class LoginPage extends BasePage {
    public LoginPage(Page page) { super(page); }
    public Response open() { return page.navigate(TestConfig.baseUrl()); }
    public Locator email() { return page.locator("app-login-component input[formcontrolname='user']"); }
    public Locator password() { return page.locator("app-login-component input[formcontrolname='password']"); }
    public Locator submit() { return page.locator("app-login-component button[type='submit']"); }
    public Locator errors() { return page.locator("app-login-component mat-error"); }
    public Locator logo() { return page.locator("app-home .h1"); }
    public Locator recoverLink() { return page.locator("app-recover a"); }
    public Locator registerLink() { return page.locator("app-link-registration a"); }
    public void selectLanguage(String language) {
        Locator switcher = page.locator("app-select-language button.btn-lang");
        assertThat(switcher).isVisible();
        if (!switcher.innerText().trim().equalsIgnoreCase(language)) {
            switcher.click();
            page.getByRole(AriaRole.MENUITEM, new Page.GetByRoleOptions().setName(java.util.regex.Pattern.compile("^" + language + "$", java.util.regex.Pattern.CASE_INSENSITIVE))).click();
        }
        assertThat(switcher).hasText(java.util.regex.Pattern.compile(language, java.util.regex.Pattern.CASE_INSENSITIVE));
    }
    public void fillCredentials(String username, String secret) {
        email().fill(username);
        password().fill(secret);
    }
    public void submitCredentials(String username, String secret) { fillCredentials(username, secret); submit().click(); }
    public void expectLoginForm() {
        assertThat(email()).isVisible();
        assertThat(password()).isVisible();
        assertThat(submit()).isVisible();
    }
    public GameSelectionPage login(String username, String secret) {
        submitCredentials(username, secret);
        GameSelectionPage game = new GameSelectionPage(page);
        game.expectLoaded();
        return game;
    }
    public RecoveryPage openRecovery() {
        return new RecoveryPage(page.waitForPopup(() -> recoverLink().click()));
    }
    public RegistrationPage openRegistration() {
        return new RegistrationPage(page.waitForPopup(() -> registerLink().click()));
    }
}
