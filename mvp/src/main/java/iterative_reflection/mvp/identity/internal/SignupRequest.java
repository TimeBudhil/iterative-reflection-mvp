package iterative_reflection.mvp.identity.internal;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Signup payload. Unknown properties (e.g. {@code role}) are ignored. {@code counselorCode} arrives in ticket 06. */
record SignupRequest(
		@NotBlank @Email @Size(max = 254) String email,
		@NotNull @Size(min = 12, max = 72, message = "must be between 12 and 72 characters") String password,
		@NotBlank @Size(max = 100) String displayName,
		@NotNull @Past LocalDate birthdate,
		@NotBlank @Size(max = 50) String acceptedTermsVersion) {
}
