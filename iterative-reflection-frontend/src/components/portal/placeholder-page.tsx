/** Stand-in for a Page map route until its feature ticket replaces it. */
export function PlaceholderPage({ title }: { title: string }) {
  return (
    <div>
      <h1 className="text-2xl font-semibold">{title}</h1>
    </div>
  );
}
