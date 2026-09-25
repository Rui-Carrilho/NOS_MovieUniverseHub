package com.movieuniverse.hub.rating;

/** Pure policy: no Spring, database, network, clock or UI dependency. */
public final class CombinedScoreCalculator {
    public static final double BASELINE = 6.0;
    public static final double PRIOR_WEIGHT = 100.0;

    public record Score(Double value, long actualVotes, String explanation) {}

    public Score calculate(Double tmdbAverage, long tmdbVotes, long localSum, long localVotes) {
        if (tmdbVotes < 0 || localVotes < 0 || localSum < 0
                || (localVotes == 0 && localSum != 0)
                || (localVotes > 0 && (localSum < localVotes || (double) localSum > 10.0 * localVotes))) {
            throw new IllegalArgumentException("Invalid vote counts or local sum");
        }
        if (tmdbVotes > 0 && (tmdbAverage == null || !Double.isFinite(tmdbAverage)
                || tmdbAverage < 0 || tmdbAverage > 10)) {
            throw new IllegalArgumentException("A valid TMDB average is required when TMDB has votes");
        }
        long actualVotes = Math.addExact(tmdbVotes, localVotes);
        if (actualVotes == 0) return new Score(null, 0, "Não há informação suficiente: ainda não existem votos.");
        double externalSum = tmdbVotes == 0 ? 0.0 : tmdbAverage * tmdbVotes;
        double value = (externalSum + localSum + PRIOR_WEIGHT * BASELINE) / (actualVotes + PRIOR_WEIGHT);
        return new Score(value, actualVotes,
                "Média ponderada pelos votos, aproximada de uma referência de 6,0 com peso 100 quando há poucos votos. "
                + "Esse peso é uma escolha da aplicação e não representa votos reais.");
    }
}
