import type { CatalogKind } from "../types";

type FoodTypeIconProps = {
  kind: CatalogKind;
};

export function FoodTypeIcon({ kind }: FoodTypeIconProps) {
  if (kind === "recipe") {
    return (
      <svg aria-hidden="true" viewBox="0 0 24 24">
        <path d="M5 12h12a5 5 0 0 1-5 5h-2a5 5 0 0 1-5-5Z" />
        <path d="M4 12h14" />
        <path d="M18 5v12" />
        <path d="M16 5h4" />
      </svg>
    );
  }

  return (
    <svg aria-hidden="true" viewBox="0 0 24 24">
      <path d="M6 20 18 8" />
      <path d="m14 4 6 6" />
      <path d="M4 18 6 20 3 21Z" />
    </svg>
  );
}
