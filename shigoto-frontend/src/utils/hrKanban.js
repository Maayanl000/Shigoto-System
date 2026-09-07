const statusLabels = {
  APPLIED: 'Applied',
  HR_INTERVIEW: 'HR interview',
  TASK_SENT: 'Task sent',
  TASK_SUBMITTED: 'Task submitted',
  TASK_APPROVED: 'Task approved',
  TECH_INTERVIEW_SCHEDULED: 'Technical interview scheduled',
  OFFER: 'Offer',
  HIRED: 'Hired',
  REJECTED: 'Rejected',
};

const activeStatuses = new Set([
  'APPLIED', 'HR_INTERVIEW', 'TASK_SENT', 'TASK_SUBMITTED', 'TASK_APPROVED',
  'TECH_INTERVIEW_SCHEDULED', 'OFFER',
]);

/**
 * Derives is active kanban status without mutating application state.
 */
export function isActiveKanbanStatus(status) {
  return activeStatuses.has(status);
}

/**
 * Derives get history group without mutating application state.
 */
export function getHistoryGroup(status) {
  if (status === 'HIRED') return 'hired';
  if (status === 'REJECTED') return 'rejected';
  return null;
}

const interviewLabels = {
  HR: 'HR interview scheduled',
  TECHNICAL: 'Technical interview scheduled',
  MANAGER: 'Manager interview scheduled',
};

/**
 * Builds the visible Kanban status, including the active interview type.
 */
export function getKanbanStatusLabel(application) {
  if (application.status === 'TECH_INTERVIEW_SCHEDULED' && application.activeInterviewType) {
    return interviewLabels[application.activeInterviewType] || statusLabels[application.status];
  }
  return statusLabels[application.status] || application.status;
}

/**
 * Selects the workflow-relevant date label and value for a Kanban card.
 */
export function getKanbanDatePresentation(application) {
  if (application.statusChangedAt) {
    return { label: 'Status changed', date: application.statusChangedAt };
  }
  return { label: 'Applied', date: application.appliedAt };
}
