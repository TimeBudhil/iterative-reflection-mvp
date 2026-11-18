package iterative_reflection.mvp.platform.internal;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Renders every MVC error as RFC 7807 problem+json. Validation failures carry an {@code errors} array of
 * {@code {field, message}}; unexpected failures are logged and answered with a generic 500 that leaks no internals.
 */
@RestControllerAdvice
class ProblemDetailsAdvice extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ProblemDetailsAdvice.class);

	/** A single failing field in a validation problem. */
	record FieldViolation(String field, String message) {
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<FieldViolation> errors = ex.getBindingResult().getAllErrors().stream()
				.map(error -> new FieldViolation(
						error instanceof FieldError fieldError ? fieldError.getField() : error.getObjectName(),
						error.getDefaultMessage()))
				.toList();
		return validationProblem(errors, headers, status, request, ex);
	}

	@Override
	protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<FieldViolation> errors = ex.getParameterValidationResults().stream()
				.flatMap(result -> result.getResolvableErrors().stream()
						.map(error -> new FieldViolation(fieldName(result.getMethodParameter().getParameterName(), error),
								error.getDefaultMessage())))
				.toList();
		return validationProblem(errors, headers, HttpStatus.BAD_REQUEST, request, ex);
	}

	@ExceptionHandler(Exception.class)
	ProblemDetail handleUnexpected(Exception ex) throws Exception {
		if (ex instanceof AccessDeniedException || ex instanceof AuthenticationException) {
			throw ex; // let Spring Security answer 401/403 (also as problem+json)
		}
		log.error("Unhandled exception", ex);
		return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
	}

	private ResponseEntity<Object> validationProblem(List<FieldViolation> errors, HttpHeaders headers,
			HttpStatusCode status, WebRequest request, Exception ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, "Validation failed.");
		problem.setProperty("errors", errors);
		return handleExceptionInternal(ex, problem, headers, status, request);
	}

	private static String fieldName(String parameterName, MessageSourceResolvable error) {
		return error instanceof FieldError fieldError ? fieldError.getField() : parameterName;
	}

}
