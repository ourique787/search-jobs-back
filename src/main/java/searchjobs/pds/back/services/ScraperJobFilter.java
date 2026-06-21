package searchjobs.pds.back.services;

import java.util.List;

public class ScraperJobFilter {

    private static final List<String> INDICADORES_TECH = List.of(
        "desenvolvedor", "developer", "engineer", "engenheiro", "engenheira",
        "programador", "programmer", "analista", "analyst",
        "arquiteto", "architect", "tech lead", "techlead",
        "software", "backend", "front-end", "frontend", "fullstack", "full stack", "full-stack",
        "devops", "sre", "cloud", "dados", "data", "database", "dba",
        "qa ", "quality", "tester", "testador",
        "ux", "ui designer", "product manager", "product owner", "scrum",
        "infra", "infraestrutura", "sysadmin", "suporte técnico", "suporte tecnico",
        "mobile", "android", "ios", "web", "ti ", " ti,", "t.i",
        "técnico", "tecnico", "informática", "informatica",
        "sistemas", "redes", "segurança da informação", "segurança da informacao",
        "inteligência artificial", "machine learning", "ciência de dados"
    );

    // Padrões claramente não-tech. Indicador tech tem prioridade sobre este bloqueio.
    private static final List<String> BLOQUEADOS_NAO_TECH = List.of(
        "auxiliar de cozinha", "auxiliar de limpeza", "auxiliar de produção",
        "auxiliar de producao", "auxiliar administrativo", "auxiliar de estoque",
        "auxiliar de logística", "auxiliar de logistica",
        "motorista", "caminhoneiro", "motoboy", "entregador",
        "cozinheiro", "cozinheira",
        "eletricista", "encanador", "pedreiro", "pintor", "marceneiro",
        "faxineiro", "faxineira",
        "vigilante", "porteiro", "segurança patrimonial",
        "enfermeiro", "médico", "fisioterapeuta", "nutricionista", "farmacêutico",
        "professor de educação", "professor de matemática", "professor de português",
        "operador de caixa", "repositor", "estoquista"
    );

    // Tech tem prioridade sobre blocklist.
    // Se nenhum sinal: mantém (permissivo — a busca já filtra por stack tech).
    public static boolean eTechJob(String titulo) {
        if (titulo == null || titulo.isBlank()) return false;
        String t = titulo.toLowerCase();
        if (INDICADORES_TECH.stream().anyMatch(t::contains)) return true;
        if (BLOQUEADOS_NAO_TECH.stream().anyMatch(t::contains)) return false;
        return true;
    }

    private ScraperJobFilter() {}
}
