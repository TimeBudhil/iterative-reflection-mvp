/**
 * Career: the read-only catalog (careers, pages, content blocks, tag taxonomy) and per-student page progress.
 * Public API: {@link iterative_reflection.mvp.career.CareerCatalog}, {@link iterative_reflection.mvp.career.CareerSummary}
 * and {@link iterative_reflection.mvp.career.TagView}. Content arrives only through Flyway migrations and is validated
 * after every migrate run (see {@code internal.CatalogValidationCallback}).
 */
package iterative_reflection.mvp.career;
