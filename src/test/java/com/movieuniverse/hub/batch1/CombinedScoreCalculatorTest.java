package com.movieuniverse.hub.batch1;

import com.movieuniverse.hub.rating.CombinedScoreCalculator;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class CombinedScoreCalculatorTest {
    private final CombinedScoreCalculator calculator = new CombinedScoreCalculator();

    @Test void zeroVotesMeansNoScoreNotThePrior() {
        var score = calculator.calculate(null, 0, 0, 0);
        assertThat(score.value()).isNull();
        assertThat(score.actualVotes()).isZero();
    }
    @Test void lowVoteMovieStaysBelowEstablishedMovieEvenAfterThreeTens() {
        double established = calculator.calculate(8.4, 30000, 0, 0).value();
        assertThat(calculator.calculate(8.9, 12, 0, 0).value()).isLessThan(established);
        var low = calculator.calculate(8.9, 12, 30, 3);
        assertThat(low.value()).isLessThan(established);
        assertThat(low.value()).isCloseTo(6.40695652173913, within(0.000000001));
        assertThat(low.actualVotes()).isEqualTo(15); // Never include prior weight as votes.
    }
    @Test void localOnlyVotesProduceAScore() {
        assertThat(calculator.calculate(null, 0, 10, 1).value()).isCloseTo(610.0 / 101, within(0.000000001));
    }
    @Test void boundsAndInvalidInputAreEnforced() {
        assertThat(calculator.calculate(10.0, 50000, 100, 10).value()).isBetween(0.0, 10.0);
        assertThatThrownBy(() -> calculator.calculate(null, 1, 0, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calculator.calculate(8.0, 1, 11, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calculator.calculate(8.0, 1, 1, 0)).isInstanceOf(IllegalArgumentException.class);
    }
}
