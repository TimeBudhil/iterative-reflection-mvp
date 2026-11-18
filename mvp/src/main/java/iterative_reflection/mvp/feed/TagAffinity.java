package iterative_reflection.mvp.feed;

/** A student's summed affinity for one tag (sum of all contributions: reflections, intake, abandons). */
public record TagAffinity(long tagId, String tagName, String category, double score) {
}
