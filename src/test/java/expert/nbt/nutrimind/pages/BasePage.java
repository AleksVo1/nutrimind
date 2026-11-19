package expert.nbt.nutrimind.pages;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import expert.nbt.nutrimind.config.TestConfig;
import java.util.regex.Pattern;

public abstract class BasePage {
    private final Page browserPage;
    private final TestConfig routes;
    protected BasePage(Page page, TestConfig routes) { browserPage = page; this.routes = routes; }
    protected final Page page() { return browserPage; }
    protected final TestConfig routes() { return routes; }
    protected final Locator button(String name) {
        return browserPage.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(name).setExact(true));
    }
    protected final Locator link(String name) {
        return browserPage.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName(name).setExact(true));
    }
    protected final <T extends BasePage> T loaded(T destination) {
        destination.expectLoaded();
        return destination;
    }
    protected final Pattern urlWithOptionalQuery(String url) {
        // Playwright sends patterns to JavaScript, which does not support Java's \Q...\E quoting.
        StringBuilder escaped = new StringBuilder("^");
        for (char character : url.toCharArray()) {
            if ("\\^$.*+?()[]{}|".indexOf(character) >= 0) escaped.append('\\');
            escaped.append(character);
        }
        return Pattern.compile(escaped + "(?:\\?.*)?$");
    }
    public final String url() { return browserPage.url(); }
    public abstract void expectLoaded();
}
