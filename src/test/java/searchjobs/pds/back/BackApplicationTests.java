package searchjobs.pds.back;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import searchjobs.pds.back.services.ScraperService;

@SpringBootTest
@ActiveProfiles("test")
class BackApplicationTests {

	@MockitoBean
	private ScraperService scraperService;

	@Test
	void contextLoads() {
	}

}
