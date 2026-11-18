package iterative_reflection.mvp.admin.internal.controller;

import iterative_reflection.mvp.admin.internal.dto.PilotMetrics;
import iterative_reflection.mvp.admin.internal.services.PilotMetricsService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PilotMetricsController {

	private final PilotMetricsService metrics;

	PilotMetricsController(PilotMetricsService metrics) {
		this.metrics = metrics;
	}

	/** Optional {@code from}/{@code to} are inclusive UTC signup dates (yyyy-MM-dd). */
	@GetMapping("/api/admin/pilot-metrics")
	PilotMetrics get(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		if (from != null && to != null && from.isAfter(to)) {
			throw new ErrorResponseException(HttpStatus.BAD_REQUEST,
					ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "from must not be after to."), null);
		}
		return metrics.compute(from, to);
	}

}
