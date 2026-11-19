package iterative_reflection.mvp.identity.internal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
		@NotBlank @Size(max = 200) String currentPassword,
		@NotNull @Size(min = 12, max = 72, message = "must be between 12 and 72 characters") String newPassword) {
}
