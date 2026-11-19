package iterative_reflection.mvp.platform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import iterative_reflection.mvp.MvpApplication;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

/** A deployment that forgets its profile must fail fast instead of silently running the dev setup. */
class NoDefaultProfileTest {

	@Test
	void startingWithoutAProfileFailsInsteadOfFallingBackToDev() {
		assertThatThrownBy(() -> new SpringApplicationBuilder(MvpApplication.class).web(WebApplicationType.NONE)
				.properties("spring.main.banner-mode=off").run()).hasStackTraceContaining("must start with \"jdbc\"");
	}

	@Test
	void devDatabaseIsFileBasedSoSessionsSurviveARestart() throws Exception {
		Properties dev = PropertiesLoaderUtils.loadProperties(new ClassPathResource("application-dev.properties"));
		assertThat(dev.getProperty("spring.datasource.url")).startsWith("jdbc:h2:file:");
	}

}
