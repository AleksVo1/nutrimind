package expert.nbt.nutrimind.pages;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.LoadState;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** Dev recovery starts on the cabinet login page, as confirmed by the user. */
public final class RecoveryPage extends BasePage {
    public RecoveryPage(Page page) { super(page); }
    public void expectLoaded() {
        assertThat(page).hasURL("https://my.nutrimind.game.nbt.expert/ru/login");
        page.waitForLoadState(LoadState.LOAD);
        assertThat(page.getByRole(AriaRole.HEADING,
                new Page.GetByRoleOptions().setName("Авторизация").setExact(true))).isVisible();
        assertThat(page.locator("#login-form #loginform-email")).isVisible();
        assertThat(page.locator("#login-form #loginform-password")).isVisible();
        assertThat(page.locator("#login-form #loginform-password")).hasAttribute("type", "password");
        assertThat(button("Авторизоваться")).isVisible();
        var resetLink = page.getByRole(AriaRole.LINK,
                new Page.GetByRoleOptions().setName("сбросить его.").setExact(true));
        assertThat(resetLink).isVisible();
        assertThat(resetLink).hasAttribute("href", "/ru/request-password-reset");
    }
}
