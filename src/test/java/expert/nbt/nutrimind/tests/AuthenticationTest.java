package expert.nbt.nutrimind.tests;

import expert.nbt.nutrimind.config.Credentials;
import org.testng.annotations.*;
import static org.testng.Assert.*;

@io.qameta.allure.Epic("NutriMind")
@io.qameta.allure.Feature("Авторизація та навігація")
public final class AuthenticationTest extends BaseTest {
    @DataProvider(name = "primaryUser")
    public Object[][] primaryUser() {
        return new Object[][]{{config.primary()}};
    }
    @Test(description = "TC-01: Невірний email і валідний пароль")
    public void invalidEmail() {
        openUkrainianLogin();
        login().expectRejectedLogin(new Credentials("invalid-email", "invalid@test.com", config.primary().password()), true);
    }
    @Test(description = "TC-02: Валідний логін і невірний пароль")
    public void invalidPassword() {
        openUkrainianLogin();
        login().expectRejectedLogin(new Credentials("invalid-password", config.primary().username(), "1"), false);
    }
    @Test(dataProvider = "primaryUser", description = "TC-03: Успішна авторизація та індикатор завантаження")
    public void successfulLogin(Credentials user) {
        openUkrainianLogin();
        login().loginExpectingProgress(user);
    }
    @Test(dataProvider = "primaryUser", description = "TC-04: Вихід із підтвердженням і закриття сесії")
    public void logout(Credentials user) {
        openUkrainianLogin();
        var signedOut = login().login(user).logout();
        signedOut.expectSignedOut();
        signedOut.openProtectedRoute();
    }
    @Test(description = "TC-05: Перемикання RU → EN → UK")
    public void languageSwitching() {
        openUkrainianLogin();
        for (String language : new String[]{"RU", "EN", "UK"}) {
            login().selectLanguage(language);
            login().expectTranslations();
        }
    }
    @Test(description = "TC-06: HTTP 200, логотип та форма авторизації")
    public void loginPageLoaded() {
        var response = login().open();
        assertNotNull(response, "Navigation response is missing");
        assertEquals(response.status(), 200);
        login().selectLanguage("UK");
        login().expectBrandAndLinks();
    }
    @Test(description = "TC-07: Відновлення пароля відкриває авторизацію кабінету (dev)")
    public void recoveryNavigation() {
        openUkrainianLogin();
        login().openRecovery();
        login().expectLoaded();
    }
    @Test(description = "TC-08: Перехід до форми реєстрації")
    public void registrationNavigation() {
        openUkrainianLogin();
        login().expectRegistrationPrompt();
        login().openRegistration();
    }
    @Test(description = "TC-09: Відкриття реєстрації — поля, кнопка та навігаційний ланцюжок")
    @io.qameta.allure.Story("Відкриття сторінки Реєстрація")
    public void registrationPageDetails() {
        openUkrainianLogin();
        login().openRegistration().expectDetailedForm();
    }
}
