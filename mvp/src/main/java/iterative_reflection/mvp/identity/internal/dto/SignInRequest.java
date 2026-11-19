package iterative_reflection.mvp.identity.internal.dto;

import jakarta.validation.constraints.NotBlank;

public record SignInRequest(@NotBlank String email, @NotBlank String password) {
}
