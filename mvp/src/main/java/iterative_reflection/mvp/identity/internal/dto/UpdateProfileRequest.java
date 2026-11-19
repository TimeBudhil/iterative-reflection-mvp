package iterative_reflection.mvp.identity.internal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Only the display name is editable; any other property (email, role, ...) is ignored. */
public record UpdateProfileRequest(@NotBlank @Size(max = 100) String displayName) {
}
