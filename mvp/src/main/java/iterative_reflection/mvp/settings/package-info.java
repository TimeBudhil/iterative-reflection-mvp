/**
 * Settings: app-wide settings stored in one generic {@code app_settings} table. Public API:
 * {@link iterative_reflection.mvp.settings.ThemeSettings}. Depends on no other feature module (identity lookups such
 * as display names belong to callers); later settings are new small typed services over the same table.
 */
package iterative_reflection.mvp.settings;
