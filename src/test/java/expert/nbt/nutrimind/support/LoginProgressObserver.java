package expert.nbt.nutrimind.support;

import com.microsoft.playwright.Page;

/** Arm before submit; preserve evidence even if the spinner disappears before click returns. */
public final class LoginProgressObserver implements AutoCloseable {
    private final Page page;
    public LoginProgressObserver(Page page) {
        this.page = page;
        page.evaluate("""
            () => {
              window.__loginProgressSeen = false;
              const inspect = () => {
                const visible = [...document.querySelectorAll('body *')].some(el =>
                  el.textContent.trim() === 'Вхід у систему...' &&
                  el.getClientRects().length > 0 && getComputedStyle(el).visibility !== 'hidden');
                if (visible) window.__loginProgressSeen = true;
              };
              window.__loginProgressObserver = new MutationObserver(inspect);
              window.__loginProgressObserver.observe(document.body, {
                childList: true, subtree: true, characterData: true, attributes: true
              });
              inspect();
            }
            """);
    }
    public void expectObserved() { page.waitForFunction("window.__loginProgressSeen === true"); }
    @Override public void close() {
        page.evaluate("() => { window.__loginProgressObserver?.disconnect(); delete window.__loginProgressObserver; delete window.__loginProgressSeen; }");
    }
}
