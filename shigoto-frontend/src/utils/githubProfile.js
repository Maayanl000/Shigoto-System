export function isValidGithubProfile(value) {
  try {
    const url = new URL(value.trim());
    const validProtocol = url.protocol === 'http:' || url.protocol === 'https:';
    const validHost = url.hostname === 'github.com' || url.hostname === 'www.github.com';
    const segments = url.pathname.split('/').filter(Boolean);
    const validUsername = segments.length === 1
      && /^[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*$/.test(segments[0])
      && segments[0].length <= 39;
    return validProtocol && validHost && !url.port && !url.username && !url.password && validUsername;
  } catch {
    return false;
  }
}
