const YOUTUBE_REGEX =
  /(?:youtube\.com\/watch\?v=|youtu\.be\/|youtube\.com\/embed\/)([a-zA-Z0-9_-]{11})/;

const VIMEO_REGEX = /vimeo\.com\/(?:video\/)?(\d+)/;

const DIRECT_VIDEO_REGEX = /\.(mp4|webm|ogg|mov)(\?.*)?$/i;

export function extractYouTubeId(url: string): string | null {
  const match = url.trim().match(YOUTUBE_REGEX);
  return match?.[1] ?? null;
}

export function extractVimeoId(url: string): string | null {
  const match = url.trim().match(VIMEO_REGEX);
  return match?.[1] ?? null;
}

export function isDirectVideoUrl(url: string): boolean {
  return DIRECT_VIDEO_REGEX.test(url.trim());
}

export function isUrlOnlyText(text: string): boolean {
  return /^https?:\/\/\S+$/i.test(text.trim());
}

export type VideoKind = "youtube" | "vimeo" | "direct" | "link" | null;

export function detectVideo(url: string): {
  kind: VideoKind;
  youtubeId: string | null;
  vimeoId: string | null;
  embedUrl: string | null;
} {
  const trimmed = url.trim();
  if (!trimmed) {
    return { kind: null, youtubeId: null, vimeoId: null, embedUrl: null };
  }

  const youtubeId = extractYouTubeId(trimmed);
  if (youtubeId) {
    return {
      kind: "youtube",
      youtubeId,
      vimeoId: null,
      embedUrl: `https://www.youtube.com/embed/${youtubeId}`,
    };
  }

  const vimeoId = extractVimeoId(trimmed);
  if (vimeoId) {
    return {
      kind: "vimeo",
      youtubeId: null,
      vimeoId,
      embedUrl: `https://player.vimeo.com/video/${vimeoId}`,
    };
  }

  if (isDirectVideoUrl(trimmed)) {
    return { kind: "direct", youtubeId: null, vimeoId: null, embedUrl: trimmed };
  }

  if (/^https?:\/\//i.test(trimmed)) {
    return { kind: "link", youtubeId: null, vimeoId: null, embedUrl: null };
  }

  return { kind: null, youtubeId: null, vimeoId: null, embedUrl: null };
}
