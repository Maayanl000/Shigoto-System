import test from 'node:test';
import assert from 'node:assert/strict';
import { hasDisplayValue } from './displayValue.js';

test('identifies absent optional display values', () => {
  assert.equal(hasDisplayValue(null), false);
  assert.equal(hasDisplayValue(undefined), false);
  assert.equal(hasDisplayValue(''), false);
  assert.equal(hasDisplayValue('   '), false);
});

test('preserves meaningful falsey and textual values', () => {
  assert.equal(hasDisplayValue(false), true);
  assert.equal(hasDisplayValue(0), true);
  assert.equal(hasDisplayValue('Developer'), true);
});
