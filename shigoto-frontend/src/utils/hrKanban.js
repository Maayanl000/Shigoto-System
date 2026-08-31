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

export function isActiveKanbanStatus(status) {
  return activeStatuses.has(status);
}

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

export function getKanbanStatusLabel(application) {
  if (application.status === 'TECH_INTERVIEW_SCHEDULED' && application.activeInterviewType) {
    return interviewLabels[application.activeInterviewType] || statusLabels[application.status];
  }
  return statusLabels[application.status] || application.status;
}

export function getKanbanDatePresentation(application) {
  if (application.statusChangedAt) {
    return { label: 'Status changed', date: application.statusChangedAt };
  }
  return { label: 'Applied', date: application.appliedAt };
}
