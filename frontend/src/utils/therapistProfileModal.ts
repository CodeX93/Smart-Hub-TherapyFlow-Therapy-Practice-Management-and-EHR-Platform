/**
 * The Zoom setup lives in a tab of the profile modal, which the topbar owns and no route
 * points at. This lets any screen send the user straight to it without threading the
 * topbar's state through the tree.
 */
export const THERAPIST_PROFILE_OPEN_EVENT = "therapyflow:open-therapist-profile";

export type TherapistProfileSectionRequest = { section: string };

export function openTherapistProfileSection(section: string): void {
  window.dispatchEvent(
    new CustomEvent<TherapistProfileSectionRequest>(THERAPIST_PROFILE_OPEN_EVENT, {
      detail: { section },
    }),
  );
}

export function subscribeToTherapistProfileOpen(
  handler: (section: string) => void,
): () => void {
  const listener = (event: Event) => {
    const detail = (event as CustomEvent<TherapistProfileSectionRequest>).detail;
    handler(detail?.section || "basic");
  };
  window.addEventListener(THERAPIST_PROFILE_OPEN_EVENT, listener);
  return () => window.removeEventListener(THERAPIST_PROFILE_OPEN_EVENT, listener);
}
