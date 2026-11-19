package iterative_reflection.mvp;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/** Enforces module boundaries: no cycles, and no access to another module's {@code internal} package [N4.1, N4.2]. */
class ModularityTest {

	static final ApplicationModules MODULES = ApplicationModules.of(MvpApplication.class);

	@Test
	void verifiesModuleBoundaries() {
		MODULES.verify();
	}

	@Test
	void writesModuleDocumentation() {
		new Documenter(MODULES).writeModulesAsPlantUml().writeIndividualModulesAsPlantUml();
	}

}
