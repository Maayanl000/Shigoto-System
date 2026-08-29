import test from 'node:test';
import assert from 'node:assert/strict';
import { getKanbanDatePresentation, getKanbanStatusLabel } from './hrKanban.js';

test('manager scheduled after a completed technical interview displays the active manager label', () => {
  assert.equal(getKanbanStatusLabel({
    status: 'TECH_INTERVIEW_SCHEDULED',
    activeInterviewType: 'MANAGER',
  }), 'Manager interview scheduled');
});

test('technical interview status retains its fallback without an active scheduled interview', () => {
  assert.equal(getKanbanStatusLabel({ status: 'TECH_INTERVIEW_SCHEDULED' }),
    'Technical interview scheduled');
});

test('uses the status-changed label only for a real status transition timestamp', () => {
  const presentation = getKanbanDatePresentation({
    appliedAt: '2026-08-20T10:00:00',
    statusChangedAt: '2026-08-25T12:00:00',
  });
  assert.deepEqual(presentation, {
    label: 'Status changed',
    date: '2026-08-25T12:00:00',
  });
});

test('uses the applied label and date for a legacy application', () => {
  const presentation = getKanbanDatePresentation({
    appliedAt: '2026-08-20T10:00:00',
    statusChangedAt: null,
  });
  assert.deepEqual(presentation, {
    label: 'Applied',
    date: '2026-08-20T10:00:00',
  });
});
