export function isValidGithubProfile(value) {
  try {
    const url = new URL(value.trim());
    const validProtocol = url.protocol === 'http:' || url.protocol === 'https:';
    const validHost = url.hostname === 'github.com' || url.hostname === 'www.github.com';
    return validProtocol && validHost && url.pathname.split('/').some(Boolean);
  } catch {
    return false;
  }
}
