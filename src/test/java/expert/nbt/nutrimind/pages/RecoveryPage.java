package expert.nbt.nutrimind.pages;

import com.microsoft.playwright.Page;
import java.util.regex.Pattern;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public final class RecoveryPage extends BasePage {
    public RecoveryPage(Page page) { super(page); }
    public void expectLoaded() {
        // A login form is not a password reset form: keep TC-07 strict.
        assertThat(page).hasURL(Pattern.compile("https://my\\.nutrimind\\.game\\.nbt\\.expert/(uk|ru|en)/request-password-reset(?:\\?.*)?$"));
        assertThat(page.locator("form input[name='PasswordResetRequestForm[email]']")).isVisible();
        assertThat(page.locator("form button[type='submit']")).isVisible();
    }
}
