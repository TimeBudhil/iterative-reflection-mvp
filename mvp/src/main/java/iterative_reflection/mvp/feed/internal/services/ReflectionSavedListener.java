package iterative_reflection.mvp.feed.internal.services;

import iterative_reflection.mvp.feed.internal.domain.AffinitySource;
import iterative_reflection.mvp.feed.internal.domain.AffinityProperties;
import iterative_reflection.mvp.feed.internal.domain.AffinityFormula;
import iterative_reflection.mvp.reflection.ReflectionSaved;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Plain synchronous listener: runs inside the reflection's transaction, so a failure rolls the reflection back. */
@Component
public class ReflectionSavedListener {

	private final FeedStateService feed;

	private final AffinityApplier affinity;

	private final AffinityProperties props;

	ReflectionSavedListener(FeedStateService feed, AffinityApplier affinity, AffinityProperties props) {
		this.feed = feed;
		this.affinity = affinity;
		this.props = props;
	}

	@EventListener
	void on(ReflectionSaved event) {
		feed.clear(event.studentId(), event.careerId());
		// replace (not add): ticket 11's edit re-publishes the event with the same reflection id
		affinity.replaceForCareer(event.studentId(), AffinitySource.REFLECTION, event.reflectionId(), event.careerId(),
				AffinityFormula.reflectionDelta(event.scaleMean(), props.neutralScore(), props.scale()));
	}

}
