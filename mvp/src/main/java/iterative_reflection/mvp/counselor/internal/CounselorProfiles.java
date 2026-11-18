package iterative_reflection.mvp.counselor.internal;

import iterative_reflection.mvp.counselor.CounselorProfile;
import iterative_reflection.mvp.counselor.CounselorService;
import java.time.Clock;
import java.util.Optional;
import java.util.random.RandomGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CounselorProfiles implements CounselorService {

	/** 32 characters: no 0/O or 1/I (R2.1). */
	static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

	static final int CODE_LENGTH = 8;

	private final CounselorProfileRepository profiles;

	private final RandomGenerator random;

	private final Clock clock;

	CounselorProfiles(CounselorProfileRepository profiles, RandomGenerator random, Clock clock) {
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
