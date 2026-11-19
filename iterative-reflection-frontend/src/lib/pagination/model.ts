/** The backend's page envelope: `{content, page: {size, number, totalElements, totalPages}}`. `number` is 0-based. */
export type PageMeta = { size: number; number: number; totalElements: number; totalPages: number };
export type Paged<T> = { content: T[]; page: PageMeta };

export function pageSummary(meta: PageMeta) {
  const total = meta.totalElements;
  const from = total === 0 ? 0 : meta.number * meta.size + 1;
  const to = Math.min(total, (meta.number + 1) * meta.size);
  return {
    from,
    to,
    total,
    pageNumber: meta.number + 1,
    totalPages: Math.max(1, meta.totalPages),
    hasPrev: meta.number > 0,
    hasNext: meta.number + 1 < meta.totalPages,
  };
}
