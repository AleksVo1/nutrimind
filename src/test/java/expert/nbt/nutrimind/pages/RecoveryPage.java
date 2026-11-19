package expert.nbt.nutrimind.pages;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.*;
import expert.nbt.nutrimind.config.TestConfig;
import java.util.regex.Pattern;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** Confirmed dev behavior: recovery starts on the cabinet login page. */
// Pages are borrowed from BrowserSession; closing the context releases all tabs.
@SuppressWarnings("resource")
public final class RecoveryPage extends BasePage {
    public RecoveryPage(Page page, TestConfig routes) { super(page, routes); }
    @Override public void expectLoaded() {
        assertThat(page()).hasURL(Pattern.compile("^https?://[^/?#]+/ru/login(?:\\?.*)?$"));
        page().waitForLoadState(LoadState.LOAD);
        assertThat(page().getByRole(AriaRole.HEADING,
                new Page.GetByRoleOptions().setName("Авторизация").setExact(true))).isVisible();
        // The cabinet fields have placeholders, but no associated labels.
        assertThat(page().getByPlaceholder("E-mail", new Page.GetByPlaceholderOptions().setExact(true))).isVisible();
        assertThat(page().getByPlaceholder("Пароль", new Page.GetByPlaceholderOptions().setExact(true))).isVisible();
        assertThat(page().getByPlaceholder("Пароль", new Page.GetByPlaceholderOptions().setExact(true))).hasAttribute("type", "password");
        assertThat(button("Авторизоваться")).isVisible();
        assertThat(link("сбросить его.")).isVisible();
        assertThat(link("сбросить его.")).hasAttribute("href", "/ru/request-password-reset");
    }
}
