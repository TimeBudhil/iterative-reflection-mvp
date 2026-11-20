package iterative_reflection.mvp;

import static org.assertj.core.api.Assertions.assertThat;

import iterative_reflection.mvp.settings.ThemeSettings;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/** Enforces module boundaries: no cycles, and no access to another module's {@code internal} package [N4.1, N4.2]. */
class ModularityTest {

	static final ApplicationModules MODULES = ApplicationModules.of(MvpApplication.class);

	@Test
	void verifiesModuleBoundaries() {
		MODULES.verify();
	}

	/** Settings depends on no other module; declaring it makes {@code verify()} fail if that ever changes. */
	@Test
	void settingsDeclaresItDependsOnNoOtherModule() {
		ApplicationModule declared = ThemeSettings.class.getPackage().getAnnotation(ApplicationModule.class);

		assertThat(declared).isNotNull();
		assertThat(declared.allowedDependencies()).isEmpty();
	}

	@Test
	void writesModuleDocumentation() {
		new Documenter(MODULES).writeModulesAsPlantUml().writeIndividualModulesAsPlantUml();
	}

}
