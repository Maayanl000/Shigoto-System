export function registrationErrorMessage(error) {
  if (error.response?.status === 409) return 'An account with this email already exists.';
  if (error.response?.status === 400 && typeof error.response?.data?.message === 'string') {
    return error.response.data.message;
  }
  return 'We could not create your account. Please try again.';
}

export function jobSaveErrorMessage(error) {
  if (error.response?.status === 409) {
    return 'This job was updated by another user. The job list was refreshed; close this dialog, reopen the job, and try again.';
  }
  return error.response?.data?.message || 'Could not save the job.';
}

export function interviewValidationMessage({ editing, type, interviewerId, interviewTime, meetingLink }, now = new Date()) {
  if (!editing && !type) return 'Choose an interview type.';
  if (!interviewerId) return 'Choose an interviewer.';
  const scheduledAt = new Date(interviewTime);
  if (!interviewTime || Number.isNaN(scheduledAt.getTime())) return 'Choose an interview date and time.';
  if (scheduledAt <= now) return 'The interview date and time must be in the future.';
  if (!meetingLink.trim()) return 'Enter a meeting URL.';
  if (meetingLink.trim().length > 255) return 'Meeting URL must be at most 255 characters.';
  try {
    const url = new URL(meetingLink.trim());
    if (!['http:', 'https:'].includes(url.protocol)) throw new Error('invalid');
  } catch {
    return 'Enter a valid HTTP or HTTPS meeting URL.';
  }
  return '';
}

export function homeTaskValidationMessage({ instructions, reviewerId, deadline }, now = new Date()) {
  if (!instructions.trim()) return 'Enter task instructions before sending the home task.';
  if (!reviewerId) return 'Choose the interviewer who will review this home task.';
  const parsedDeadline = new Date(deadline);
  if (!deadline || Number.isNaN(parsedDeadline.getTime()) || parsedDeadline <= now) {
    return 'The home task deadline must be in the future.';
  }
  return '';
}
