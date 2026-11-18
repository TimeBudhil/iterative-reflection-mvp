package iterative_reflection.mvp.platform.internal.integration;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** Writes RFC 7807 problem+json bodies outside Spring MVC (e.g. from security filters). */
@Component
public class ProblemResponseWriter {

	private final JsonMapper jsonMapper;

	ProblemResponseWriter(JsonMapper jsonMapper) {
		this.jsonMapper = jsonMapper;
	}

	void write(HttpServletResponse response, HttpStatus status, String detail) throws IOException {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		jsonMapper.writeValue(response.getOutputStream(), problem);
	}

}
