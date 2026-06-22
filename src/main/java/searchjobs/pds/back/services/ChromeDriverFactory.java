package searchjobs.pds.back.services;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.util.List;

public class ChromeDriverFactory {

    public static WebDriver create() {
        String chromedriverPath = System.getenv("CHROMEDRIVER_PATH");
        if (chromedriverPath != null) {
            System.setProperty("webdriver.chrome.driver", chromedriverPath);
        } else {
            WebDriverManager.chromedriver().setup();
        }

        ChromeOptions options = new ChromeOptions();

        String chromeBin = System.getenv("CHROME_BIN");
        if (chromeBin != null) {
            options.setBinary(chromeBin);
        }

        if ("true".equalsIgnoreCase(System.getenv("CHROME_HEADLESS"))) {
            options.addArguments("--headless");
        }

        options.addArguments("--window-size=1280,720");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--no-zygote");
        options.addArguments("--disable-setuid-sandbox");
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-plugins");
        options.addArguments("--blink-settings=imagesEnabled=false");
        options.addArguments("--lang=pt-BR");
        options.addArguments("--disable-notifications");
        options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");
        options.setExperimentalOption("excludeSwitches", List.of("enable-automation"));
        options.setExperimentalOption("useAutomationExtension", false);

        WebDriver driver = new ChromeDriver(options);
        ((JavascriptExecutor) driver)
                .executeScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})");

        return driver;
    }
}
