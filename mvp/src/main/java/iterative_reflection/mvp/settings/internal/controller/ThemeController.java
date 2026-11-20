package iterative_reflection.mvp.settings.internal.controller;

import iterative_reflection.mvp.settings.ThemeSettings;
import iterative_reflection.mvp.settings.internal.dto.ThemeResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** The public theme read: every page, signed in or out, applies this preset. */
@RestController
public class ThemeController {

	private final ThemeSettings theme;

	ThemeController(ThemeSettings theme) {
		this.theme = theme;
	}

	@GetMapping("/api/settings/theme")
	ResponseEntity<ThemeResponse> theme() {
		return ResponseEntity.ok().cacheControl(CacheControl.noCache())
				.body(new ThemeResponse(theme.currentPresetId()));
	}

}
