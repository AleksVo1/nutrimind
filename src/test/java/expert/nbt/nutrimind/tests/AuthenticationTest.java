package expert.nbt.nutrimind.tests;

import com.microsoft.playwright.Response;
import com.microsoft.playwright.Page;
import expert.nbt.nutrimind.config.TestConfig;
import expert.nbt.nutrimind.pages.GameSelectionPage;
import org.testng.annotations.Test;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.testng.Assert.*;

@io.qameta.allure.Epic("NutriMind")
@io.qameta.allure.Feature("Авторизація та навігація")
public final class AuthenticationTest extends BaseTest {
    private static final String ERROR = "Ім'я користувача та пароль не знайдено. Перевірте ще раз!";

    @Test(description = "TC-01: Невірний email і валідний пароль")
    public void invalidEmail() {
        openUkrainianLogin();
        String original = page.url();
        login.submitCredentials("invalid@test.com", TestConfig.password());
        expectRejectedLogin(original);
        // Assert red emphasis without tying the test to a particular Material theme hex.
        assertTrue((Boolean) login.errors().first().evaluate("el => { const c = getComputedStyle(el).color.match(/[\\d.]+/g).map(Number); return c[0] > c[1] * 1.5 && c[0] > c[2] * 1.5; }"), "Помилка повинна бути червоною");
    }
    @Test(description = "TC-02: Валідний логін і невірний пароль")
    public void invalidPassword() {
        openUkrainianLogin();
        String original = page.url();
        login.submitCredentials(TestConfig.username(), "1");
        expectRejectedLogin(original);
    }
    private void expectRejectedLogin(String original) {
        assertThat(login.errors()).hasCount(2);
        assertThat(login.errors().first()).hasText(ERROR);
        assertThat(login.errors().last()).hasText(ERROR);
        assertThat(login.errors().first()).isVisible();
        assertThat(login.errors().last()).isVisible();
        login.expectLoginForm();
        assertThat(page).hasURL(original);
    }
    @Test(description = "TC-03: Успішна авторизація та індикатор завантаження")
    public void successfulLogin() {
        openUkrainianLogin();
        login.fillCredentials(TestConfig.username(), TestConfig.password());
        login.submit().click();
        assertThat(page.getByText("Вхід у систему...", new Page.GetByTextOptions().setExact(true))).isVisible();
        new GameSelectionPage(page).expectLoaded();
        assertThat(login.email()).not().isVisible();
    }
    @Test(description = "TC-04: Вихід із підтвердженням і закриття сесії")
    public void logout() {
        openUkrainianLogin();
        login.login(TestConfig.username(), TestConfig.password()).logout();
        assertThat(page).hasURL(TestConfig.baseUrl());
        // Reload a protected route in the same context: old session must not grant access.
        page.navigate(TestConfig.baseUrl().replaceAll("/+$", "") + "/step-one");
        login.expectLoginForm();
        assertThat(page).hasURL(TestConfig.baseUrl());
    }
    @Test(description = "TC-05: Перемикання RU → EN → UK")
    public void languageSwitching() {
        openUkrainianLogin();
        String[][] translations = {
            {"RU", "Пароль", "Войти", "Восстановить пароль", "Зарегистрироваться"},
            {"EN", "Password", "Enter", "Recover password", "Register"},
            {"UK", "Пароль", "Увійти", "Відновити пароль", "Зареєструватися"}
        };
        for (String[] language : translations) {
            login.selectLanguage(language[0]);
            assertThat(login.email()).hasAttribute("placeholder", "E-mail");
            assertThat(login.password()).hasAttribute("placeholder", language[1]);
            assertThat(login.submit()).hasText(language[2]);
            assertThat(login.recoverLink()).hasText(language[3]);
            assertThat(login.registerLink()).hasText(language[4]);
            login.expectLoginForm();
            assertThat(login.recoverLink()).isVisible();
            assertThat(login.registerLink()).isVisible();
            assertFalse(page.locator("body").innerText().matches("(?s).*(loginForm\\.|homePage\\.|buttons\\.|links\\.).*"), "Видимі ключі перекладу");
        }
    }
    @Test(description = "TC-06: HTTP 200, логотип та форма авторизації")
    public void loginPageLoaded() {
        Response response = login.open();
        assertNotNull(response, "Відсутня відповідь навігації");
        assertEquals(response.status(), 200);
        login.selectLanguage("UK");
        login.expectLoginForm();
        assertThat(login.logo()).isVisible();
        assertThat(login.logo()).containsText("Nutri");
        assertThat(login.logo()).containsText("Mind");
        assertThat(login.submit()).hasText("Увійти");
        assertThat(login.recoverLink()).isVisible();
        assertThat(login.registerLink()).isVisible();
    }
    @Test(description = "TC-07: Посилання відкриває форму скидання пароля")
    public void recoveryNavigation() {
        openUkrainianLogin();
        login.openRecovery().expectLoaded();
    }
    @Test(description = "TC-08: Перехід до форми реєстрації")
    public void registrationNavigation() {
        openUkrainianLogin();
        assertThat(page.locator("app-link-registration")).containsText("Немає аккаунта?");
        login.openRegistration().expectLoaded();
    }
}
