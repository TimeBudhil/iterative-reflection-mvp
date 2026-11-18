package iterative_reflection.mvp.admin.internal.integration;

import iterative_reflection.mvp.admin.internal.dto.PilotMetrics;
import iterative_reflection.mvp.admin.internal.services.PilotMetricsService;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import java.util.function.ToDoubleFunction;
import org.springframework.stereotype.Component;

/** All-time pilot metrics as Prometheus gauges [N5.3]; values come from the TTL-cached service. */
@Component
public class PilotMetricsGauges implements MeterBinder {

	private final PilotMetricsService service;

	PilotMetricsGauges(PilotMetricsService service) {
		this.service = service;
	}

	@Override
	public void bindTo(MeterRegistry registry) {
		gauge(registry, "pilot.students", "Active non-demo students", m -> m.totalStudents());
		gauge(registry, "pilot.students.with.three.reflections", "Students with at least 3 reflections",
				m -> m.studentsWithThreeReflections());
		gauge(registry, "pilot.students.with.intake", "Students who completed intake", m -> m.studentsWithIntake());
		gauge(registry, "pilot.percent.with.three.reflections", "Percent of students with at least 3 reflections",
				m -> m.percentWithThreeReflections());
		gauge(registry, "pilot.percent.intake.completed", "Percent of students who completed intake",
				m -> m.percentIntakeCompleted());
		verdict(registry, "YES", m -> m.verdicts().YES());
		verdict(registry, "MAYBE", m -> m.verdicts().MAYBE());
		verdict(registry, "NO", m -> m.verdicts().NO());
	}

	private void gauge(MeterRegistry registry, String name, String description, ToDoubleFunction<PilotMetrics> f) {
		Gauge.builder(name, service, s -> f.applyAsDouble(s.allTimeCached())).description(description)
				.register(registry);
	}

	private void verdict(MeterRegistry registry, String verdict, ToDoubleFunction<PilotMetrics> f) {
		Gauge.builder("pilot.verdicts", service, s -> f.applyAsDouble(s.allTimeCached()))
				.description("Current reflection verdicts of pilot students").tag("verdict", verdict)
				.register(registry);
	}

}
