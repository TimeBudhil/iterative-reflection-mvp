package iterative_reflection.mvp.feed.internal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

/** Pure unit test of the affinity formula with the default constants (5.5, 4.5) [R6.2]. */
class AffinityFormulaTest {

	@Test
	void allTensGivePlusOne() {
		assertThat(AffinityFormula.reflectionDelta(10, 5.5, 4.5)).isEqualTo(1.0);
	}

	@Test
	void allOnesGiveMinusOne() {
		assertThat(AffinityFormula.reflectionDelta(1, 5.5, 4.5)).isEqualTo(-1.0);
	}

	@Test
	void neutralMeanGivesZero() {
		assertThat(AffinityFormula.reflectionDelta(5.5, 5.5, 4.5)).isEqualTo(0.0);
	}

	@Test
	void workedExample() {
		// mean of 8, 6, 7, 5, 9 = 7.0 -> (7.0 - 5.5) / 4.5 = 0.3333...
		assertThat(AffinityFormula.reflectionDelta(7.0, 5.5, 4.5)).isCloseTo(1.0 / 3.0, within(1e-12));
	}

	@Test
	void constantsAreConfigurable() {
		assertThat(AffinityFormula.reflectionDelta(10, 4, 3)).isEqualTo(2.0);
	}

}
