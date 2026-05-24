package searchjobs.pds.back;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import searchjobs.pds.back.config.DatabaseSeeder;
import searchjobs.pds.back.services.EmpregosBrScraperService;
import searchjobs.pds.back.services.InfoJobsScraperService;
import searchjobs.pds.back.services.JobEnricherService;
import searchjobs.pds.back.services.ScraperService;

@SpringBootTest
@ActiveProfiles("test")
class BackApplicationTests {

	@MockitoBean
	private DatabaseSeeder databaseSeeder;

	@MockitoBean
	private ScraperService scraperService;

	@MockitoBean
	private InfoJobsScraperService infoJobsScraperService;

	@MockitoBean
	private EmpregosBrScraperService empregosBrScraperService;

	@MockitoBean
	private JobEnricherService jobEnricherService;

	@Test
	void contextLoads() {
	}

}
