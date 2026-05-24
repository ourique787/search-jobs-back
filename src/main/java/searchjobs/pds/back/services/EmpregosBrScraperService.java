package searchjobs.pds.back.services;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import searchjobs.pds.back.entities.Job;
import searchjobs.pds.back.entities.Stack;
import searchjobs.pds.back.repositories.JobRepository;
import searchjobs.pds.back.repositories.StackRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
@Order(4)
public class EmpregosBrScraperService implements CommandLineRunner {

    private final JobService jobService;
    private final StackRepository stackRepository;
    private final JobRepository jobRepository;
    private final Random random = new Random();

    private static final int MAX_PAGINAS_CARGA_INICIAL = 5;
    private static final int MAX_PAGINAS_ATUALIZACAO   = 1;
    private static final int TAMANHO_LOTE              = 5;

    private static final String BASE_URL  = "https://www.empregos.com.br/vagas/";
    private static final String BASE_HOST = "https://www.empregos.com.br";

    public EmpregosBrScraperService(JobService jobService,
                                    StackRepository stackRepository,
                                    JobRepository jobRepository) {
        this.jobService = jobService;
        this.stackRepository = stackRepository;
        this.jobRepository = jobRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        iniciarScraping();
    }

    public void iniciarScraping() {
        List<Stack> stacks = stackRepository.findAll();

        if (stacks.isEmpty()) {
            System.out.println("⚠️ [Empregos.com.br] Nenhuma stack no banco.");
            return;
        }

        boolean cargaInicial = jobRepository.count() == 0;

        System.out.println(cargaInicial
                ? "🚀 [Empregos.com.br] CARGA INICIAL (5 páginas por stack)"
                : "🔄 [Empregos.com.br] ATUALIZAÇÃO (1 página por stack)");
        System.out.println("📦 [Empregos.com.br] Stacks: " + stacks.size());

        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--start-maximized");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--lang=pt-BR");
        options.addArguments("--disable-notifications");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.setExperimentalOption("excludeSwitches", List.of("enable-automation"));
        options.setExperimentalOption("useAutomationExtension", false);
        options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");

        WebDriver driver = new ChromeDriver(options);
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

        int totalSalvo     = 0;
        int totalAssociado = 0;
        int stacksProcessadas = 0;

        try {
            driver.get(BASE_URL + "java");
            System.out.println("🌍 [Empregos.com.br] Site carregado.");
            pausaAleatoria(2000, 3500);
            fecharBannerCookies(driver, js);

            for (Stack stack : stacks) {
                String nomeStack  = stack.getNome();
                String slug       = toSlug(nomeStack);
                int limitePaginas = cargaInicial ? MAX_PAGINAS_CARGA_INICIAL : MAX_PAGINAS_ATUALIZACAO;

                System.out.println("\n════════════════════════════════════");
                System.out.println("🔍 [Empregos.com.br] Stack " + (stacksProcessadas + 1)
                        + "/" + stacks.size() + ": " + nomeStack);
                System.out.println("════════════════════════════════════");

                for (int pagina = 1; pagina <= limitePaginas; pagina++) {
                    String url = pagina == 1
                            ? BASE_URL + slug
                            : BASE_URL + slug + "?pagina=" + pagina;

                    System.out.println("📄 [Empregos.com.br] Página " + pagina
                            + "/" + limitePaginas + " → " + url);

                    driver.get(url);
                    pausaAleatoria(2000, 3500);
                    fecharBannerCookies(driver, js);

                    boolean temVagas = esperarVagas(driver, wait);
                    if (!temVagas) {
                        System.out.println("⚠️ [Empregos.com.br] Sem vagas na página "
                                + pagina + " para: " + nomeStack);
                        diagnosticar(driver);
                        break;
                    }

                    List<String[]> vagas = extrairVagasDaPagina(driver);
                    System.out.println("🔎 Vagas extraídas: " + vagas.size());
                    if (vagas.isEmpty()) {
                        diagnosticar(driver);
                        break;
                    }

                    for (String[] vaga : vagas) {
                        String link    = vaga[0];
                        String titulo  = vaga[1];
                        String empresa = vaga[2];
                        try {
                            Optional<Job> existente = jobService.buscarPorLink(link);
                            if (existente.isPresent()) {
                                jobService.associarStack(existente.get(), stack);
                                totalAssociado++;
                                System.out.println("🔗 [" + nomeStack + "] → " + titulo);
                            } else {
                                Job novaVaga = new Job();
                                novaVaga.setTitulo(titulo);
                                novaVaga.setEmpresa(empresa);
                                novaVaga.setFonte("Empregos.com.br");
                                novaVaga.setDataColeta(LocalDateTime.now());
                                novaVaga.setLinkOriginal(link);
                                novaVaga.getStacksRequisitadas().add(stack);
                                jobService.salvarVaga(novaVaga);
                                totalSalvo++;
                                System.out.println("💾 " + titulo + " | " + empresa);
                            }
                        } catch (Exception e) {
                            System.out.println("⚠️ [Empregos.com.br] Erro ao salvar: "
                                    + e.getMessage());
                        }
                    }

                    pausaAleatoria(1500, 3000);
                }

                stacksProcessadas++;

                if (stacksProcessadas % TAMANHO_LOTE == 0 && stacksProcessadas < stacks.size()) {
                    int pausa = 20000 + random.nextInt(10000);
                    System.out.println("⏸️ [Empregos.com.br] Pausa entre lotes: " + pausa / 1000 + "s");
                    Thread.sleep(pausa);
                } else {
                    pausaAleatoria(4000, 8000);
                }
            }

            System.out.println("\n════════════════════════════════════");
            System.out.println("🎉 [Empregos.com.br] Finalizado!");
            System.out.println("💾 Novas vagas: " + totalSalvo);
            System.out.println("🔗 Stacks associadas: " + totalAssociado);
            System.out.println("════════════════════════════════════");

        } catch (Exception e) {
            System.err.println("❌ [Empregos.com.br] Erro geral: " + e.getMessage());
            e.printStackTrace();
        } finally {
            driver.quit();
            System.out.println("🏁 [Empregos.com.br] Navegador fechado.");
        }
    }

    // ── Extração ──────────────────────────────────────────────────────────

    private List<String[]> extrairVagasDaPagina(WebDriver driver) {
        List<String[]> resultado = new ArrayList<>();

        // Empregos.com.br: links de vaga contêm /emprego/ no href
        List<WebElement> links = driver.findElements(
                By.cssSelector("a[href*='/vaga/']"));

        for (WebElement link : links) {
            try {
                String href = link.getAttribute("href");
                if (href == null || href.isBlank()) continue;
                if (href.startsWith("/")) href = BASE_HOST + href;
                final String linkFinal = href.split("\\?")[0];

                String titulo = link.getText().trim();
                if (titulo.isBlank()) {
                    try {
                        titulo = link.findElement(
                                By.cssSelector("h2, h3, [class*='title' i], [class*='titulo' i]"))
                                .getText().trim();
                    } catch (NoSuchElementException ignored) {}
                }
                if (titulo.isBlank()) continue;

                String empresa = "Não informada";
                try {
                    WebElement container = link.findElement(
                            By.xpath("./ancestor::li[1] | ./ancestor::article[1] "
                                   + "| ./ancestor::div[contains(@class,'vaga')][1] "
                                   + "| ./ancestor::div[contains(@class,'job')][1]"));
                    empresa = extrairEmpresa(container);
                } catch (Exception ignored) {}

                boolean duplicado = resultado.stream().anyMatch(v -> v[0].equals(linkFinal));
                if (!duplicado) {
                    resultado.add(new String[]{linkFinal, titulo, empresa});
                }
            } catch (Exception ignored) {}
        }

        return resultado;
    }

    private String extrairEmpresa(WebElement container) {
        String[] selectors = {
            "[class*='empresa' i]",
            "[class*='company' i]",
            "[class*='employer' i]",
            "span[class*='nome' i]"
        };
        for (String sel : selectors) {
            try {
                String t = container.findElement(By.cssSelector(sel)).getText().trim();
                if (!t.isBlank()) return t;
            } catch (NoSuchElementException ignored) {}
        }
        return "Não informada";
    }

    private boolean esperarVagas(WebDriver driver, WebDriverWait wait) {
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("a[href*='/vaga/']")));
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    // ── Diagnóstico ───────────────────────────────────────────────────────

    private void diagnosticar(WebDriver driver) {
        System.out.println("🔬 [Empregos.com.br] === DIAGNÓSTICO ===");
        System.out.println("   Título: " + driver.getTitle());
        System.out.println("   URL: " + driver.getCurrentUrl());

        System.out.println("   Botões visíveis:");
        int bc = 0;
        for (WebElement b : driver.findElements(By.tagName("button"))) {
            if (b.isDisplayed() && !b.getText().isBlank()) {
                System.out.println("     \"" + b.getText().trim() + "\"");
                if (++bc >= 5) break;
            }
        }

        System.out.println("   Primeiros 8 hrefs:");
        int ac = 0;
        for (WebElement a : driver.findElements(By.tagName("a"))) {
            String href = a.getAttribute("href");
            if (href != null && !href.isBlank() && !href.startsWith("javascript")) {
                System.out.println("     " + href);
                if (++ac >= 8) break;
            }
        }
        System.out.println("🔬 === FIM DIAGNÓSTICO ===");
    }

    // ── Cookies ───────────────────────────────────────────────────────────

    private void fecharBannerCookies(WebDriver driver, JavascriptExecutor js) {
        // 1ª: API JS (OneTrust / Didomi)
        try {
            js.executeScript(
                "if (typeof OneTrust !== 'undefined') { OneTrust.AllowAll(); } " +
                "else if (typeof Didomi !== 'undefined') { Didomi.setUserAgreeToAll(); }"
            );
            pausaAleatoria(500, 800);
        } catch (Exception ignored) {}

        // 2ª: botão pelo texto — aguarda até 6s para aparecer
        try {
            WebElement btn = new WebDriverWait(driver, Duration.ofSeconds(6)).until(d -> {
                for (WebElement b : d.findElements(By.tagName("button"))) {
                    if (!b.isDisplayed()) continue;
                    String txt = b.getText().toLowerCase().trim();
                    if (txt.contains("aceitar") || txt.contains("concordar")
                            || txt.contains("permitir") || txt.contains("accept")
                            || txt.contains("agree")) return b;
                }
                return null;
            });
            btn.click();
            System.out.println("🍪 [Empregos.com.br] Cookies aceitos: \"" + btn.getText().trim() + "\"");
            pausaAleatoria(600, 1000);
        } catch (Exception ignored) {}
    }

    // ── Utilitários ───────────────────────────────────────────────────────

    private String toSlug(String nome) {
        return nome.toLowerCase()
                .replace("c#",      "csharp")
                .replace("c++",     "cplusplus")
                .replace(".net",    "net")
                .replace("node.js", "nodejs")
                .replace(" ",       "-")
                .replace("#",       "sharp")
                .replace("+",       "plus")
                .replaceAll("[^a-z0-9\\-]", "");
    }

    private void pausaAleatoria(int minMs, int maxMs) throws InterruptedException {
        Thread.sleep(minMs + random.nextInt(maxMs - minMs));
    }
}
