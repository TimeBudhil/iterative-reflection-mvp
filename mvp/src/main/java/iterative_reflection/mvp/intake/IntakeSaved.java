package iterative_reflection.mvp.intake;

import java.util.Set;

/**
 * Published inside the saving transaction (synchronously, via {@code ApplicationEventPublisher}) every time an
 * intake is created or edited; an edit re-publishes with the same {@code intakeResponseId}. Consumers must use a
 * plain {@code @EventListener} so that their failure rolls the save back.
 *
 * @param interestTagIds every tag selected in any TAGS question (the complete current set, possibly empty)
 */
public record IntakeSaved(long studentId, long intakeResponseId, Set<Long> interestTagIds) {
}
