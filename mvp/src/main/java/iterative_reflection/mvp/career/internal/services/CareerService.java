package iterative_reflection.mvp.career.internal.services;

import iterative_reflection.mvp.career.CareerSummary;
import iterative_reflection.mvp.career.internal.domain.CareerPage;
import iterative_reflection.mvp.career.internal.repository.JdbcCareerCatalog;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/** Use-case facade over the catalog for the career endpoints, so controllers never touch the repository layer. */
@Service
public class CareerService {

	private final JdbcCareerCatalog catalog;

	CareerService(JdbcCareerCatalog catalog) {
		this.catalog = catalog;
	}

	public Optional<CareerSummary> findCareer(long careerId) {
		return catalog.findCareer(careerId);
	}

	public int lastViewedPage(long studentId, long careerId) {
		return catalog.lastViewedPage(studentId, careerId);
	}

	public List<CareerPage> pages(long careerId) {
		return catalog.pages(careerId);
	}

	public int recordViewed(long studentId, long careerId, int page) {
		return catalog.recordViewed(studentId, careerId, page);
	}

}
