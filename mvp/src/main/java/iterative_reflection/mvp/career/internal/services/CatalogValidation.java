package iterative_reflection.mvp.career.internal.services;

import iterative_reflection.mvp.career.internal.domain.CatalogValidator;
import iterative_reflection.mvp.career.internal.repository.CatalogSnapshotRepository;
import java.util.List;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.stereotype.Service;

/** Validates the stored catalog over any connection (so it also runs from the Flyway callback). */
@Service
public class CatalogValidation {

	private final CatalogSnapshotRepository snapshots;

	private final CatalogValidator validator;

	CatalogValidation(CatalogSnapshotRepository snapshots, CatalogValidator validator) {
		this.snapshots = snapshots;
		this.validator = validator;
	}

	/** @return human-readable violations, empty when the catalog is valid */
	public List<String> validate(JdbcOperations jdbc) {
		return validator.validate(snapshots.load(jdbc));
	}

}
