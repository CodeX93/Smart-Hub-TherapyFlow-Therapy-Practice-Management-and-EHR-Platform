export type ProfileSectionHandle<TValues> = {
  getValues: () => TValues;
  trigger: () => Promise<boolean>;
  reset: (values?: TValues) => void;
  isDirty: () => boolean;
  focusError?: () => void;
};

/**
 * A Schedule section also reports whether the timezone was explicitly chosen.
 *
 * The picker always shows an effective zone, but a therapist who never picked one
 * follows the clinic, and that link only survives while the profile stores no
 * timezone of its own. So the saver needs to tell "the admin chose this" apart from
 * "this is just the clinic default on display", and only persist the former.
 */
export type ScheduleSectionHandle<TValues> = ProfileSectionHandle<TValues> & {
  isTimezoneExplicitlyChosen: () => boolean;
};
