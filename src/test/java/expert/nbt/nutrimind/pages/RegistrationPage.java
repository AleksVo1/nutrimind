package expert.nbt.nutrimind.pages;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.*;
import expert.nbt.nutrimind.config.TestConfig;
import java.util.regex.Pattern;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

// Pages are borrowed from BrowserSession; closing the context releases all tabs.
@SuppressWarnings("resource")
public final class RegistrationPage extends BasePage {
    public RegistrationPage(Page page, TestConfig routes) { super(page, routes); }
    private Locator email() { return page().getByPlaceholder("E-mail", new Page.GetByPlaceholderOptions().setExact(true)); }
    private Locator password() { return page().getByPlaceholder("Пароль", new Page.GetByPlaceholderOptions().setExact(true)); }
    @Override public void expectLoaded() {
        assertThat(page()).hasURL(Pattern.compile("^https?://[^/?#]+/uk/signup(?:\\?.*)?$"));
        assertThat(email()).isVisible(); assertThat(password()).isVisible();
        assertThat(button("Реєстрація")).isVisible();
    }
    public void expectDetailedForm() {
        expectLoaded();
        page().waitForLoadState(LoadState.LOAD);
        assertThat(password()).hasAttribute("type", "password");
        assertThat(button("Реєстрація")).isEnabled();
        assertThat(page().getByRole(AriaRole.HEADING,
                new Page.GetByRoleOptions().setName("Реєстрація").setExact(true))).isVisible();
        // This navigation has no accessible name; scope the semantic role by its home link.
        Locator breadcrumbs = page().getByRole(AriaRole.NAVIGATION).filter(new Locator.FilterOptions()
                .setHas(page().getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Головна").setExact(true))));
        assertThat(breadcrumbs).isVisible();
        assertThat(breadcrumbs.getByRole(AriaRole.LISTITEM)).hasText(new String[]{"Головна", "Реєстрація"});
        assertThat(breadcrumbs.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Головна").setExact(true)))
                .hasAttribute("href", "/uk/");
    }
}
