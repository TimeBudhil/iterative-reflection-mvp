package iterative_reflection.mvp.feed.internal;

import iterative_reflection.mvp.career.CareerCatalog;
import iterative_reflection.mvp.career.TagView;
import java.util.List;
import org.springframework.stereotype.Component;

/** Tag ids of a career via the public career API. */
@Component
class CareerTags {

	private final CareerCatalog catalog;

	CareerTags(CareerCatalog catalog) {
		this.catalog = catalog;
	}

	List<Long> tagIds(long careerId) {
		return catalog.findCareer(careerId).map(c -> c.tags().stream().map(TagView::id).toList()).orElse(List.of());
	}

}
