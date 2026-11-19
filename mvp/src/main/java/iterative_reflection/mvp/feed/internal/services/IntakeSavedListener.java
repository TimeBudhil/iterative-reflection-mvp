package iterative_reflection.mvp.feed.internal.services;

import iterative_reflection.mvp.feed.internal.domain.AffinitySource;
import iterative_reflection.mvp.feed.internal.domain.AffinityProperties;
import iterative_reflection.mvp.intake.IntakeSaved;
import java.util.HashMap;
import java.util.Map;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Plain synchronous listener: runs inside the intake's transaction, so a failure rolls the intake back [N3.2].
 * Replaces (not adds to) the intake's previous seeding, so editing never double counts [R7.4].
 */
@Component
public class IntakeSavedListener {

	private final AffinityApplier affinity;

	private final AffinityProperties props;

	IntakeSavedListener(AffinityApplier affinity, AffinityProperties props) {
		this.affinity = affinity;
		this.props = props;
	}

	@EventListener
	void on(IntakeSaved event) {
		Map<Long, Double> perTag = new HashMap<>();
		event.interestTagIds().forEach(t -> perTag.put(t, props.intakeBoost()));
		affinity.replace(event.studentId(), AffinitySource.INTAKE, event.intakeResponseId(), perTag);
	}

}
