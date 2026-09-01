import test from 'node:test';
import assert from 'node:assert/strict';
import { isValidGithubProfile } from './githubProfile.js';

test('accepts GitHub profile URLs', () => {
  assert.equal(isValidGithubProfile('https://github.com/octocat'), true);
  assert.equal(isValidGithubProfile('https://www.github.com/octocat/'), true);
});

test('rejects repositories, wrong hosts, and missing usernames', () => {
  assert.equal(isValidGithubProfile('https://github.com/octocat/repo'), false);
  assert.equal(isValidGithubProfile('https://example.com/octocat'), false);
  assert.equal(isValidGithubProfile('https://github.com'), false);
});
