package iterative_reflection.mvp.feed.internal.services;

import iterative_reflection.mvp.career.CareerCatalog;
import iterative_reflection.mvp.career.TagView;
import iterative_reflection.mvp.feed.internal.domain.AffinitySource;
import iterative_reflection.mvp.feed.internal.domain.FeedRows.TagScore;
import iterative_reflection.mvp.feed.internal.repository.AffinityRepository;
import iterative_reflection.mvp.feed.TagAffinity;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The one place that writes {@code affinity_contributions}. Contributions are rows per (student, tag, source type,
 * source id); a tag's score is their sum. Replacing a source's contribution is "delete that source's rows, insert new
 * ones", so nothing is ever double counted. Writes require an enclosing transaction (the caller's listener runs inside
 * the reflection/intake/abandon transaction, so a failure rolls that back too) [R6.5].
 *
 * <p>
 * Tickets: 11 (reflection edit) calls {@link #replaceForCareer} again with the same source id, 12 uses source ABANDON
 * (source id = abandon id), 13 uses INTAKE with {@link #replace} (tags chosen in intake).
 */
@Component
public class AffinityApplier {

	private final AffinityRepository repository;

	private final CareerTags careerTags;

	private final CareerCatalog catalog;

	AffinityApplier(AffinityRepository repository, CareerTags careerTags, CareerCatalog catalog) {
		this.repository = repository;
		this.careerTags = careerTags;
		this.catalog = catalog;
	}

	/** Replaces the source's rows with {@code delta} on each tag of the career. */
	@Transactional(propagation = Propagation.MANDATORY)
	void replaceForCareer(long studentId, AffinitySource source, long sourceId, long careerId, double delta) {
		Map<Long, Double> perTag = new HashMap<>();
		careerTags.tagIds(careerId).forEach(t -> perTag.put(t, delta));
		replace(studentId, source, sourceId, perTag);
	}

	/** Replaces the source's rows with the given per-tag deltas (empty map = just remove). */
	@Transactional(propagation = Propagation.MANDATORY)
	void replace(long studentId, AffinitySource source, long sourceId, Map<Long, Double> deltaByTag) {
		remove(studentId, source, sourceId);
		deltaByTag.forEach((tag, delta) -> repository.insert(studentId, tag, source.name(), sourceId, delta));
	}

	@Transactional(propagation = Propagation.MANDATORY)
	void remove(long studentId, AffinitySource source, long sourceId) {
		repository.delete(studentId, source.name(), sourceId);
	}

	/** Summed affinity per tag id (tags with no contributions are absent). */
	public Map<Long, Double> scores(long studentId) {
		return repository.scores(studentId);
	}

	List<TagAffinity> top(long studentId, int n) {
		List<TagScore> scores = repository.top(studentId, n);
		Map<Long, TagView> tags = catalog.tagsByIds(scores.stream().map(TagScore::tagId).toList());
		return scores.stream().filter(s -> tags.containsKey(s.tagId()))
				.map(s -> new TagAffinity(s.tagId(), tags.get(s.tagId()).name(), tags.get(s.tagId()).category(),
						s.score()))
				.toList();
	}

}
