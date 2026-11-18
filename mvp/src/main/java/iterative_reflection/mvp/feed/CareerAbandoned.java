package iterative_reflection.mvp.feed;

/** Published synchronously inside the abandon transaction, after the penalty is written. */
public record CareerAbandoned(long studentId, long careerId) {
}
