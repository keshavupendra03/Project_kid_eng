const encoder = new TextEncoder();

export const toHex = (bytes) => [...new Uint8Array(bytes)].map((b) => b.toString(16).padStart(2, "0")).join("");

export function randomHex(byteCount) {
  return toHex(crypto.getRandomValues(new Uint8Array(byteCount)));
}

export async function sha256(text) {
  return toHex(await crypto.subtle.digest("SHA-256", encoder.encode(text)));
}

// 100,000 iterations is the most Cloudflare Workers allows for PBKDF2.
export async function hashPassword(password, salt) {
  const key = await crypto.subtle.importKey("raw", encoder.encode(password), "PBKDF2", false, ["deriveBits"]);
  const bits = await crypto.subtle.deriveBits(
    { name: "PBKDF2", hash: "SHA-256", salt: encoder.encode(salt), iterations: 100_000 },
    key,
    256,
  );
  return toHex(bits);
}

/** Compares secrets without leaking, through timing, how many characters matched. */
export function sameSecret(a, b) {
  if (a.length !== b.length) return false;
  let difference = 0;
  for (let i = 0; i < a.length; i++) difference |= a.charCodeAt(i) ^ b.charCodeAt(i);
  return difference === 0;
}

// No 0/O or 1/I, so a code on a paper slip can't be misread.
const CODE_LETTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

export function newJoinCode() {
  return [...crypto.getRandomValues(new Uint8Array(8))].map((b) => CODE_LETTERS[b % CODE_LETTERS.length]).join("");
}
