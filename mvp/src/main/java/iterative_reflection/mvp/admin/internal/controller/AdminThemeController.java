package iterative_reflection.mvp.admin.internal.controller;

import iterative_reflection.mvp.identity.AppUserPrincipal;
import iterative_reflection.mvp.identity.UserService;
import iterative_reflection.mvp.identity.UserSummary;
import iterative_reflection.mvp.settings.SavedTheme;
import iterative_reflection.mvp.settings.ThemeSettings;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin read and save of the app theme. Lives in {@code admin} so the {@code settings} module needs no identity
 * lookup: this combines {@link ThemeSettings} with the display name from {@code identity}.
 */
@RestController
public class AdminThemeController {

	record ThemeRequest(
			@NotNull @Pattern(regexp = "grove|harbor|plum|slate", message = "must be one of: grove, harbor, plum, slate") String presetId) {
	}

	record AdminThemeResponse(String presetId, Instant updatedAt, String updatedByName) {
	}

	private final ThemeSettings theme;

	private final UserService users;

	AdminThemeController(ThemeSettings theme, UserService users) {
		this.theme = theme;
		this.users = users;
	}

	@GetMapping("/api/admin/settings/theme")
	AdminThemeResponse get() {
		return respond(theme.saved());
	}

	@PutMapping("/api/admin/settings/theme")
	AdminThemeResponse put(@Valid @RequestBody ThemeRequest request, @AuthenticationPrincipal AppUserPrincipal me) {
		return respond(theme.save(request.presetId(), me.id()));
	}

	private AdminThemeResponse respond(SavedTheme saved) {
		String name = saved.updatedBy() == null ? null
				: users.findById(saved.updatedBy()).map(UserSummary::displayName).orElse(null);
		return new AdminThemeResponse(saved.presetId(), saved.updatedAt(), name);
	}

}
