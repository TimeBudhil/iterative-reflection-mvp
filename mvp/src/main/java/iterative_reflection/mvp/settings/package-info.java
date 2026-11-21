/**
 * Settings: app-wide settings stored in one generic {@code app_settings} table. Public API:
 * {@link iterative_reflection.mvp.settings.ThemeSettings}. Depends on no other module (enforced by
 * {@code allowedDependencies = {}}); identity lookups such as display names belong to callers. Later settings are
 * new small typed services over the same table.
 */
@ApplicationModule(allowedDependencies = {})
package iterative_reflection.mvp.settings;

import org.springframework.modulith.ApplicationModule;

