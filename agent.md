# Agent instructions

## Small UI changes

For localized UI requests such as:

- moving controls
- changing icons
- changing labels
- spacing/alignment
- colors
- showing/hiding existing controls

Treat the request as a surgical edit.

1. Locate the screen/component directly.
2. Read only that file and immediately required shared components/theme definitions.
3. Do not explore unrelated architecture.
4. Do not inspect repositories broadly to understand patterns that are already obvious from the affected code.
5. Make the smallest change satisfying the request.
6. Run the smallest useful compile/test validation.
7. If compilation succeeds, stop.

Do not repeatedly reread unchanged files for manual correctness validation. Do not run broad test suites for purely visual/local layout changes unless the affected code requires it.

## BiteWise Product Direction

Before making architectural or UX changes involving food data, meals, recipes,
ingredients, leftovers, pantry, household sharing, notifications, grocery
features, or AI recommendations, read:

`docs/Future_Features.md`

This document describes planned product direction and should inform data-model
decisions even when the feature being described is not yet implemented.

Do not implement future features merely because they appear in that document.
Only implement them when explicitly requested. However, avoid architectural
decisions that unnecessarily conflict with the documented direction.