package iterative_reflection.mvp.feed.internal.controller;

import iterative_reflection.mvp.career.CareerCatalog;
import iterative_reflection.mvp.career.CareerSummary;
import iterative_reflection.mvp.career.TagView;
import iterative_reflection.mvp.feed.FeedCard;
import iterative_reflection.mvp.feed.FeedService;
import iterative_reflection.mvp.identity.AppUserPrincipal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FeedController {

	private final FeedService feed;

	private final CareerCatalog catalog;

	FeedController(FeedService feed, CareerCatalog catalog) {
		this.feed = feed;
		this.catalog = catalog;
	}

	@GetMapping("/api/feed/next")
	NextResponse next(@AuthenticationPrincipal AppUserPrincipal user) {
		FeedCard card = feed.next(user.id());
		if (card.exhausted()) {
			return new NextResponse(null, true);
		}
		CareerSummary c = card.career();
		return new NextResponse(new CardView(c.id(), c.title(), c.summary(), c.estimatedMinutes(), c.tags(),
				c.lastPage(), catalog.lastViewedPage(user.id(), c.id())), false);
	}

	@PostMapping("/api/careers/{id}/abandon")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void abandon(@AuthenticationPrincipal AppUserPrincipal user, @PathVariable long id) {
		feed.abandon(user.id(), id);
	}

	record NextResponse(CardView career, boolean exhausted) {
	}

	record CardView(long id, String title, String summary, int estimatedMinutes, List<TagView> tags, int lastPage,
			int lastViewedPage) {
	}

}
