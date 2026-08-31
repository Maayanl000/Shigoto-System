export const recruitmentStages = ['Applied', 'Review', 'Task', 'Interview', 'Decision'];

const statusDisplay = {
  APPLIED: { label: 'Applied', progressStage: 1, color: 'secondary' },
  HR_INTERVIEW: { label: 'HR interview', progressStage: 2, color: 'secondary' },
  TASK_SENT: { label: 'Task sent', progressStage: 3, color: 'secondary' },
  TASK_SUBMITTED: { label: 'Task submitted', progressStage: 3, color: 'secondary' },
  TASK_APPROVED: { label: 'Task approved', progressStage: 3, color: 'secondary' },
  TECH_INTERVIEW_SCHEDULED: { label: 'Technical interview scheduled', progressStage: 4, color: 'secondary' },
  OFFER: { label: 'Offer', progressStage: 5, color: 'success' },
  HIRED: { label: 'Hired', progressStage: 5, color: 'success' },
  REJECTED: { label: 'Rejected', progressStage: 5, color: 'error' },
};

export function getApplicationStatusDisplay(status) {
  return statusDisplay[status] || { label: status || 'Unknown status', progressStage: 1, color: 'default' };
}
