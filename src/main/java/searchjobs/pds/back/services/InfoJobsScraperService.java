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
@Order(3)
public class InfoJobsScraperService implements CommandLineRunner {

    private final JobService jobService;
    private final StackRepository stackRepository;
    private final JobRepository jobRepository;
    private final Random random = new Random();

    private static final int MAX_PAGINAS_CARGA_INICIAL = 5;
    private static final int MAX_PAGINAS_ATUALIZACAO   = 1;
    private static final int TAMANHO_LOTE              = 5;

    // URL real de busca — parâmetro "palabra" (espanhol, origem do InfoJobs)
    private static final String SEARCH_URL = "https://www.infojobs.com.br/empregos.aspx?palabra=";
    private static final String BASE_HOST  = "https://www.infojobs.com.br";

    public InfoJobsScraperService(JobService jobService,
                                  StackRepository stackRepository,
                                  JobRepository jobRepository) {
        this.jobService = jobService;
        this.stackRepository = stackRepository;
        this.jobRepository = jobRepository;
    }

    @Override
    public void run(String... args) throws Exception {
      //  iniciarScraping();
    }

    public void iniciarScraping() {
        List<Stack> stacks = stackRepository.findAll();

        if (stacks.isEmpty()) {
            System.out.println("⚠️ [InfoJobs] Nenhuma stack no banco.");
            return;
        }

        boolean cargaInicial = jobRepository.count() == 0;

        System.out.println(cargaInicial
                ? "🚀 [InfoJobs] CARGA INICIAL (5 páginas por stack)"
                : "🔄 [InfoJobs] ATUALIZAÇÃO (1 página por stack)");
        System.out.println("📦 [InfoJobs] Stacks: " + stacks.size());

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
            driver.get(SEARCH_URL + "java");
            System.out.println("🌍 [InfoJobs] Site carregado.");
            pausaAleatoria(2500, 4000);
            fecharBannerCookies(driver, js);

            for (Stack stack : stacks) {
                String nomeStack  = stack.getNome();
                String termo = nomeStack.replace(" ", "+")
                        .replace("#", "%23");

                int limitePaginas = cargaInicial ? MAX_PAGINAS_CARGA_INICIAL : MAX_PAGINAS_ATUALIZACAO;

                System.out.println("\n════════════════════════════════════");
                System.out.println("🔍 [InfoJobs] Stack " + (stacksProcessadas + 1)
                        + "/" + stacks.size() + ": " + nomeStack);
                System.out.println("════════════════════════════════════");

                for (int pagina = 1; pagina <= limitePaginas; pagina++) {
                    String url = pagina == 1
                            ? SEARCH_URL + termo
                            : SEARCH_URL + termo + "&pagina=" + pagina;

                    System.out.println("📄 [InfoJobs] Página " + pagina + "/" + limitePaginas
                            + " → " + url);

                    driver.get(url);
                    pausaAleatoria(2500, 4000);
                    fecharBannerCookies(driver, js);

                    // Espera cards ou detecta "nenhuma vaga"
                    boolean temVagas = esperarCards(driver, wait);
                    if (!temVagas) {
                        System.out.println("⚠️ [InfoJobs] Sem vagas na página " + pagina
                                + " para: " + nomeStack);
                        break;
                    }

                    List<String[]> vagas = extrairVagasDaPagina(driver);
                    System.out.println("🔎 Vagas extraídas: " + vagas.size());

                    for (String[] vaga : vagas) {
                        String link   = vaga[0];
                        String titulo = vaga[1];
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
                                novaVaga.setFonte("InfoJobs");
                                novaVaga.setDataColeta(LocalDateTime.now());
                                novaVaga.setLinkOriginal(link);
                                novaVaga.getStacksRequisitadas().add(stack);
                                jobService.salvarVaga(novaVaga);
                                totalSalvo++;
                                System.out.println("💾 " + titulo + " | " + empresa);
                            }
                        } catch (Exception e) {
                            System.out.println("⚠️ [InfoJobs] Erro ao salvar: " + e.getMessage());
                        }
                    }

                    pausaAleatoria(1500, 3000);
                }

                stacksProcessadas++;

                if (stacksProcessadas % TAMANHO_LOTE == 0 && stacksProcessadas < stacks.size()) {
                    int pausa = 20000 + random.nextInt(10000);
                    System.out.println("⏸️ [InfoJobs] Pausa entre lotes: " + pausa / 1000 + "s");
                    Thread.sleep(pausa);
                } else {
                    pausaAleatoria(4000, 8000);
                }
            }

            System.out.println("\n════════════════════════════════════");
            System.out.println("🎉 [InfoJobs] Finalizado!");
            System.out.println("💾 Novas vagas: " + totalSalvo);
            System.out.println("🔗 Stacks associadas: " + totalAssociado);
            System.out.println("════════════════════════════════════");

        } catch (Exception e) {
            System.err.println("❌ [InfoJobs] Erro geral: " + e.getMessage());
            e.printStackTrace();
        } finally {
            driver.quit();
            System.out.println("🏁 [InfoJobs] Navegador fechado.");
        }
    }

    // ── Extração ───────────────────────────────────────────────────────────

    /**
     * InfoJobs coloca todos os links de vagas com href iniciando em /vaga-de-
     * Percorre esses links e monta os dados a partir do elemento pai.
     */
    private List<String[]> extrairVagasDaPagina(WebDriver driver) {
        List<String[]> resultado = new ArrayList<>();

        // Links de vagas sempre têm /vaga-de- no href
        List<WebElement> links = driver.findElements(
                By.cssSelector("a[href*='/vaga-de-']"));

        for (WebElement link : links) {
            try {
                String href = link.getAttribute("href");
                if (href == null || href.isBlank()) continue;
                final String finalHref = href.startsWith("/") ? BASE_HOST + href : href;

                String titulo = link.getText().trim();
                if (titulo.isBlank()) {
                    // título pode estar num filho do link
                    try { titulo = link.findElement(By.cssSelector("h2,h3,span")).getText().trim(); }
                    catch (NoSuchElementException ignored) {}
                }
                if (titulo.isBlank()) continue;

                // Empresa: procura no container pai do link
                String empresa = "Não informada";
                try {
                    WebElement container = link.findElement(
                            By.xpath("./ancestor::li[1] | ./ancestor::div[contains(@class,'job')][1]"
                                    + " | ./ancestor::article[1]"));
                    empresa = extrairEmpresaDoContainer(container);
                } catch (Exception ignored) {}

                // Evita duplicatas dentro da mesma página
                boolean jaAdicionado = resultado.stream().anyMatch(v -> v[0].equals(finalHref));
                if (!jaAdicionado) {
                    resultado.add(new String[]{finalHref, titulo, empresa});
                }
            } catch (Exception ignored) {}
        }

        return resultado;
    }

    private String extrairEmpresaDoContainer(WebElement container) {
        String[] selectors = {
            "a[href*='/empresa-']",
            "[class*='empresa']",
            "[class*='company']",
            "[class*='employer']"
        };
        for (String sel : selectors) {
            try {
                String t = container.findElement(By.cssSelector(sel)).getText().trim();
                if (!t.isBlank()) return t;
            } catch (NoSuchElementException ignored) {}
        }
        return "Não informada";
    }

    private boolean esperarCards(WebDriver driver, WebDriverWait wait) {
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("a[href*='/vaga-de-']")));
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    // ── Utilitários ───────────────────────────────────────────────────────

    private void fecharBannerCookies(WebDriver driver, JavascriptExecutor js) {
        // 1ª tentativa: API JavaScript do Didomi (mais confiável que clicar no botão)
        try {
            js.executeScript(
                "if (typeof Didomi !== 'undefined') { " +
                "  Didomi.setUserAgreeToAll(); " +
                "}"
            );
            pausaAleatoria(800, 1200);
            // Verifica se o banner sumiu
            boolean bannerSumiu = driver.findElements(By.cssSelector("#didomi-notice")).isEmpty();
            if (bannerSumiu) {
                System.out.println("🍪 [InfoJobs] Cookies aceitos via API Didomi.");
                return;
            }
        } catch (Exception ignored) {}

        // 2ª tentativa: clicar no botão (fallback)
        String[] selectors = {
            "#didomi-notice-agree-button",
            "button[id*='didomi'][id*='agree']",
            "button.didomi-components-button--filled",
            "button#onetrust-accept-btn-handler"
        };
        for (String sel : selectors) {
            try {
                WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(3));
                WebElement btn = shortWait.until(
                        ExpectedConditions.elementToBeClickable(By.cssSelector(sel)));
                btn.click();
                System.out.println("🍪 [InfoJobs] Cookies aceitos via botão (" + sel + ").");
                pausaAleatoria(800, 1200);
                return;
            } catch (Exception ignored) {}
        }

        System.out.println("ℹ️ [InfoJobs] Banner não encontrado — continuando.");
    }

    private void pausaAleatoria(int minMs, int maxMs) throws InterruptedException {
        Thread.sleep(minMs + random.nextInt(maxMs - minMs));
    }
}