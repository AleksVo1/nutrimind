package expert.nbt.nutrimind.pages;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import expert.nbt.nutrimind.config.TestConfig;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

// Pages are borrowed from BrowserSession; closing the context releases all tabs.
@SuppressWarnings("resource")
public final class GameSelectionPage extends BasePage {
    private final Runnable privacy;
    public GameSelectionPage(Page page, TestConfig routes, Runnable privacy) {
        super(page, routes); this.privacy = privacy;
    }
    @Override public void expectLoaded() {
        assertThat(page()).hasURL(urlWithOptionalQuery(routes().game("step-one")));
        assertThat(page().getByText("Оберіть тип гри", new Page.GetByTextOptions().setExact(true))).isVisible();
    }
    public LoginPage logout() {
        // The icon-only menu has no useful accessible name. Scope a role by its visible icon text.
        page().getByRole(AriaRole.BUTTON).filter(new Locator.FilterOptions()
                .setHas(page().getByText("menu", new Page.GetByTextOptions().setExact(true)))).click();
        page().getByRole(AriaRole.MENUITEM,
                new Page.GetByRoleOptions().setName("Вийти із системи").setExact(true)).click();
        Locator dialog = page().getByRole(AriaRole.DIALOG);
        assertThat(dialog).containsText("Ви впевнені що хочете вийти з системи?");
        dialog.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Так").setExact(true)).click();
        return loaded(new LoginPage(page(), routes(), privacy));
    }
}
