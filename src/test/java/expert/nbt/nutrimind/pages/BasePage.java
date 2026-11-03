package expert.nbt.nutrimind.pages;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

public abstract class BasePage {
    protected final Page page;
    protected BasePage(Page page) { this.page = page; }
    protected Locator button(String name) {
        return page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(name).setExact(true));
    }
    public String url() { return page.url(); }
}
