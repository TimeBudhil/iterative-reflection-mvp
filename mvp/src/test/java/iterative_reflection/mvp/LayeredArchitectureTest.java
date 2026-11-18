package iterative_reflection.mvp;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Enforces the layer direction inside every module's {@code internal} package (ticket 20):
 * controller -> services -> repository -> domain; dto is shared by controller and services;
 * integration may use the core layers but nothing in the core layers uses integration.
 */
class LayeredArchitectureTest {

	private static final String BASE = "iterative_reflection.mvp";
	private static final String INTERNAL = BASE + "..internal.";

	static JavaClasses classes;

	@BeforeAll
	static void importClasses() {
		classes = new ClassFileImporter().withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS).importPackages(BASE);
	}

	@Test
	void layerDirectionIsRespectedInEveryModule() {
		layeredArchitecture().consideringOnlyDependenciesInLayers()
			.layer("controller").definedBy(INTERNAL + "controller..")
			.layer("services").definedBy(INTERNAL + "services..")
			.layer("repository").definedBy(INTERNAL + "repository..")
			.layer("domain").definedBy(INTERNAL + "domain..")
			.layer("dto").definedBy(INTERNAL + "dto..")
			.layer("integration").definedBy(INTERNAL + "integration..")
			.whereLayer("controller").mayNotBeAccessedByAnyLayer()
			.whereLayer("services").mayOnlyBeAccessedByLayers("controller", "integration")
			.whereLayer("repository").mayOnlyBeAccessedByLayers("services", "integration")
			.whereLayer("domain").mayOnlyBeAccessedByLayers("services", "repository", "integration", "dto")
			.whereLayer("dto").mayOnlyBeAccessedByLayers("controller", "services", "integration")
			.whereLayer("integration").mayNotBeAccessedByAnyLayer()
			.check(classes);
	}

	@Test
	void controllersNeverTouchRepositoriesOrDomainDirectly() {
		noClasses().that().resideInAPackage(INTERNAL + "controller..")
			.should().dependOnClassesThat().resideInAnyPackage(INTERNAL + "repository..", INTERNAL + "domain..")
			.check(classes);
	}

	@Test
	void noClassIsLooseInInternal() {
		List<String> loose = classes.stream().map(JavaClass::getPackageName)
			.filter(p -> p.endsWith(".internal")).distinct().toList();
		assertThat(loose).as("packages with classes directly in internal").isEmpty();
	}

}
