/**
 * Cross-cutting platform concerns shared by every module: HTTP security baseline (actuator lockdown, CSRF,
 * problem+json 401/403), RFC 7807 error handling, request IDs and JSON logging, and the injectable
 * {@link java.time.Clock} and {@link java.util.random.RandomGenerator} beans. It exposes no API of its own; other
 * modules depend only on the JDK types it provides as beans.
 */
package iterative_reflection.mvp.platform;
