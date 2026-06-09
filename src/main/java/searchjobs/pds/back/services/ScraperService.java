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
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
@Order(2)
public class ScraperService implements CommandLineRunner {

    private final JobService jobService;
    private final StackRepository stackRepository;
    private final JobRepository jobRepository;
    private final Random random = new Random();

    private static final int MAX_PAGINAS_CARGA_INICIAL = 5; // ~50 vagas por stack
    private static final int MAX_PAGINAS_ATUALIZACAO   = 1; // ~20 vagas recentes por stack
    private static final int TAMANHO_LOTE              = 5; // pausa a cada 5 stacks

    public ScraperService(JobService jobService, StackRepository stackRepository, JobRepository jobRepository) {
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
            System.out.println("⚠️ Nenhuma stack no banco. Verifique o DatabaseSeeder.");
            return;
        }

        boolean cargaInicial = jobRepository.count() == 0;

        if (cargaInicial) {
            System.out.println("🚀 Banco vazio — CARGA INICIAL (" + MAX_PAGINAS_CARGA_INICIAL + " páginas por stack)");
        } else {
            System.out.println("🔄 Banco populado — ATUALIZAÇÃO (" + MAX_PAGINAS_ATUALIZACAO + " página por stack)");
        }
        System.out.println("📦 Total de stacks: " + stacks.size());

        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--start-maximized");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.setExperimentalOption("excludeSwitches", List.of("enable-automation"));
        options.setExperimentalOption("useAutomationExtension", false);
        options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");

        WebDriver driver = new ChromeDriver(options);
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

        int totalSalvo     = 0;
        int totalAssociado = 0;
        int stacksProcessadas = 0;

        try {
            // Abre o site uma vez só para aceitar cookies
            driver.get("https://portal.gupy.io/job-search/term=java");
            System.out.println("🌍 Site carregado.");

            try {
                WebElement btnCookies = wait.until(ExpectedConditions.presenceOfElementLocated(
                        By.cssSelector("span[aria-label='Aceitar Cookies'], span.cc-dismiss")));
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btnCookies);
                System.out.println("🍪 Cookies aceitos.");
                pausaAleatoria(2000, 3500);
            } catch (TimeoutException e) {
                System.out.println("⚠️ Banner de cookies não apareceu, continuando...");
            }

            for (Stack stack : stacks) {
                String nomeStack  = stack.getNome();
                String termoBusca = nomeStack.replace(" ", "%20").replace("#", "%23");
                String ordenacao = cargaInicial ? "" : "&sortBy=publishedDate&sortOrder=desc";
                String url = cargaInicial
                        ? "https://portal.gupy.io/job-search/term=" + termoBusca
                        : "https://portal.gupy.io/job-search/sortBy=publishedDate&sortOrder=desc&term=" + termoBusca;
                int limitePaginas = cargaInicial ? MAX_PAGINAS_CARGA_INICIAL : MAX_PAGINAS_ATUALIZACAO;

                System.out.println("\n════════════════════════════════════");
                System.out.println("🔍 Stack " + (stacksProcessadas + 1) + "/" + stacks.size() + ": " + nomeStack);
                System.out.println(cargaInicial
                        ? "📋 Modo: carga inicial (" + MAX_PAGINAS_CARGA_INICIAL + " páginas)"
                        : "⚡ Modo: atualização (" + MAX_PAGINAS_ATUALIZACAO + " página)");
                System.out.println("════════════════════════════════════");

                driver.get(url);
                pausaAleatoria(2500, 4500);

                try {
                    wait.until(ExpectedConditions.presenceOfElementLocated(
                            By.cssSelector("a[href*='.gupy.io/jobs/']")
                    ));
                } catch (TimeoutException e) {
                    System.out.println("⚠️ Sem vagas para: " + nomeStack + " — pulando.");
                    stacksProcessadas++;
                    continue;
                }

                int pagina = 1;

                while (true) {
                    if (pagina > limitePaginas) {
                        System.out.println("✅ Limite de " + limitePaginas + " páginas atingido para: " + nomeStack);
                        break;
                    }

                    System.out.println("📄 Página " + pagina + "/" + limitePaginas + "...");

                    pausaAleatoria(1000, 2000);

                    List<WebElement> links = driver.findElements(
                            By.cssSelector("a[href*='.gupy.io/jobs/']")
                    );
                    if (!cargaInicial && links.size() > 10)
                        links = links.subList(0, 10);
                    System.out.println("🔎 Vagas nesta página: " + links.size());

                    for (WebElement linkEl : links) {
                        try {
                            String linkVaga = linkEl.getAttribute("href");
                            if (linkVaga == null) continue;

                            String titulo = "";
                            try {
                                titulo = linkEl.findElement(By.cssSelector("h3")).getText().trim();
                            } catch (NoSuchElementException ignored) {}
                            if (titulo.isBlank()) titulo = linkEl.getText().trim();
                            if (titulo.isBlank()) continue;

                            String empresa = "Não informada";
                            try {
                                WebElement container = linkEl.findElement(
                                        By.xpath("./ancestor::li[1]"));
                                WebElement empresaEl = container.findElement(
                                        By.cssSelector("div[aria-label*='Empresa']"));
                                empresa = empresaEl.getAttribute("aria-label")
                                        .replace("Empresa ", "").trim();
                            } catch (NoSuchElementException ignored) {}

                            Optional<Job> vagaExistente = jobService.buscarPorLink(linkVaga);

                            if (vagaExistente.isPresent()) {
                                jobService.associarStack(vagaExistente.get(), stack);
                                totalAssociado++;
                                System.out.println("🔗 [" + nomeStack + "] → " + titulo);
                            } else {
                                Job novaVaga = new Job();
                                novaVaga.setTitulo(titulo);
                                novaVaga.setEmpresa(empresa);
                                novaVaga.setFonte("Gupy");
                                novaVaga.setDataColeta(LocalDateTime.now());
                                novaVaga.setLinkOriginal(linkVaga);
                                novaVaga.getStacksRequisitadas().add(stack);
                                novaVaga.setDescricao(buscarDescricao(driver, linkVaga));

                                jobService.salvarVaga(novaVaga);
                                totalSalvo++;
                                System.out.println("💾 " + titulo + " | " + empresa);
                            }

                        } catch (Exception e) {
                            System.out.println("⚠️ Erro num card: " + e.getMessage());
                        }
                    }

                    // Próxima página
                    try {
                        WebElement btnProxima = driver.findElement(
                                By.cssSelector("nav[aria-label*='paginação'] li:last-child button")
                        );

                        if (!btnProxima.isEnabled() || btnProxima.getAttribute("disabled") != null) {
                            System.out.println("✅ Última página de: " + nomeStack);
                            break;
                        }

                        js.executeScript("arguments[0].scrollIntoView({block: 'center'});", btnProxima);
                        pausaAleatoria(800, 1500);
                        btnProxima.click();
                        pagina++;
                        pausaAleatoria(2000, 3500);

                    } catch (NoSuchElementException e) {
                        System.out.println("✅ Sem mais páginas para: " + nomeStack);
                        break;
                    }
                }

                stacksProcessadas++;

                // Pausa maior a cada TAMANHO_LOTE stacks
                if (stacksProcessadas % TAMANHO_LOTE == 0 && stacksProcessadas < stacks.size()) {
                    int pausaLote = 15000 + random.nextInt(10000);
                    System.out.println("\n⏸️ Pausa entre lotes: " + (pausaLote / 1000) + "s...");
                    Thread.sleep(pausaLote);
                } else {
                    pausaAleatoria(4000, 8000);
                }
            }

            System.out.println("\n════════════════════════════════════");
            System.out.println("🎉 Scraping finalizado!");
            System.out.println("💾 Vagas novas salvas: " + totalSalvo);
            System.out.println("🔗 Stacks associadas: " + totalAssociado);
            System.out.println("════════════════════════════════════");

        } catch (Exception e) {
            System.err.println("❌ Erro geral: " + e.getMessage());
            e.printStackTrace();
        } finally {
            driver.quit();
            System.out.println("🏁 Navegador fechado.");
        }
    }

    private String buscarDescricao(WebDriver driver, String jobUrl) {
        String mainWindow = driver.getWindowHandle();
        String newWindow = null;
        try {
            ((JavascriptExecutor) driver).executeScript("window.open('')");
            newWindow = driver.getWindowHandles().stream()
                    .filter(h -> !h.equals(mainWindow))
                    .findFirst().orElse(null);
            if (newWindow == null) return "";

            driver.switchTo().window(newWindow);
            driver.get(jobUrl);
            Thread.sleep(2500);

            // Aceita cookies se o banner aparecer (só ocorre na primeira vaga da sessão)
            try {
                WebElement btnCookie = new WebDriverWait(driver, Duration.ofSeconds(5))
                        .until(ExpectedConditions.presenceOfElementLocated(
                                By.cssSelector("span[aria-label='Aceitar Cookies'], span.cc-dismiss")));
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btnCookie);
                Thread.sleep(1000);
            } catch (Exception ignored) {}

            String[] selectors = {
                "div[data-testid='text-section']",
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
        } catch (Exception e) {
            return "";
        } finally {
            try {
                if (newWindow != null && driver.getWindowHandles().contains(newWindow)) {
                    driver.switchTo().window(newWindow);
                    driver.close();
                }
                driver.switchTo().window(mainWindow);
            } catch (Exception ignored) {}
        }
    }

    private void pausaAleatoria(int minMs, int maxMs) throws InterruptedException {
        int pausa = minMs + random.nextInt(maxMs - minMs);
        Thread.sleep(pausa);
    }
}