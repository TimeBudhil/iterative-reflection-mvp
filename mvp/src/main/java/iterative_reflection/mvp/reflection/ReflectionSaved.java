package iterative_reflection.mvp.reflection;

/**
 * Published inside the saving transaction (synchronously, via {@code ApplicationEventPublisher}) every time a
 * reflection is saved. Consumers must use a plain {@code @EventListener} so that their failure rolls the save back.
 *
 * @param scaleMean mean of the 1-10 scale answers of the saved version
 */
public record ReflectionSaved(long studentId, long careerId, double scaleMean, long reflectionId) {
}
