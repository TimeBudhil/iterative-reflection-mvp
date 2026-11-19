import type { Block } from "@/lib/experience/model";

const SANDBOX = "allow-scripts allow-same-origin allow-presentation";

/** One content block. All urls come from the backend (https only); embeds are sandboxed, links never leak the opener (N2.4). */
export function BlockView({ block }: { block: Block }) {
  switch (block.type) {
    case "TEXT":
      return (
        <div className="space-y-3">
          {(block.text ?? "").split(/\n{2,}/).map((p, i) => (
            <p key={i} className="whitespace-pre-line">
              {p}
            </p>
          ))}
        </div>
      );
    case "VIDEO_EMBED":
    case "AUDIO_EMBED": {
      const video = block.type === "VIDEO_EMBED";
      return (
        <iframe
          src={block.url}
          title={block.label || (video ? "Video" : "Audio")}
          sandbox={SANDBOX}
          allow="encrypted-media; picture-in-picture"
          allowFullScreen={video}
          loading="lazy"
          referrerPolicy="strict-origin-when-cross-origin"
          className={video ? "aspect-video w-full rounded-md border" : "h-40 w-full rounded-md border"}
        />
      );
    }
    case "IMAGE_URL":
      return (
        // eslint-disable-next-line @next/next/no-img-element -- arbitrary https hosts, not optimisable
        <img src={block.url} alt={block.label ?? ""} className="max-w-full rounded-md" />
      );
    case "EXTERNAL_LINK":
      return (
        <a href={block.url} target="_blank" rel="noopener noreferrer" className="inline-block underline">
          {block.label || block.url}
        </a>
      );
    default:
      return null;
  }
}
