import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manual latency check (ticket 19, N1 / N6.1). NOT part of the Maven build or CI: run it through perf/run-perf.sh.
 * Seeds 300 students x 20 reflections (+ abandons, intakes, counselor links) straight into the running app's H2
 * file database, then drives the real HTTP API with real sessions and prints p95 per endpoint.
 * Args: baseUrl jdbcUrl [students] [threads]
 */
public class PerfCheck {

	static final String PREFIX = "perf";
	static String base;
	static final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
	static final Map<String, List<Long>> samples = new ConcurrentHashMap<>();
	static final Map<String, AtomicInteger> unexpected = new ConcurrentHashMap<>();

	record Session(String id, String csrf) {
	}

	record Res(int status, String body) {
	}

	public static void main(String[] args) throws Exception {
		base = args[0];
		String jdbc = args[1];
		int students = args.length > 2 ? Integer.parseInt(args[2]) : 300;
		int threads = args.length > 3 ? Integer.parseInt(args[3]) : 4;
		long t0 = System.currentTimeMillis();
		seed(jdbc, students);
		System.out.println("seeded in " + (System.currentTimeMillis() - t0) + " ms");

		signIn("admin@demo.example", "demo", "warm-up (not reported)");
		samples.remove("warm-up (not reported)");
		Session admin = signIn("admin@demo.example", "demo", "POST /api/auth/signin (admin)");
		Session counselor = signIn("counselor@demo.example", "demo", "POST /api/auth/signin (counselor)");
		List<Session> st = new ArrayList<>();
		for (int i = 1; i <= students; i++) {
			st.add(signIn(email(i), "demo", "POST /api/auth/signin (student)"));
		}
		List<Long> studentIds = new ArrayList<>();
		try (Connection c = DriverManager.getConnection(jdbc, "sa", ""); Statement s = c.createStatement();
				ResultSet r = s.executeQuery("select id from users where email like '" + PREFIX
						+ "%@perf.example' order by id")) {
			while (r.next()) {
				studentIds.add(r.getLong(1));
			}
		}
		ExecutorService pool = Executors.newFixedThreadPool(threads);

		// 1. feed/next: first call picks and stores a card, later calls re-read it
		long[] cardId = new long[students];
		int[] cardPages = new int[students];
		each(pool, students, i -> {
			Res r = get(st.get(i), "/api/feed/next", "GET /api/feed/next (first call, picks card)", 200);
			cardId[i] = num(r.body, "\"career\":\\{\"id\":(\\d+)");
			cardPages[i] = (int) num(r.body, "\"lastPage\":(\\d+)");
		});
		for (int round = 0; round < 3; round++) {
			each(pool, students, i -> get(st.get(i), "/api/feed/next", "GET /api/feed/next (card stored)", 200));
		}

		// 2. student reads
		for (int round = 0; round < 2; round++) {
			each(pool, students, i -> {
				Session s = st.get(i);
				get(s, "/api/me", "GET /api/me", 200);
				get(s, "/api/reflection-questions", "GET /api/reflection-questions", 200);
				get(s, "/api/students/me/reflections", "GET /api/students/me/reflections (page of 20)", 200);
				get(s, "/api/students/me/reflections?size=100", "GET /api/students/me/reflections (size 100)", 200);
				get(s, "/api/students/me/counselors", "GET /api/students/me/counselors", 200);
				get(s, "/api/intake/questions", "GET /api/intake/questions", 200);
				get(s, "/api/intake", "GET /api/intake", 200, 404);
				get(s, "/api/careers/" + cardId[i] + "/experience", "GET /api/careers/{id}/experience", 200);
			});
		}
		each(pool, students, i -> {
			long careerId = firstReflectedCareer(jdbc, i);
			get(st.get(i), "/api/careers/" + careerId + "/reflection/versions",
					"GET /api/careers/{id}/reflection/versions", 200);
		});

		// 3. counselor and admin reads
		for (int round = 0; round < 20; round++) {
			get(counselor, "/api/counselor/me", "GET /api/counselor/me", 200);
			get(counselor, "/api/counselor/students", "GET /api/counselor/students (page of 20)", 200);
			get(counselor, "/api/counselor/students?size=100&page=1", "GET /api/counselor/students (size 100)", 200);
			get(counselor, "/api/counselor/students?page=14", "GET /api/counselor/students (last page)", 200);
		}
		each(pool, students, i -> get(counselor, "/api/counselor/students/" + studentIds.get(i),
				"GET /api/counselor/students/{id}", 200));
		for (int round = 0; round < 20; round++) {
			get(admin, "/api/admin/pilot-metrics", "GET /api/admin/pilot-metrics", 200);
			get(admin, "/api/admin/audit", "GET /api/admin/audit", 200);
			get(admin, "/api/admin/counselors", "GET /api/admin/counselors", 200);
		}

		// 4. writes: first third view the card, reflect and edit; second third view page 1 and abandon
		int third = Math.min(100, students / 3);
		each(pool, third, i -> {
			Session s = st.get(i);
			for (int p = 1; p <= cardPages[i]; p++) {
				post(s, "/api/careers/" + cardId[i] + "/pages/" + p + "/viewed", null,
						"POST /api/careers/{id}/pages/{n}/viewed", 200);
			}
			post(s, "/api/careers/" + cardId[i] + "/reflection", reflectionBody(i), "POST /api/careers/{id}/reflection",
					201);
			put(s, "/api/careers/" + cardId[i] + "/reflection", reflectionBody(i + 3), "PUT /api/careers/{id}/reflection",
					200);
			get(s, "/api/feed/next", "GET /api/feed/next (after reflecting)", 200);
		});
		each(pool, third, i -> {
			int k = third + i;
			Session s = st.get(k);
			post(s, "/api/careers/" + cardId[k] + "/pages/1/viewed", null, "POST /api/careers/{id}/pages/{n}/viewed", 200);
			post(s, "/api/careers/" + cardId[k] + "/abandon", null, "POST /api/careers/{id}/abandon", 204);
			get(s, "/api/feed/next", "GET /api/feed/next (after abandoning)", 200);
		});
		// students with an odd id have no intake yet
		each(pool, 40, i -> {
			Session s = st.get(students - 1 - 2 * i);
			post(s, "/api/intake", "{\"answers\":{\"goals\":{\"text\":\"Find a career that fits me\"}}}", "POST /api/intake",
					201, 409);
		});
		pool.shutdown();
		report(students, threads, System.currentTimeMillis() - t0);
	}

	// ---------- reporting ----------

	static void report(int students, int threads, long totalMs) {
		StringBuilder sb = new StringBuilder();
		sb.append("| Endpoint | n | p50 ms | p95 ms | max ms | target | result |\n|---|---|---|---|---|---|---|\n");
		boolean ok = true;
		for (Map.Entry<String, List<Long>> e : new TreeMap<>(samples).entrySet()) {
			List<Long> v = new ArrayList<>(e.getValue());
			Collections.sort(v);
			double p50 = v.get((int) Math.floor(0.50 * (v.size() - 1))) / 1e6;
			double p95 = v.get((int) Math.ceil(0.95 * v.size()) - 1) / 1e6;
			double max = v.get(v.size() - 1) / 1e6;
			boolean feed = e.getKey().startsWith("GET /api/feed/next");
			double target = feed ? 100 : 300;
			boolean pass = p95 < target;
			ok &= pass;
			sb.append(String.format("| %s | %d | %.1f | %.1f | %.1f | < %.0f | %s |%n", e.getKey(), v.size(), p50, p95,
					max, target, pass ? "PASS" : "FAIL"));
		}
		System.out.println();
		System.out.println(sb);
		unexpected.forEach((k, v) -> System.out.println("UNEXPECTED STATUS x" + v + ": " + k));
		System.out.printf("students=%d threads=%d total=%d s%n", students, threads, totalMs / 1000);
		boolean clean = unexpected.isEmpty();
		System.out.println(ok && clean ? "RESULT: PASS" : "RESULT: FAIL");
		System.exit(ok && clean ? 0 : 1);
	}

	// ---------- http ----------

	static String email(int i) {
		return PREFIX + i + "@perf.example";
	}

	static Session signIn(String email, String pw, String label) throws Exception {
		HttpResponse<String> h = client.send(HttpRequest.newBuilder(URI.create(base + "/actuator/health")).build(),
				HttpResponse.BodyHandlers.ofString());
		String csrf = h.headers().allValues("set-cookie").stream().filter(c -> c.startsWith("XSRF-TOKEN="))
				.map(c -> c.substring(11).split(";", 2)[0]).findFirst().orElseThrow();
		long t = System.nanoTime();
		HttpResponse<String> r = client.send(HttpRequest.newBuilder(URI.create(base + "/api/auth/signin"))
				.header("Content-Type", "application/json").header("Cookie", "XSRF-TOKEN=" + csrf)
				.header("X-XSRF-TOKEN", csrf)
				.POST(HttpRequest.BodyPublishers.ofString("{\"email\":\"" + email + "\",\"password\":\"" + pw + "\"}"))
				.build(), HttpResponse.BodyHandlers.ofString());
		record(label, System.nanoTime() - t);
		if (r.statusCode() != 200) {
			throw new IllegalStateException("signin failed " + r.statusCode() + " for " + email + ": " + r.body());
		}
		String sid = r.headers().allValues("set-cookie").stream().filter(c -> c.startsWith("SESSION="))
				.map(c -> c.substring(8).split(";", 2)[0]).findFirst().orElseThrow();
		return new Session(sid, csrf);
	}

	static Res get(Session s, String path, String label, int... ok) {
		return call("GET", s, path, null, label, ok);
	}

	static Res post(Session s, String path, String body, String label, int... ok) {
		return call("POST", s, path, body, label, ok);
	}

	static Res put(Session s, String path, String body, String label, int... ok) {
		return call("PUT", s, path, body, label, ok);
	}

	static Res call(String method, Session s, String path, String body, String label, int... ok) {
		try {
			HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path))
					.header("Cookie", "SESSION=" + s.id() + "; XSRF-TOKEN=" + s.csrf())
					.header("X-XSRF-TOKEN", s.csrf()).header("Accept", "application/json");
			if (body != null) {
				b.header("Content-Type", "application/json");
			}
			b.method(method,
					body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
			long t = System.nanoTime();
			HttpResponse<String> r = client.send(b.build(), HttpResponse.BodyHandlers.ofString());
			long dt = System.nanoTime() - t;
			boolean good = false;
			for (int o : ok) {
				good |= o == r.statusCode();
			}
			if (good) {
				record(label, dt);
			}
			else {
				unexpected.computeIfAbsent(label + " -> " + r.statusCode() + " " + trim(r.body()),
						k -> new AtomicInteger()).incrementAndGet();
			}
			return new Res(r.statusCode(), r.body());
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	static String trim(String s) {
		return s.length() > 160 ? s.substring(0, 160) : s;
	}

	static void record(String label, long nanos) {
		samples.computeIfAbsent(label, k -> Collections.synchronizedList(new ArrayList<>())).add(nanos);
	}

	static long num(String body, String regex) {
		Matcher m = Pattern.compile(regex).matcher(body);
		return m.find() ? Long.parseLong(m.group(1)) : 0;
	}

	static String reflectionBody(int seed) {
		String[] keys = { "interest", "values", "lifestyle", "responsibilities", "sacrifices" };
		StringBuilder sb = new StringBuilder("{\"answers\":{");
		for (int k = 0; k < keys.length; k++) {
			sb.append(k > 0 ? "," : "").append("\"").append(keys[k]).append("\":{\"score\":").append((seed + k) % 10 + 1)
					.append(",\"openText\":\"Because it matters to me.\"}");
		}
		return sb.append("},\"verdict\":\"").append(new String[] { "YES", "MAYBE", "NO" }[seed % 3]).append("\"}")
				.toString();
	}

	interface Task {
		void run(int i) throws Exception;
	}

	static void each(ExecutorService pool, int n, Task t) throws Exception {
		List<Future<?>> fs = new ArrayList<>();
		for (int i = 0; i < n; i++) {
			int idx = i;
			fs.add(pool.submit(() -> {
				try {
					t.run(idx);
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
			}));
		}
		for (Future<?> f : fs) {
			f.get(10, TimeUnit.MINUTES);
		}
	}

	static long firstReflectedCareer(String jdbc, int i) throws Exception {
		try (Connection c = DriverManager.getConnection(jdbc, "sa", ""); Statement s = c.createStatement();
				ResultSet r = s.executeQuery("select min(r.career_id) from reflections r join users u on u.id = r.student_id "
						+ "where u.email = '" + email(i + 1) + "'")) {
			r.next();
			return r.getLong(1);
		}
	}

	// ---------- seeding (plain SQL against the app's own schema) ----------

	static void seed(String jdbc, int students) throws Exception {
		try (Connection c = DriverManager.getConnection(jdbc, "sa", ""); Statement s = c.createStatement()) {
			ResultSet r = s.executeQuery("select count(*) from users where email like '" + PREFIX + "%@perf.example'");
			r.next();
			if (r.getInt(1) > 0) {
				System.out.println("perf data already present (" + r.getInt(1) + " students); reusing it");
				return;
			}
			c.setAutoCommit(false);
			String now = "CURRENT_TIMESTAMP";
			String like = "like '" + PREFIX + "%@perf.example'";
			String perf = "(select id from users where email " + like + ")";
			// every perf student reuses student1@demo.example's BCrypt hash, so their password is "demo"
			s.execute("insert into users (email, password_hash, display_name, role, active, must_change_password, demo, "
					+ "age_verified_at, accepted_terms_version, accepted_terms_at, created_at, version) "
					+ "select '" + PREFIX + "' || g.n || '@perf.example', "
					+ "(select password_hash from users where email = 'student1@demo.example'), "
					+ "'Perf Student ' || g.n, 'STUDENT', true, false, false, " + now + ", 'v1', " + now + ", " + now
					+ ", 0 from system_range(1, " + students + ") as g(n)");
			s.execute("insert into counselor_links (student_id, counselor_id, linked_at) select u.id, "
					+ "(select id from users where email = 'counselor@demo.example'), " + now
					+ " from users u where u.email " + like);
			// 20 reflections per student over the first 20 careers (1 version, 5 answers each)
			s.execute("insert into reflections (student_id, career_id, current_version, version, created_at, updated_at) "
					+ "select u.id, k.id, 1, 0, dateadd('MINUTE', -(u.id * 20 + k.default_order), " + now + "), "
					+ "dateadd('MINUTE', -(u.id * 20 + k.default_order), " + now + ") from users u, careers k "
					+ "where u.email " + like + " and k.default_order <= 20 and k.active");
			s.execute("insert into reflection_versions (reflection_id, version_number, verdict, created_at) "
					+ "select id, 1, case mod(id, 3) when 0 then 'YES' when 1 then 'MAYBE' else 'NO' end, created_at "
					+ "from reflections where student_id in " + perf);
			s.execute("insert into reflection_answers (reflection_version_id, question_id, score, open_text) "
					+ "select v.id, q.id, mod(v.id + q.id, 10) + 1, 'Because it matters to me.' "
					+ "from reflection_versions v join reflections r on r.id = v.reflection_id, reflection_questions q "
					+ "where r.student_id in " + perf + " and q.active");
			s.execute("insert into affinity_contributions (student_id, tag_id, source_type, source_id, delta) "
					+ "select r.student_id, ct.tag_id, 'REFLECTION', r.id, mod(r.id, 7) - 3 from reflections r "
					+ "join career_tags ct on ct.career_id = r.career_id where r.student_id in " + perf);
			s.execute("insert into page_progress (student_id, career_id, last_viewed_page, updated_at) "
					+ "select r.student_id, r.career_id, "
					+ "(select max(page_number) from career_pages p where p.career_id = r.career_id), " + now
					+ " from reflections r where r.student_id in " + perf);
			// two abandoned careers each (default order 21 and 22), partly viewed
			s.execute("insert into abandons (student_id, career_id, last_viewed_page, abandon_count, abandoned_at) "
					+ "select u.id, k.id, 1, 1, " + now + " from users u, careers k where u.email " + like
					+ " and k.default_order in (21, 22) and k.active");
			s.execute("insert into page_progress (student_id, career_id, last_viewed_page, updated_at) "
					+ "select a.student_id, a.career_id, 1, " + now + " from abandons a where a.student_id in " + perf);
			s.execute("insert into affinity_contributions (student_id, tag_id, source_type, source_id, delta) "
					+ "select a.student_id, ct.tag_id, 'ABANDON', a.id, -1 from abandons a "
					+ "join career_tags ct on ct.career_id = a.career_id where a.student_id in " + perf);
			// intake for every student with an even id
			s.execute("insert into intake_responses (student_id, created_at, updated_at) select id, " + now + ", " + now
					+ " from users where email " + like + " and mod(id, 2) = 0");
			s.execute("insert into intake_answers (response_id, question_id, text_value) select ir.id, q.id, "
					+ "case q.question_type when 'TEXT' then 'Something I care about' else null end "
					+ "from intake_responses ir, intake_questions q where ir.student_id in " + perf + " and q.active");
			s.execute("insert into intake_answer_tags (answer_id, tag_id) select a.id, t.id from intake_answers a "
					+ "join intake_questions q on q.id = a.question_id and q.question_type = 'TAGS' "
					+ "join intake_responses ir on ir.id = a.response_id, tags t where mod(t.id + ir.id, 10) = 0");
			s.execute("insert into affinity_contributions (student_id, tag_id, source_type, source_id, delta) "
					+ "select ir.student_id, at.tag_id, 'INTAKE', ir.id, 2 from intake_responses ir "
					+ "join intake_answers a on a.response_id = ir.id "
					+ "join intake_answer_tags at on at.answer_id = a.id where ir.student_id in " + perf);
			c.commit();
		}
	}

}
