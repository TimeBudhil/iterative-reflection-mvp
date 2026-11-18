package iterative_reflection.mvp.identity.internal;

import jakarta.validation.constraints.NotBlank;

record SignInRequest(@NotBlank String email, @NotBlank String password) {
}
