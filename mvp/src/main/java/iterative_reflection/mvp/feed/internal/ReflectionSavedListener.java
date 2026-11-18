package iterative_reflection.mvp.feed.internal;

import iterative_reflection.mvp.reflection.ReflectionSaved;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Plain synchronous listener: runs inside the reflection's transaction, so a failure rolls the reflection back. */
@Component
class ReflectionSavedListener {

	private final FeedStateService feed;

	ReflectionSavedListener(FeedStateService feed) {
		this.feed = feed;
	}

	@EventListener
	void on(ReflectionSaved event) {
		feed.clear(event.studentId(), event.careerId());
	}

}
