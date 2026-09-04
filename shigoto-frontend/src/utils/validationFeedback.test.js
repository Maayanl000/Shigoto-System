import test from 'node:test';
import assert from 'node:assert/strict';
import {
  homeTaskValidationMessage,
  interviewValidationMessage,
  jobSaveErrorMessage,
  registrationErrorMessage,
} from './validationFeedback.js';

test('surfaces safe registration validation while preserving duplicate email handling', () => {
  assert.equal(registrationErrorMessage({ response: { status: 400, data: { message: 'First name must contain letters only' } } }),
    'First name must contain letters only');
  assert.equal(registrationErrorMessage({ response: { status: 409 } }),
    'An account with this email already exists.');
  assert.equal(registrationErrorMessage({ response: { status: 500, data: { message: 'internal detail' } } }),
    'We could not create your account. Please try again.');
});

test('reports a clear stale job conflict', () => {
  assert.match(jobSaveErrorMessage({ response: { status: 409 } }), /updated by another user/i);
});

test('reports interview fields separately and rejects past times', () => {
  const now = new Date('2026-09-04T10:00:00');
  const base = { editing: false, type: 'TECHNICAL', interviewerId: '5', interviewTime: '2026-09-04T11:00:00', meetingLink: 'https://meet.example.com/x' };
  assert.equal(interviewValidationMessage({ ...base, interviewerId: '' }, now), 'Choose an interviewer.');
  assert.equal(interviewValidationMessage({ ...base, interviewTime: '2026-09-04T09:00:00' }, now),
    'The interview date and time must be in the future.');
  assert.equal(interviewValidationMessage({ ...base, meetingLink: '' }, now), 'Enter a meeting URL.');
  assert.equal(interviewValidationMessage(base, now), '');
});

test('home task validation requires its reviewer and a future deadline', () => {
  const now = new Date('2026-09-04T10:00:00');
  assert.match(homeTaskValidationMessage({ instructions: 'Task', reviewerId: '', deadline: '2026-09-05T10:00:00' }, now), /review/);
  assert.match(homeTaskValidationMessage({ instructions: 'Task', reviewerId: '5', deadline: '2026-09-03T10:00:00' }, now), /future/);
  assert.equal(homeTaskValidationMessage({ instructions: 'Task', reviewerId: '5', deadline: '2026-09-05T10:00:00' }, now), '');
});
