package iterative_reflection.mvp.counselor.internal.services;

import iterative_reflection.mvp.counselor.internal.domain.CounselorLinkEntity;
import iterative_reflection.mvp.counselor.internal.repository.CounselorProfileRepository;
import iterative_reflection.mvp.counselor.internal.domain.CounselorProfileEntity;
import iterative_reflection.mvp.counselor.internal.repository.CounselorLinkRepository;
import iterative_reflection.mvp.counselor.CounselorProfile;
import iterative_reflection.mvp.counselor.CounselorService;
import iterative_reflection.mvp.counselor.CounselorLink;
import iterative_reflection.mvp.counselor.CounselorOverview;
import iterative_reflection.mvp.identity.UserService;
import iterative_reflection.mvp.identity.UserSummary;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.random.RandomGenerator;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CounselorProfiles implements CounselorService {

	/** 32 characters: no 0/O or 1/I (R2.1). */
	static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

	static final int CODE_LENGTH = 8;

	private final CounselorProfileRepository profiles;

	private final RandomGenerator random;

	private final Clock clock;

	private final CounselorLinkRepository links;

	private final UserService users;

	private final AuditLog audit;

	CounselorProfiles(CounselorProfileRepository profiles, RandomGenerator random, Clock clock,
			CounselorLinkRepository links, UserService users, AuditLog audit) {
		this.audit = audit;
		this.links = links;
		this.users = users;
		this.profiles = profiles;
		this.random = random;
		this.clock = clock;
	}

	@Override
	@Transactional
	public CounselorProfile createProfile(long userId) {
		CounselorProfileEntity e = new CounselorProfileEntity();
		e.userId = userId;
		e.joinCode = newUniqueCode();
		e.createdAt = clock.instant();
		profiles.saveAndFlush(e);
		return toProfile(e);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<CounselorProfile> findByUserId(long userId) {
		return profiles.findById(userId).map(CounselorProfiles::toProfile);
	}

	@Override
	@Transactional
	public CounselorLink link(long studentId, String code) {
		String normalized = code.trim().toUpperCase(Locale.ROOT);
		CounselorProfileEntity profile = profiles.findByJoinCode(normalized).orElse(null);
		UserSummary counselor = profile == null ? null : users.findById(profile.userId).orElse(null);
		if (counselor == null || !counselor.active()) {
			throw problem(HttpStatus.NOT_FOUND, "Unknown join code.");
		}
		Instant now = clock.instant();
		CounselorLinkEntity link = links.findByStudentIdAndCounselorId(studentId, counselor.id()).orElse(null);
		if (link == null) {
			link = new CounselorLinkEntity();
			link.studentId = studentId;
			link.counselorId = counselor.id();
		}
		else if (link.revokedAt == null) {
			throw problem(HttpStatus.CONFLICT, "Already linked to this counselor.");
		}
		link.linkedAt = now;
		link.revokedAt = null;
		try {
			return toLink(links.saveAndFlush(link), counselor);
		}
		catch (DataIntegrityViolationException e) {
			throw problem(HttpStatus.CONFLICT, "Already linked to this counselor.");
		}
	}

	@Override
	@Transactional
	public void revoke(long studentId, long linkId) {
		CounselorLinkEntity link = links.findByIdAndStudentIdAndRevokedAtIsNull(linkId, studentId)
				.orElseThrow(() -> problem(HttpStatus.NOT_FOUND, "No such link."));
		link.revokedAt = clock.instant();
		links.save(link);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<CounselorLink> linksOf(long studentId, Pageable pageable) {
		return links.findByStudentIdAndRevokedAtIsNullOrderByIdAsc(studentId, pageable)
				.map(l -> toLink(l, users.findById(l.counselorId).orElseThrow()));
	}

	@Override
	@Transactional(readOnly = true)
	public boolean canSee(long counselorId, long studentId) {
		return links.existsByCounselorIdAndStudentIdAndRevokedAtIsNull(counselorId, studentId)
				&& users.findById(counselorId).map(UserSummary::active).orElse(false);
	}

	@Override
	public Page<iterative_reflection.mvp.counselor.AuditEntry> auditLog(Long studentId, Pageable pageable) {
		return audit.find(studentId, pageable);
	}

	@Override
	@Transactional
	public CounselorProfile rotateJoinCode(long counselorId) {
		CounselorProfileEntity e = profiles.findById(counselorId)
				.orElseThrow(() -> problem(HttpStatus.NOT_FOUND, "No counselor profile."));
		e.joinCode = newUniqueCode();
		return toProfile(profiles.saveAndFlush(e));
	}

	@Override
	@Transactional(readOnly = true)
	public java.util.Map<Long, CounselorOverview> overviews(java.util.Collection<Long> counselorIds) {
		java.util.Map<Long, CounselorOverview> result = new java.util.HashMap<>();
		profiles.findAllById(counselorIds).forEach(p -> result.put(p.userId,
				new CounselorOverview(p.joinCode, links.countByCounselorIdAndRevokedAtIsNull(p.userId))));
		return result;
	}

	private static CounselorLink toLink(CounselorLinkEntity l, UserSummary counselor) {
		return new CounselorLink(l.id, l.counselorId, counselor.displayName(), counselor.active(), l.linkedAt);
	}

	private static ErrorResponseException problem(HttpStatus status, String detail) {
		return new ErrorResponseException(status, ProblemDetail.forStatusAndDetail(status, detail), null);
	}

	/** Collisions are checked up front; the unique constraint backstops a concurrent race. */
	String newUniqueCode() {
		String code;
		do {
			StringBuilder sb = new StringBuilder(CODE_LENGTH);
			for (int i = 0; i < CODE_LENGTH; i++) {
				sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
			}
			code = sb.toString();
		}
		while (profiles.existsByJoinCode(code));
		return code;
	}

	private static CounselorProfile toProfile(CounselorProfileEntity e) {
		return new CounselorProfile(e.userId, e.joinCode);
	}

}
