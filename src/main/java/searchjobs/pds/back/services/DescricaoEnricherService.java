package searchjobs.pds.back.services;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;
import searchjobs.pds.back.entities.Job;
import searchjobs.pds.back.repositories.JobRepository;

import java.time.Duration;
import java.util.List;

@Service
public class DescricaoEnricherService {

    private final JobRepository jobRepository;

    public DescricaoEnricherService(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    public void enriquecerDescricoes() {
        List<Job> vagas = jobRepository.findSemDescricao();

        if (vagas.isEmpty()) {
            System.out.println("✅ [DescricaoEnricher] Nenhuma vaga com descrição vazia.");
            return;
        }

        System.out.println("\n════════════════════════════════════");
        System.out.println("🔬 [DescricaoEnricher] Vagas para enriquecer: " + vagas.size());
        System.out.println("════════════════════════════════════");

        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--start-maximized");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--lang=pt-BR");
        options.setExperimentalOption("excludeSwitches", List.of("enable-automation"));
        options.setExperimentalOption("useAutomationExtension", false);
        options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");

        WebDriver driver = new ChromeDriver(options);
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

        int atualizadas = 0;
        int semDescricao = 0;

        try {
            aceitarCookiesInfoJobs(driver, js);
            aceitarCookiesEmpregos(driver);

            boolean gupyCookiesAceitos = false;

            for (int i = 0; i < vagas.size(); i++) {
                Job vaga = vagas.get(i);
                System.out.printf("📄 [%d/%d] [%s] %s%n",
                        i + 1, vagas.size(), vaga.getFonte(), vaga.getTitulo());
                try {
                    driver.get(vaga.getLinkOriginal());
                    Thread.sleep(2500);

                    // Tenta aceitar cookies Gupy em cada página até conseguir uma vez.
                    // Vagas encerradas não mostram o banner, então continua tentando
                    // nas próximas até achar uma ativa. Após aceitar, o browser guarda
                    // o consentimento e o flag evita tentativas desnecessárias.
                    if ("Gupy".equals(vaga.getFonte()) && !gupyCookiesAceitos) {
                        gupyCookiesAceitos = tentarAceitarCookiesGupy(driver);
                    }

                    String descricao = extrairDescricao(driver);
                    vaga.setDescricao(descricao);
                    jobRepository.save(vaga);
                    if (descricao.equals("Descrição não encontrada")) {
                        semDescricao++;
                        System.out.println("   ⚠️ Descrição não encontrada");
                    } else {
                        atualizadas++;
                        System.out.println("   ✅ Descrição salva (" + descricao.length() + " chars)");
                    }

                    Thread.sleep(1500 + (int) (Math.random() * 1500));

                } catch (Exception e) {
                    semDescricao++;
                    System.out.println("   ❌ Erro: " + e.getMessage());
                }
            }
        } finally {
            driver.quit();
            System.out.println("\n════════════════════════════════════");
            System.out.println("🎉 [DescricaoEnricher] Finalizado!");
            System.out.println("✅ Atualizadas: " + atualizadas);
            System.out.println("⚠️  Sem descrição: " + semDescricao);
            System.out.println("════════════════════════════════════\n");
        }
    }

    // ── Aceite de cookies por domínio ─────────────────────────────────────

    // Tenta aceitar o banner da Gupy na página atual. Retorna true se conseguiu.
    // Usa timeout curto para não travar em vagas encerradas (sem banner).
    private boolean tentarAceitarCookiesGupy(WebDriver driver) {
        try {
            WebElement btn = new WebDriverWait(driver, Duration.ofSeconds(4))
                    .until(ExpectedConditions.presenceOfElementLocated(
                            By.cssSelector("span[aria-label='Aceitar Cookies'], span.cc-dismiss")));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);
            Thread.sleep(800);
            System.out.println("   🍪 Cookies Gupy aceitos.");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void aceitarCookiesInfoJobs(WebDriver driver, JavascriptExecutor js) {
        try {
            System.out.println("🍪 Aceitando cookies InfoJobs...");
            driver.get("https://www.infojobs.com.br/");
            Thread.sleep(2000);
            js.executeScript("if (typeof Didomi !== 'undefined') Didomi.setUserAgreeToAll();");
            Thread.sleep(1000);
            System.out.println("   ✅ Cookies InfoJobs aceitos.");
        } catch (Exception e) {
            System.out.println("   ⚠️ Banner InfoJobs não apareceu — continuando.");
        }
    }

    private void aceitarCookiesEmpregos(WebDriver driver) {
        try {
            System.out.println("🍪 Aceitando cookies Empregos.com.br...");
            driver.get("https://www.empregos.com.br/");
            Thread.sleep(2000);
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(6));
            WebElement btn = shortWait.until(d -> {
                for (WebElement b : d.findElements(By.tagName("button"))) {
                    if (!b.isDisplayed()) continue;
                    String txt = b.getText().toLowerCase().trim();
                    if (txt.contains("aceitar") || txt.contains("concordar") || txt.contains("accept"))
                        return b;
                }
                return null;
            });
            btn.click();
            Thread.sleep(1000);
            System.out.println("   ✅ Cookies Empregos.com.br aceitos.");
        } catch (Exception e) {
            System.out.println("   ⚠️ Banner Empregos.com.br não apareceu — continuando.");
        }
    }

    // ── Extração da descrição ─────────────────────────────────────────────

    private String extrairDescricao(WebDriver driver) {
        // Empregos.com.br usa layout Tailwind — pega o div.break-words irmão do h3 de descrição
        String currentUrl = driver.getCurrentUrl();
        if (currentUrl != null && currentUrl.contains("empregos.com.br")) {
            try {
                WebElement div = driver.findElement(
                        By.xpath("//h3[contains(@class,'text-cinza90')]/following-sibling::div[contains(@class,'break-words')]"));
                String texto = div.getText().trim();
                if (texto.length() > 20) return texto;
            } catch (NoSuchElementException ignored) {}
        }

        String[] selectors = {
            // Gupy
            "div[data-testid='text-section']",
            // InfoJobs
            ".js_vacancyDataPanels",
            "[itemprop='description']",
            "#tab-description",
            // Genéricos
            "div[class*='description']",
            "section[class*='description']",
            "div[class*='job-description']"
        };

        for (String sel : selectors) {
            try {
                String texto = driver.findElement(By.cssSelector(sel)).getText().trim();
                if (texto.length() > 20 && !texto.equals(".")) return texto;
            } catch (NoSuchElementException ignored) {}
        }
        return "Descrição não encontrada";
    }
}
