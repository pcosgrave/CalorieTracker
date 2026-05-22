function toBase64Url(bytes: Uint8Array): string {
  return Buffer.from(bytes)
    .toString("base64")
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/u, "");
}

export async function createPkcePair(): Promise<{ verifier: string; challenge: string }> {
  const verifier = toBase64Url(crypto.getRandomValues(new Uint8Array(32)));
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(verifier));
  const challenge = toBase64Url(new Uint8Array(digest));
  return { verifier, challenge };
}

export function createOpaqueState(): string {
  return toBase64Url(crypto.getRandomValues(new Uint8Array(24)));
}
