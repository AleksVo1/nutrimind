package expert.nbt.nutrimind.pages;

import com.microsoft.playwright.Page;
import java.util.regex.Pattern;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public final class RegistrationPage extends BasePage {
    public RegistrationPage(Page page) { super(page); }
    public void expectLoaded() {
        assertThat(page).hasURL(Pattern.compile("https://my\\.nutrimind\\.game\\.nbt\\.expert/uk/signup(?:\\?.*)?$"));
        assertThat(page.locator("#form-signup #signupform-email")).isVisible();
        assertThat(page.locator("#form-signup #signupform-password")).isVisible();
        assertThat(page.locator("#form-signup button[type='submit']")).isVisible();
    }
}
