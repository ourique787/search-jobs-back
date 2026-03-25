package searchjobs.pds.back.services;

import io.github.bonigarcia.wdm.WebDriverManager;
import jakarta.annotation.PostConstruct;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;
import searchjobs.pds.back.entities.Job;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ScraperService {

    private final JobService jobService;

    public ScraperService(JobService jobService) {
        this.jobService = jobService;
    }

    @PostConstruct
    public void iniciarScraping() {
        System.out.println("🚀 Iniciando scraping da Gupy...");

        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--start-maximized");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.setExperimentalOption("excludeSwitches", List.of("enable-automation"));
        options.setExperimentalOption("useAutomationExtension", false);
        options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");

        WebDriver driver = new ChromeDriver(options);
        JavascriptExecutor js = (JavascriptExecutor) driver;

        ((JavascriptExecutor) driver).executeScript(
                "Object.defineProperty(navigator, 'webdriver', {get: () => undefined})"
        );

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

        try {
            driver.get("https://portal.gupy.io/job-search/term=java");
            System.out.println("🌍 Página carregada.");

            // --- COOKIES ---
            try {
                WebElement btnCookies = wait.until(
                        ExpectedConditions.elementToBeClickable(By.id("onetrust-accept-btn-handler"))
                );
                btnCookies.click();
                System.out.println("🍪 Cookies aceitos.");
                Thread.sleep(1500);
            } catch (TimeoutException e) {
                System.out.println("⚠️ Banner de cookies não apareceu, continuando...");
            }

            // --- ESPERA A LISTA CARREGAR ---
            wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("ul[class*='sc-414a0afd'] li")
            ));

            Set<String> urlsJaSalvas = new HashSet<>();
            int totalSalvo = 0;
            int pagina = 1;

            while (true) {
                System.out.println("\n📄 Coletando página " + pagina + "...");

                // Espera os cards estarem visíveis
                wait.until(ExpectedConditions.presenceOfElementLocated(
                        By.cssSelector("ul[class*='sc-414a0afd'] li")
                ));
                Thread.sleep(1000);

                // --- COLETA OS CARDS DA PÁGINA ATUAL ---
                List<WebElement> cards = driver.findElements(
                        By.cssSelector("ul[class*='sc-414a0afd'] li")
                );
                System.out.println("🔎 Cards nesta página: " + cards.size());

                for (WebElement card : cards) {
                    try {
                        WebElement linkEl = card.findElement(
                                By.cssSelector("a[class*='sc-4d881605']")
                        );
                        String url = linkEl.getAttribute("href");

                        if (url == null || urlsJaSalvas.contains(url)) continue;
                        urlsJaSalvas.add(url);

                        String titulo = "";
                        try {
                            titulo = card.findElement(
                                    By.cssSelector("h3[class*='sc-bZkfAO']")
                            ).getText().trim();
                        } catch (NoSuchElementException ignored) {}

                        if (titulo.isBlank()) continue;

                        String empresa = "Não informada";
                        try {
                            WebElement empresaEl = card.findElement(
                                    By.cssSelector("div[aria-label*='Empresa']")
                            );
                            empresa = empresaEl.getAttribute("aria-label")
                                    .replace("Empresa ", "").trim();
                        } catch (NoSuchElementException ignored) {}

                        Job novaVaga = new Job();
                        novaVaga.setTitulo(titulo);
                        novaVaga.setEmpresa(empresa);
                        novaVaga.setFonte("Gupy");
                        novaVaga.setDataColeta(LocalDateTime.now());
                        novaVaga.setLinkOriginal(url);

                        jobService.salvarVaga(novaVaga);
                        totalSalvo++;
                        System.out.printf("💾 %s | %s%n", titulo, empresa);

                    } catch (Exception e) {
                        System.out.println("⚠️ Erro num card: " + e.getMessage());
                    }
                }

                // --- TENTA IR PARA A PRÓXIMA PÁGINA ---
                try {
                    // Botão "próxima página" do MuiPagination — aria-label="Go to next page"
                    WebElement btnProxima = driver.findElement(
                            By.cssSelector("nav[aria-label*='paginação'] button[aria-label*='next'], nav[aria-label*='paginação'] button[aria-label*='próxima'], nav[aria-label*='paginação'] li:last-child button")
                    );

                    // Se o botão estiver desabilitado, chegamos na última página
                    if (!btnProxima.isEnabled() || btnProxima.getAttribute("disabled") != null) {
                        System.out.println("✅ Última página atingida.");
                        break;
                    }

                    // Scroll até o botão e clica
                    js.executeScript("arguments[0].scrollIntoView({block: 'center'});", btnProxima);
                    Thread.sleep(500);
                    btnProxima.click();
                    pagina++;

                    // Espera a página recarregar os cards
                    Thread.sleep(2000);

                } catch (NoSuchElementException e) {
                    System.out.println("✅ Paginação não encontrada, encerrando.");
                    break;
                }
            }

            System.out.println("\n🎉 Total salvo: " + totalSalvo + " vagas.");

        } catch (Exception e) {
            System.err.println("❌ Erro geral: " + e.getMessage());
            e.printStackTrace();
        } finally {
            driver.quit();
            System.out.println("🏁 Navegador fechado.");
        }
    }
}