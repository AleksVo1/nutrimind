package expert.nbt.nutrimind.pages;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.AriaRole;
import java.util.regex.Pattern;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public final class RegistrationPage extends BasePage {
    public RegistrationPage(Page page) { super(page); }
    public void expectDetailedForm() {
        page.waitForLoadState(LoadState.LOAD);
        assertThat(page.getByPlaceholder("E-mail", new Page.GetByPlaceholderOptions().setExact(true))).isVisible();
        assertThat(page.getByPlaceholder("Пароль", new Page.GetByPlaceholderOptions().setExact(true))).isVisible();
        assertThat(page.getByPlaceholder("Пароль", new Page.GetByPlaceholderOptions().setExact(true)))
                .hasAttribute("type", "password");
        assertThat(button("Реєстрація")).isVisible();
        assertThat(button("Реєстрація")).isEnabled();
        assertThat(page.getByRole(AriaRole.HEADING,
                new Page.GetByRoleOptions().setName("Реєстрація").setExact(true))).isVisible();
        var breadcrumbs = page.locator("nav.breadcrumbs");
        assertThat(breadcrumbs).isVisible();
        assertThat(breadcrumbs.locator("ol > li")).hasText(new String[]{"Головна", "Реєстрація"});
        assertThat(breadcrumbs.getByRole(AriaRole.LINK,
                new com.microsoft.playwright.Locator.GetByRoleOptions().setName("Головна").setExact(true)))
                .hasAttribute("href", "/uk/");
    }
    public void expectLoaded() {
        assertThat(page).hasURL(Pattern.compile("https://my\\.nutrimind\\.game\\.nbt\\.expert/uk/signup(?:\\?.*)?$"));
        assertThat(page.locator("#form-signup #signupform-email")).isVisible();
        assertThat(page.locator("#form-signup #signupform-password")).isVisible();
        assertThat(page.locator("#form-signup button[type='submit']")).isVisible();
    }
}
