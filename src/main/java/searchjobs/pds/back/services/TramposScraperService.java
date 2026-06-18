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
@Order(2)
public class TramposScraperService implements CommandLineRunner {

    private final JobService jobService;
    private final StackRepository stackRepository;
    private final JobRepository jobRepository;
    private final Random random = new Random();

    private static final int MAX_PAGINAS_CARGA_INICIAL = 5;
    private static final int MAX_PAGINAS_ATUALIZACAO   = 1;
    private static final int TAMANHO_LOTE              = 5;

    private static final String BASE_URL  = "https://trampos.co/oportunidades";
    private static final String BASE_HOST = "https://trampos.co";

    public TramposScraperService(JobService jobService,
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
            System.out.println("⚠️ [Trampos] Nenhuma stack no banco.");
            return;
        }

        boolean cargaInicial = jobRepository.count() == 0;

        System.out.println(cargaInicial
                ? "🚀 [Trampos] CARGA INICIAL (" + MAX_PAGINAS_CARGA_INICIAL + " páginas por stack)"
                : "🔄 [Trampos] ATUALIZAÇÃO (" + MAX_PAGINAS_ATUALIZACAO + " página por stack)");
        System.out.println("📦 [Trampos] Stacks: " + stacks.size());

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
            driver.get(BASE_URL + "?tr=java");
            System.out.println("🌍 [Trampos] Site carregado.");
            pausaAleatoria(2000, 3500);
            fecharBannerCookies(driver);

            for (Stack stack : stacks) {
                String nomeStack = stack.getNome();
                String termo = nomeStack
                        .replace(" ", "%20")
                        .replace("#", "%23")
                        .replace("+", "%2B");
                int limitePaginas = cargaInicial ? MAX_PAGINAS_CARGA_INICIAL : MAX_PAGINAS_ATUALIZACAO;

                System.out.println("\n════════════════════════════════════");
                System.out.println("🔍 [Trampos] Stack " + (stacksProcessadas + 1)
                        + "/" + stacks.size() + ": " + nomeStack);
                System.out.println("════════════════════════════════════");

                for (int pagina = 1; pagina <= limitePaginas; pagina++) {
                    String url = pagina == 1
                            ? BASE_URL + "?tr=" + termo
                            : BASE_URL + "?tr=" + termo + "&page=" + pagina;

                    System.out.println("📄 [Trampos] Página " + pagina + "/" + limitePaginas + " → " + url);

                    driver.get(url);
                    pausaAleatoria(2000, 3500);
                    fecharBannerCookies(driver);

                    boolean temVagas = esperarVagas(driver, wait);
                    if (!temVagas) {
                        System.out.println("⚠️ [Trampos] Sem vagas na página " + pagina + " para: " + nomeStack);
                        diagnosticar(driver);
                        break;
                    }

                    List<String[]> vagas = extrairVagasDaPagina(driver, js);
                    if (!cargaInicial && vagas.size() > 10)
                        vagas = vagas.subList(0, 10);
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
                            if (!ScraperJobFilter.eTechJob(titulo)) {
                                System.out.println("⏭️ [Trampos] Ignorada (não é vaga de TI): " + titulo);
                                continue;
                            }

                            Optional<Job> existente = jobService.buscarPorLink(link);
                            if (existente.isPresent()) {
                                Job jobExistente = existente.get();
                                String textoExistente = titulo + " "
                                        + (jobExistente.getDescricao() != null ? jobExistente.getDescricao() : "");
                                if (!stackConflitaComTexto(textoExistente, nomeStack)) {
                                    jobService.associarStack(jobExistente, stack);
                                    totalAssociado++;
                                    System.out.println("🔗 [" + nomeStack + "] → " + titulo);
                                }
                            } else {
                                // Busca descrição primeiro para usar na checagem de conflito
                                String descricao = buscarDescricao(driver, link);
                                String textoCompleto = titulo + " " + descricao;

                                Job novaVaga = new Job();
                                novaVaga.setTitulo(titulo);
                                novaVaga.setEmpresa(empresa);
                                novaVaga.setFonte("Trampos");
                                novaVaga.setDataColeta(LocalDateTime.now());
                                novaVaga.setLinkOriginal(link);
                                novaVaga.setDescricao(descricao);
                                if (!stackConflitaComTexto(textoCompleto, nomeStack)) {
                                    novaVaga.getStacksRequisitadas().add(stack);
                                }
                                jobService.salvarVaga(novaVaga);
                                totalSalvo++;
                                System.out.println("💾 " + titulo + " | " + empresa);
                            }
                        } catch (Exception e) {
                            System.out.println("⚠️ [Trampos] Erro ao salvar: " + e.getMessage());
                        }
                    }

                    pausaAleatoria(1500, 3000);
                }

                stacksProcessadas++;

                if (stacksProcessadas % TAMANHO_LOTE == 0 && stacksProcessadas < stacks.size()) {
                    int pausa = 15000 + random.nextInt(10000);
                    System.out.println("⏸️ [Trampos] Pausa entre lotes: " + pausa / 1000 + "s");
                    Thread.sleep(pausa);
                } else {
                    pausaAleatoria(4000, 8000);
                }
            }

            System.out.println("\n════════════════════════════════════");
            System.out.println("🎉 [Trampos] Finalizado!");
            System.out.println("💾 Novas vagas: " + totalSalvo);
            System.out.println("🔗 Stacks associadas: " + totalAssociado);
            System.out.println("════════════════════════════════════");

        } catch (Exception e) {
            System.err.println("❌ [Trampos] Erro geral: " + e.getMessage());
            e.printStackTrace();
        } finally {
            driver.quit();
            System.out.println("🏁 [Trampos] Navegador fechado.");
        }
    }

    // ── Extração ──────────────────────────────────────────────────────────

    private List<String[]> extrairVagasDaPagina(WebDriver driver, JavascriptExecutor js) {
        List<String[]> resultado = new ArrayList<>();

        // Estratégia heading-first: parte de h2/h3/h4, sobe até encontrar
        // o link /oportunidades/ no container pai — resiliente a mudanças de classe.
        Object rawResult = js.executeScript(
            "var items = [];" +
            "var seen = new Set();" +
            "document.querySelectorAll('h2, h3, h4').forEach(function(h) {" +
            "  var titulo = h.innerText.trim();" +
            "  if (!titulo || titulo.length < 5 || titulo.length > 150) return;" +
            "  var node = h.parentElement;" +
            "  var vagaHref = null, empresa = '';" +
            "  for (var d = 0; d < 8 && node; d++) {" +
            "    var a = node.querySelector(\"a[href*='/oportunidades/']\");" +
            "    if (a && !a.href.startsWith('mailto:')) {" +
            "      vagaHref = a.href.split('?')[0];" +
            "      var empEl = node.querySelector('[class*=\"company\"], [class*=\"empresa\"], [class*=\"employer\"], [class*=\"organization\"], [class*=\"client\"]');" +
            "      if (empEl) empresa = empEl.innerText.trim();" +
            "      break;" +
            "    }" +
            "    node = node.parentElement;" +
            "  }" +
            "  if (!vagaHref || seen.has(vagaHref)) return;" +
            "  seen.add(vagaHref);" +
            "  items.push([vagaHref, titulo, empresa]);" +
            "});" +
            "return items;"
        );

        if (!(rawResult instanceof List<?> rawList)) return resultado;

        System.out.println("   🔗 Candidatos encontrados via JS: " + rawList.size());

        for (Object item : rawList) {
            if (!(item instanceof List<?> row) || row.size() < 3) continue;
            String href    = String.valueOf(row.get(0)).trim();
            String titulo  = String.valueOf(row.get(1)).trim();
            String emp     = String.valueOf(row.get(2)).trim();

            if (href.isEmpty() || !href.contains("/oportunidades/")) continue;
            if (titulo.isBlank()) continue;
            if (titulo.equalsIgnoreCase("ver mais")
                    || titulo.equalsIgnoreCase("mais detalhes")
                    || titulo.equalsIgnoreCase("filtros")) continue;

            resultado.add(new String[]{href, titulo, emp.isBlank() ? "Não informada" : emp});
        }

        return resultado;
    }

    private boolean esperarVagas(WebDriver driver, WebDriverWait wait) {
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("a[href*='/oportunidades/']")));
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    // ── Descrição ─────────────────────────────────────────────────────────

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

            // Aguarda o Ember.js renderizar o conteúdo da vaga
            try {
                new WebDriverWait(driver, Duration.ofSeconds(10))
                        .until(ExpectedConditions.presenceOfElementLocated(
                                By.cssSelector("div.description .text, div.description")));
            } catch (TimeoutException ignored) {}

            // div.description contém Descrição + Requisitos no Trampos
            try {
                String texto = driver.findElement(By.cssSelector("div.description")).getText().trim();
                if (texto.length() > 20) return texto;
            } catch (NoSuchElementException ignored) {}

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

    // ── Diagnóstico ───────────────────────────────────────────────────────

    private void diagnosticar(WebDriver driver) {
        System.out.println("🔬 [Trampos] === DIAGNÓSTICO ===");
        System.out.println("   Título: " + driver.getTitle());
        System.out.println("   URL: " + driver.getCurrentUrl());
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

    private void fecharBannerCookies(WebDriver driver) {
        try {
            WebElement btn = new WebDriverWait(driver, Duration.ofSeconds(5)).until(d -> {
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
            System.out.println("🍪 [Trampos] Cookies aceitos: \"" + btn.getText().trim() + "\"");
            pausaAleatoria(600, 1000);
        } catch (Exception ignored) {
            System.out.println("ℹ️ [Trampos] Banner de cookies não encontrado — continuando.");
        }
    }

    // ── Utilitários ───────────────────────────────────────────────────────

    // Retorna true se o texto (título + descrição) contém o nome da stack como SUBSTRING
    // mas NÃO como palavra inteira — indica falso positivo de substring.
    // Ex: busca "java", texto contém "javascript" mas não "java" isolado → conflito → não associa.
    // Textos sem menção alguma ao termo ("Backend Developer") → sem conflito → associa (confia na busca).
    private boolean stackConflitaComTexto(String texto, String nomeStack) {
        String t = texto.toLowerCase();
        String s = nomeStack.toLowerCase();
        String sRegex = s.replace(".", "\\.");
        boolean contemSubstring      = t.contains(s);
        boolean contemPalavraInteira = t.matches(".*\\b" + sRegex + "\\b.*");
        return contemSubstring && !contemPalavraInteira;
    }

    private void pausaAleatoria(int minMs, int maxMs) throws InterruptedException {
        Thread.sleep(minMs + random.nextInt(maxMs - minMs));
    }
}
