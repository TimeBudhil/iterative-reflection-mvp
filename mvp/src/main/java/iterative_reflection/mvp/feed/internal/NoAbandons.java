package iterative_reflection.mvp.feed.internal;

import org.springframework.stereotype.Component;

/** Placeholder until ticket 12 adds abandons. */
@Component
class NoAbandons implements AbandonCounter {

	@Override
	public long abandons(long studentId) {
		return 0;
	}

}
