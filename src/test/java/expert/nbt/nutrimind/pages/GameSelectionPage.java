package expert.nbt.nutrimind.pages;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.Locator;
import java.util.regex.Pattern;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public final class GameSelectionPage extends BasePage {
    public GameSelectionPage(Page page) { super(page); }
    public void expectLoaded() {
        assertThat(page).hasURL(Pattern.compile(".*/step-one(?:\\?.*)?$"));
        assertThat(page.getByText("Оберіть тип гри", new Page.GetByTextOptions().setExact(true))).isVisible();
    }
    public LoginPage logout() {
        page.locator("app-user-profile button.user-menu").click();
        page.getByText("Вийти із системи", new Page.GetByTextOptions().setExact(true)).click();
        Locator dialog = page.getByRole(com.microsoft.playwright.options.AriaRole.DIALOG);
        assertThat(dialog).containsText("Ви впевнені що хочете вийти з системи?");
        dialog.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON,
                new com.microsoft.playwright.Locator.GetByRoleOptions().setName("Так").setExact(true)).click();
        LoginPage login = new LoginPage(page);
        login.expectLoginForm();
        return login;
    }
}
