import { userEmailCookie, userNameCookie, userSubCookie } from "./cookies";

export type ClientAuthUser = {
  userSub: string;
  email?: string | undefined;
  name?: string | undefined;
};

function readCookie(name: string): string | undefined {
  if (typeof document === "undefined") {
    return undefined;
  }

  const prefix = `${name}=`;
  return document.cookie
    .split("; ")
    .find((entry) => entry.startsWith(prefix))
    ?.slice(prefix.length);
}

export function currentUserScope(): string {
  return readCookie(userSubCookie) || "guest";
}

export function currentClientAuthUser(): ClientAuthUser | null {
  const userSub = readCookie(userSubCookie);
  if (!userSub) {
    return null;
  }

  return {
    userSub,
    email: readCookie(userEmailCookie),
    name: readCookie(userNameCookie),
  };
}
