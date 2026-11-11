/**
 * What each school role may see. Change access here and the whole API follows.
 *
 * Voice recordings, other-app usage and app locks never reach the server at all, so no
 * setting here can expose them: the phone doesn't send them (see SchoolSync.kt).
 */
export const VISIBILITY = {
  headmaster: { progress: true, minutes: true, skills: true, parentContact: true },
  teacher: { progress: true, minutes: true, skills: true, parentContact: false },
};
