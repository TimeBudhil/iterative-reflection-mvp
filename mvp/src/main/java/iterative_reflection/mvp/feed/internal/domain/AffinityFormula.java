package iterative_reflection.mvp.feed.internal.domain;

/** Pure affinity arithmetic [R6.2]. */
public final class AffinityFormula {

	private AffinityFormula() {
	}

	/** (mean of scale answers - neutral) / scale: all 10s give +1.0, all 1s give -1.0 with defaults 5.5 / 4.5. */
	public static double reflectionDelta(double scaleMean, double neutralScore, double scale) {
		return (scaleMean - neutralScore) / scale;
	}

}
