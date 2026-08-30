import { useCallback, useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { Alert, Box, Button, Card, CardContent, Chip, CircularProgress, Dialog, DialogActions, DialogContent, DialogTitle, Grid, Link, MenuItem, Stack, TextField, Typography } from '@mui/material';
import DownloadRoundedIcon from '@mui/icons-material/DownloadRounded';
import GitHubIcon from '@mui/icons-material/GitHub';
import SaveRoundedIcon from '@mui/icons-material/SaveRounded';
import PageSkeleton from '../components/PageSkeleton';
import api from '../services/api';

const statusLabels = {
  APPLIED: 'Applied', HR_INTERVIEW: 'HR interview', TASK_SENT: 'Task sent',
  TASK_SUBMITTED: 'Task submitted', TASK_APPROVED: 'Task approved',
  TECH_INTERVIEW_SCHEDULED: 'Technical interview scheduled', OFFER: 'Offer', REJECTED: 'Rejected',
};

const employmentLabels = {
  FULL_TIME: 'Full time', PART_TIME: 'Part time', STUDENT: 'Student position', INTERNSHIP: 'Internship',
};

const interviewTypeLabels = { HR: 'HR interview', TECHNICAL: 'Technical interview', MANAGER: 'Manager interview' };

function display(value) {
  return value === null || value === undefined || value === '' ? 'Not provided' : value;
}

function formatDate(value) {
  if (!value) return 'Not provided';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? 'Not provided' : new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium', timeStyle: 'short',
  }).format(date);
}

function toLocalDateTimeInput(value) {
  if (!value) return '';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? ''
    : new Date(date.getTime() - date.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
}

function Detail({ label, value }) {
  return <Box><Typography variant="caption" color="text.secondary">{label}</Typography><Typography variant="body2" sx={{ mt: 0.25 }}>{display(value)}</Typography></Box>;
}

export default function CandidateDetails() {
  const { applicationId } = useParams();
  const [record, setRecord] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [notes, setNotes] = useState('');
  const [savingNotes, setSavingNotes] = useState(false);
  const [notesMessage, setNotesMessage] = useState('');
  const [cvError, setCvError] = useState('');
  const [downloadingCv, setDownloadingCv] = useState(false);
  const [statusBusy, setStatusBusy] = useState(false);
  const [statusMessage, setStatusMessage] = useState('');
  const [taskDeadline, setTaskDeadline] = useState('');
  const [taskInstructions, setTaskInstructions] = useState('');
  const [editingTaskDeadline, setEditingTaskDeadline] = useState(false);
  const [deadlineBusy, setDeadlineBusy] = useState(false);
  const [interviewers, setInterviewers] = useState([]);
  const [interviewersLoading, setInterviewersLoading] = useState(true);
  const [interviewersError, setInterviewersError] = useState('');
  const [interviews, setInterviews] = useState([]);
  const [interviewsLoading, setInterviewsLoading] = useState(true);
  const [interviewType, setInterviewType] = useState('');
  const [interviewerId, setInterviewerId] = useState('');
  const [interviewTime, setInterviewTime] = useState('');
  const [meetingLink, setMeetingLink] = useState('');
  const [interviewBusy, setInterviewBusy] = useState(false);
  const [interviewMessage, setInterviewMessage] = useState('');
  const [editingInterviewId, setEditingInterviewId] = useState(null);
  const [editingInterviewType, setEditingInterviewType] = useState('');
  const [rejectOpen, setRejectOpen] = useState(false);
  const [candidateFeedbackDraft, setCandidateFeedbackDraft] = useState('');
  const [candidateFeedbackBusy, setCandidateFeedbackBusy] = useState(false);
  const [candidateFeedbackMessage, setCandidateFeedbackMessage] = useState('');

  const statusActions = record ? {
    APPLIED: [{ label: 'Move to HR interview', status: 'HR_INTERVIEW' }, { label: 'Reject', status: 'REJECTED' }],
    HR_INTERVIEW: [{ label: 'Reject', status: 'REJECTED' }],
    TASK_SENT: [{ label: 'Reject', status: 'REJECTED' }],
    TASK_SUBMITTED: [{ label: 'Reject', status: 'REJECTED' }],
    TASK_APPROVED: [{ label: 'Reject', status: 'REJECTED' }],
    TECH_INTERVIEW_SCHEDULED: [{ label: 'Make offer', status: 'OFFER' }, { label: 'Reject', status: 'REJECTED' }],
  }[record.status] || [] : [];
  const schedulableTypes = record ? {
    APPLIED: ['HR'], TASK_APPROVED: ['TECHNICAL'], TECH_INTERVIEW_SCHEDULED: ['MANAGER'],
  }[record.status] || [] : [];
  const selectedInterviewType = schedulableTypes.includes(interviewType)
    ? interviewType : schedulableTypes[0] || '';
  const interviewGroups = [
    { label: 'Upcoming', status: 'SCHEDULED' },
    { label: 'Completed', status: 'COMPLETED' },
    { label: 'Canceled', status: 'CANCELED' },
  ];

  const loadRecord = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const response = await api.get(`/hr/applications/${applicationId}`);
      setRecord(response.data);
      setNotes(response.data.hrNotes || '');
      setCandidateFeedbackDraft(response.data.candidateFeedback || '');
    } catch (requestError) {
      setError(requestError.response?.status === 404
        ? 'This application was not found or is not available to your company.'
        : requestError.response?.data?.message || 'Could not load the candidate record.');
    } finally {
      setLoading(false);
    }
  }, [applicationId]);

  useEffect(() => {
    let active = true;
    api.get(`/hr/applications/${applicationId}`)
      .then((response) => {
        if (active) {
          setRecord(response.data);
          setNotes(response.data.hrNotes || '');
          setCandidateFeedbackDraft(response.data.candidateFeedback || '');
        }
      })
      .catch((requestError) => {
        if (active) setError(requestError.response?.status === 404
          ? 'This application was not found or is not available to your company.'
          : requestError.response?.data?.message || 'Could not load the candidate record.');
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => { active = false; };
  }, [applicationId]);

  useEffect(() => {
    let active = true;
    api.get('/hr/interviewers')
      .then((response) => { if (active) setInterviewers(Array.isArray(response.data) ? response.data : []); })
      .catch((requestError) => { if (active) setInterviewersError(requestError.response?.data?.message || 'Could not load interviewers.'); })
      .finally(() => { if (active) setInterviewersLoading(false); });
    return () => { active = false; };
  }, []);

  const loadInterviews = useCallback(async () => {
    setInterviewsLoading(true);
    try {
      const response = await api.get(`/hr/applications/${applicationId}/interviews`);
      setInterviews(Array.isArray(response.data) ? response.data : []);
    } catch {
      setInterviews([]);
    } finally {
      setInterviewsLoading(false);
    }
  }, [applicationId]);

  useEffect(() => {
    let active = true;
    api.get(`/hr/applications/${applicationId}/interviews`)
      .then((response) => { if (active) setInterviews(Array.isArray(response.data) ? response.data : []); })
      .catch(() => { if (active) setInterviews([]); })
      .finally(() => { if (active) setInterviewsLoading(false); });
    return () => { active = false; };
  }, [applicationId]);

  const downloadCv = async () => {
    setDownloadingCv(true);
    setCvError('');
    try {
      const response = await api.get(`/hr/applications/${applicationId}/cv`, { responseType: 'blob' });
      const url = URL.createObjectURL(response.data);
      const anchor = document.createElement('a');
      anchor.href = url;
      anchor.download = `cv-application-${applicationId}.pdf`;
      document.body.appendChild(anchor);
      anchor.click();
      anchor.remove();
      URL.revokeObjectURL(url);
    } catch (requestError) {
      setCvError(requestError.response?.data?.message || 'Could not download this CV.');
    } finally {
      setDownloadingCv(false);
    }
  };

  const saveNotes = async () => {
    setSavingNotes(true);
    setNotesMessage('');
    try {
      const response = await api.put(`/hr/applications/${applicationId}/notes`, { hrNotes: notes });
      setRecord(response.data);
      setNotes(response.data.hrNotes || '');
      setNotesMessage('Notes saved.');
    } catch (requestError) {
      setNotesMessage(requestError.response?.data?.message || 'Could not save HR notes.');
    } finally {
      setSavingNotes(false);
    }
  };

  const updateStatus = async (status) => {
    setStatusBusy(true);
    setStatusMessage('');
    try {
      const response = await api.put(`/hr/applications/${applicationId}/status`, { status });
      setRecord(response.data);
      setStatusMessage(`Status updated to ${statusLabels[response.data.status] || response.data.status}.`);
    } catch (requestError) {
      setStatusMessage(requestError.response?.data?.message || 'Could not update the application status.');
    } finally {
      setStatusBusy(false);
    }
  };

  const rejectCandidate = async () => {
    setCandidateFeedbackBusy(true);
    setStatusMessage('');
    try {
      const response = await api.put(`/hr/applications/${applicationId}/reject`, {
        candidateFeedback: candidateFeedbackDraft.trim() || null,
      });
      setRecord(response.data);
      setCandidateFeedbackDraft(response.data.candidateFeedback || '');
      setRejectOpen(false);
      setStatusMessage('Candidate rejected.');
    } catch (requestError) {
      setStatusMessage(requestError.response?.data?.message || 'Could not reject the candidate.');
    } finally {
      setCandidateFeedbackBusy(false);
    }
  };

  const saveCandidateFeedback = async () => {
    setCandidateFeedbackBusy(true);
    setCandidateFeedbackMessage('');
    try {
      const response = await api.put(`/hr/applications/${applicationId}/candidate-feedback`, {
        candidateFeedback: candidateFeedbackDraft.trim() || null,
      });
      setRecord(response.data);
      setCandidateFeedbackDraft(response.data.candidateFeedback || '');
      setCandidateFeedbackMessage('Candidate feedback saved.');
    } catch (requestError) {
      setCandidateFeedbackMessage(requestError.response?.data?.message || 'Could not save candidate feedback.');
    } finally {
      setCandidateFeedbackBusy(false);
    }
  };

  const sendHomeTask = async () => {
    if (!taskInstructions.trim()) {
      setStatusMessage('Enter task instructions before sending the home task.');
      return;
    }
    const parsedDeadline = new Date(taskDeadline);
    if (!taskDeadline || Number.isNaN(parsedDeadline.getTime()) || parsedDeadline <= new Date()) {
      setStatusMessage('Choose a future deadline before sending the home task.');
      return;
    }
    setStatusBusy(true);
    setStatusMessage('');
    try {
      const response = await api.post(`/hr/applications/${applicationId}/home-task`, {
        taskInstructions: taskInstructions.trim(),
        deadline: taskDeadline || null,
      });
      setRecord(response.data);
      setTaskDeadline('');
      setTaskInstructions('');
      setStatusMessage('Home task sent.');
    } catch (requestError) {
      setStatusMessage(requestError.response?.data?.message || 'Could not send the home task.');
    } finally {
      setStatusBusy(false);
    }
  };

  const updateHomeTaskDeadline = async () => {
    const parsedDeadline = new Date(taskDeadline);
    if (!taskDeadline || Number.isNaN(parsedDeadline.getTime()) || parsedDeadline <= new Date()) {
      setStatusMessage('Choose a future deadline before updating the home task.');
      return;
    }
    setDeadlineBusy(true);
    setStatusMessage('');
    try {
      const response = await api.put(`/hr/applications/${applicationId}/home-task/deadline`, {
        deadline: taskDeadline,
      });
      setRecord(response.data);
      setEditingTaskDeadline(false);
      setTaskDeadline('');
      setStatusMessage('Home task deadline updated.');
    } catch (requestError) {
      setStatusMessage(requestError.response?.data?.message || 'Could not update the home task deadline.');
    } finally {
      setDeadlineBusy(false);
    }
  };

  const scheduleInterview = async () => {
    const scheduledAt = new Date(interviewTime);
    if ((!editingInterviewId && !selectedInterviewType) || !interviewerId || !interviewTime || Number.isNaN(scheduledAt.getTime())
      || scheduledAt <= new Date() || !meetingLink.trim()) {
      setInterviewMessage('Choose an interviewer, future time, type, and meeting URL.');
      return;
    }
    try {
      const url = new URL(meetingLink.trim());
      if (!['http:', 'https:'].includes(url.protocol)) throw new Error('invalid');
    } catch {
      setInterviewMessage('Enter a valid HTTP or HTTPS meeting URL.');
      return;
    }
    setInterviewBusy(true);
    setInterviewMessage('');
    try {
      const payload = { interviewerId: Number(interviewerId), scheduledAt: interviewTime, meetingLink: meetingLink.trim() };
      const response = editingInterviewId
        ? await api.put(`/hr/interviews/${editingInterviewId}`, payload)
        : await api.post(`/hr/applications/${applicationId}/interviews`, { ...payload, type: selectedInterviewType });
      setRecord((current) => ({ ...current, status: response.data.applicationStatus }));
      setInterviewerId('');
      setInterviewTime('');
      setMeetingLink('');
      setEditingInterviewId(null);
      setEditingInterviewType('');
      setInterviewMessage(editingInterviewId ? 'Interview rescheduled.' : 'Interview scheduled.');
      await loadInterviews();
    } catch (requestError) {
      setInterviewMessage(requestError.response?.data?.message || 'Could not schedule the interview.');
    } finally {
      setInterviewBusy(false);
    }
  };

  const beginReschedule = (interview) => {
    const date = new Date(interview.scheduledAt);
    const localValue = Number.isNaN(date.getTime()) ? ''
      : new Date(date.getTime() - date.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
    setEditingInterviewId(interview.interviewId);
    setEditingInterviewType(interview.type);
    setInterviewerId(String(interview.interviewerId));
    setInterviewTime(localValue);
    setMeetingLink(interview.meetingLink || '');
    setInterviewMessage('');
  };

  const closeReschedule = () => {
    if (interviewBusy) return;
    setEditingInterviewId(null);
    setEditingInterviewType('');
    setInterviewerId('');
    setInterviewTime('');
    setMeetingLink('');
    setInterviewMessage('');
  };

  const cancelInterview = async (interviewId) => {
    setInterviewBusy(true);
    setInterviewMessage('');
    try {
      const response = await api.put(`/hr/interviews/${interviewId}/cancel`);
      setRecord((current) => ({ ...current, status: response.data.applicationStatus }));
      setInterviewMessage('Interview canceled.');
      await loadInterviews();
    } catch (requestError) {
      setInterviewMessage(requestError.response?.data?.message || 'Could not cancel the interview.');
    } finally {
      setInterviewBusy(false);
    }
  };

  return (
    <PageSkeleton title="Candidate Record" description="Review this candidate in the context of one company job application.">
      {loading && <Box sx={{ minHeight: 320, display: 'grid', placeItems: 'center' }}><CircularProgress size={34} /></Box>}
      {!loading && error && <Alert severity="error" action={<Button color="inherit" size="small" onClick={loadRecord}>Retry</Button>}>{error}</Alert>}
      {!loading && !error && record && (
        <Grid container spacing={2.5}>
          <Grid size={{ xs: 12, md: 6 }}>
            <Card sx={{ height: '100%' }}><CardContent>
              <Typography variant="h6" gutterBottom>Candidate profile</Typography>
              <Stack spacing={1.5}>
                <Detail label="Name" value={`${record.firstName} ${record.lastName}`} />
                <Detail label="Email" value={record.email} />
                <Detail label="Current title" value={record.currentTitle} />
                <Detail label="Desired role" value={record.desiredRole} />
                <Detail label="Employment preference" value={employmentLabels[record.employmentType]} />
                <Detail label="Student" value={record.student ? 'Yes' : 'No'} />
                {record.githubProfileUrl ? <Link href={record.githubProfileUrl} target="_blank" rel="noopener noreferrer" sx={{ display: 'inline-flex', alignItems: 'center', gap: 0.75 }}><GitHubIcon fontSize="small" />GitHub profile</Link> : <Detail label="GitHub profile" value={null} />}
              </Stack>
            </CardContent></Card>
          </Grid>

          <Grid size={{ xs: 12, md: 6 }}>
            <Card sx={{ height: '100%' }}><CardContent>
              <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 2 }}>
                <Typography variant="h6">Application summary</Typography>
                <Chip label={statusLabels[record.status] || record.status} color="secondary" variant="outlined" />
              </Stack>
              <Stack spacing={1.5}>
                <Detail label="Applied" value={formatDate(record.appliedAt)} />
                <Detail label="Application ID" value={record.applicationId} />
                <Detail label="Job" value={record.jobTitle} />
                <Detail label="Location" value={record.location} />
                <Detail label="Company" value={record.companyName} />
              </Stack>
            </CardContent></Card>
          </Grid>

          <Grid size={{ xs: 12, md: 7 }}>
            <Card><CardContent>
              <Typography variant="h6" gutterBottom>Cover letter</Typography>
              <Typography variant="body2" color="text.secondary" sx={{ whiteSpace: 'pre-wrap', lineHeight: 1.75 }}>{display(record.coverLetter)}</Typography>
            </CardContent></Card>
          </Grid>
          <Grid size={{ xs: 12, md: 5 }}>
            <Card sx={{ height: '100%' }}><CardContent>
              <Typography variant="h6" gutterBottom>CV</Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>Download the CV securely for this application.</Typography>
              <Button variant="outlined" startIcon={<DownloadRoundedIcon />} onClick={downloadCv} disabled={downloadingCv}>{downloadingCv ? 'Downloading…' : 'Download CV'}</Button>
              {cvError && <Alert severity="error" sx={{ mt: 2 }}>{cvError}</Alert>}
            </CardContent></Card>
          </Grid>

          <Grid size={{ xs: 12, md: 6 }}>
            <Card sx={{ height: '100%' }}><CardContent>
              <Typography variant="h6" gutterBottom>Task information</Typography>
              <Stack spacing={1.5}>
                {record.taskInstructions && <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap', lineHeight: 1.7 }}>{record.taskInstructions}</Typography>}
                {record.status === 'TASK_SUBMITTED' && <Alert severity="info">Submitted task is awaiting technical review.</Alert>}
                {record.status === 'TASK_APPROVED' && <Alert severity="success">Technical review passed.</Alert>}
                <Detail label="Deadline" value={formatDate(record.taskDeadline)} />
                {record.status === 'TASK_SENT' && (!editingTaskDeadline ? (
                  <Button variant="outlined" onClick={() => {
                    setTaskDeadline(toLocalDateTimeInput(record.taskDeadline));
                    setStatusMessage('');
                    setEditingTaskDeadline(true);
                  }}>Update deadline</Button>
                ) : (
                  <Stack spacing={1.25}>
                    <TextField
                      label="Updated home task deadline"
                      type="datetime-local"
                      value={taskDeadline}
                      onChange={(event) => { setTaskDeadline(event.target.value); setStatusMessage(''); }}
                      slotProps={{ inputLabel: { shrink: true } }}
                    />
                    <Stack direction="row" spacing={1}>
                      <Button variant="contained" onClick={updateHomeTaskDeadline} disabled={deadlineBusy || !taskDeadline}>{deadlineBusy ? 'Updating…' : 'Save deadline'}</Button>
                      <Button onClick={() => { setEditingTaskDeadline(false); setTaskDeadline(''); setStatusMessage(''); }} disabled={deadlineBusy}>Cancel</Button>
                    </Stack>
                  </Stack>
                ))}
                {record.taskRepoUrl ? <Link href={record.taskRepoUrl} target="_blank" rel="noopener noreferrer">Submitted repository</Link> : <Detail label="Repository" value={null} />}
                {record.status === 'HR_INTERVIEW' && (
                  <Stack spacing={1.25}>
                    <TextField
                      label="Task instructions"
                      multiline
                      minRows={5}
                      value={taskInstructions}
                      onChange={(event) => { setTaskInstructions(event.target.value); setStatusMessage(''); }}
                      inputProps={{ maxLength: 10000 }}
                      helperText={`${taskInstructions.length}/10000`}
                    />
                    <TextField
                      label="Home task deadline"
                      type="datetime-local"
                      value={taskDeadline}
                      onChange={(event) => { setTaskDeadline(event.target.value); setStatusMessage(''); }}
                      slotProps={{ inputLabel: { shrink: true } }}
                    />
                    <Button variant="contained" onClick={sendHomeTask} disabled={statusBusy || !taskDeadline || !taskInstructions.trim()}>Send home task</Button>
                  </Stack>
                )}
              </Stack>
            </CardContent></Card>
          </Grid>
          <Grid size={{ xs: 12, md: 6 }}>
            <Card sx={{ height: '100%' }}><CardContent>
              <Typography variant="h6" gutterBottom>Interview history</Typography>
              {interviewsLoading ? <CircularProgress size={22} /> : interviews.length === 0
                ? <Typography variant="body2" color="text.secondary">No interviews scheduled.</Typography>
                : <Stack spacing={2}>{interviewGroups.map((group) => {
                  const grouped = interviews.filter((interview) => interview.status === group.status);
                  return grouped.length > 0 && <Box key={group.status}>
                    <Typography variant="subtitle2" sx={{ mb: 1 }}>{group.label}</Typography>
                    <Stack spacing={1}>{grouped.map((interview) => (
                      <Box key={interview.interviewId} sx={{ p: 1.5, border: 1, borderColor: 'divider', borderRadius: 1.5 }}>
                        <Typography variant="subtitle2">{interviewTypeLabels[interview.type] || interview.type}</Typography>
                        <Typography variant="body2">{interview.interviewerName}</Typography>
                        <Typography variant="body2" color="text.secondary">{formatDate(interview.scheduledAt)} · {interview.status}</Typography>
                        {interview.meetingLink && <Link href={interview.meetingLink} target="_blank" rel="noopener noreferrer">Meeting link</Link>}
                        {interview.status === 'COMPLETED' && interview.feedback && <Box sx={{ mt: 1.25, p: 1.25, bgcolor: 'action.hover', borderRadius: 1 }}>
                          <Typography variant="caption" color="text.secondary">Interviewer feedback</Typography>
                          <Typography variant="body2" sx={{ mt: 0.5, whiteSpace: 'pre-wrap' }}>{interview.feedback}</Typography>
                        </Box>}
                        {interview.status === 'SCHEDULED' && <Stack direction="row" spacing={1} sx={{ mt: 1 }}>
                          <Button size="small" onClick={() => beginReschedule(interview)}>Reschedule</Button>
                          <Button size="small" color="error" onClick={() => cancelInterview(interview.interviewId)}>Cancel</Button>
                        </Stack>}
                      </Box>
                    ))}</Stack>
                  </Box>;
                })}</Stack>}
            </CardContent></Card>
          </Grid>

          <Grid size={{ xs: 12 }}>
            <Card><CardContent>
              <Typography variant="h6" gutterBottom>Schedule interview</Typography>
              {interviewersLoading ? <CircularProgress size={22} /> : interviewersError
                ? <Alert severity="error">{interviewersError}</Alert> : interviewers.length === 0
                  ? <Alert severity="info">No interviewers are available for this company.</Alert>
                  : schedulableTypes.length === 0 && !editingInterviewId
                    ? <Typography variant="body2" color="text.secondary">No interview can be scheduled at this application stage.</Typography>
                    : <Stack spacing={1.5}>
                      <TextField select label="Interview type" value={editingInterviewId ? 'Fixed' : selectedInterviewType} disabled={Boolean(editingInterviewId)} onChange={(event) => setInterviewType(event.target.value)}>
                        {editingInterviewId && <MenuItem value="Fixed">Existing type (unchanged)</MenuItem>}
                        {schedulableTypes.map((type) => <MenuItem key={type} value={type}>{interviewTypeLabels[type]}</MenuItem>)}
                      </TextField>
                      <TextField select label="Interviewer" value={interviewerId} onChange={(event) => setInterviewerId(event.target.value)}>
                        {interviewers.map((interviewer) => <MenuItem key={interviewer.interviewerId} value={interviewer.interviewerId}>{interviewer.fullName} · {interviewer.email}</MenuItem>)}
                      </TextField>
                      <TextField label="Date and time" type="datetime-local" value={interviewTime} onChange={(event) => setInterviewTime(event.target.value)} slotProps={{ inputLabel: { shrink: true } }} />
                      <TextField label="Meeting URL" value={meetingLink} onChange={(event) => setMeetingLink(event.target.value)} placeholder="https://meet.example.com/interview" />
                      <Stack direction="row" spacing={1}>
                        <Button variant="contained" onClick={scheduleInterview} disabled={interviewBusy}>{interviewBusy ? 'Saving…' : editingInterviewId ? 'Save reschedule' : 'Schedule interview'}</Button>
                        {editingInterviewId && <Button onClick={() => setEditingInterviewId(null)}>Cancel edit</Button>}
                      </Stack>
                    </Stack>}
              {interviewMessage && <Alert severity={['Interview scheduled.', 'Interview rescheduled.', 'Interview canceled.'].includes(interviewMessage) ? 'success' : interviewMessage.startsWith('Select') ? 'info' : 'error'} sx={{ mt: 2 }}>{interviewMessage}</Alert>}
            </CardContent></Card>
          </Grid>

          <Grid size={{ xs: 12 }}>
            <Card><CardContent>
              <Typography variant="h6" gutterBottom>Application actions</Typography>
              {statusActions.length > 0 ? (
                <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.25}>
                  {statusActions.map((action) => (
                    <Button
                      key={action.status}
                      variant={action.status === 'REJECTED' ? 'outlined' : 'contained'}
                      color={action.status === 'REJECTED' ? 'error' : 'primary'}
                      disabled={statusBusy}
                      onClick={() => {
                        if (action.status === 'REJECTED') {
                          setCandidateFeedbackDraft(record.candidateFeedback || '');
                          setRejectOpen(true);
                        } else updateStatus(action.status);
                      }}
                    >{action.label}</Button>
                  ))}
                </Stack>
              ) : <Typography variant="body2" color="text.secondary">No HR status actions are available at this stage.</Typography>}
              {statusMessage && <Alert severity={statusMessage.startsWith('Status updated') || statusMessage === 'Home task sent.' || statusMessage === 'Home task deadline updated.' || statusMessage === 'Candidate rejected.' ? 'success' : 'error'} sx={{ mt: 2 }}>{statusMessage}</Alert>}
            </CardContent></Card>
          </Grid>

          {record.status === 'REJECTED' && <Grid size={{ xs: 12 }}>
            <Card><CardContent>
              <Typography variant="h6" gutterBottom>Feedback to candidate</Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>This message is visible to the candidate. Do not include internal notes or interviewer feedback.</Typography>
              <TextField multiline minRows={4} fullWidth value={candidateFeedbackDraft} onChange={(event) => { setCandidateFeedbackDraft(event.target.value); setCandidateFeedbackMessage(''); }} inputProps={{ maxLength: 10000 }} helperText={`${candidateFeedbackDraft.length}/10000`} placeholder="Optional candidate-facing feedback" />
              <Stack direction="row" alignItems="center" spacing={2} sx={{ mt: 2 }}>
                <Button variant="contained" onClick={saveCandidateFeedback} disabled={candidateFeedbackBusy}>{candidateFeedbackBusy ? 'Saving…' : 'Save feedback'}</Button>
                {candidateFeedbackMessage && <Typography variant="body2" color={candidateFeedbackMessage === 'Candidate feedback saved.' ? 'success.main' : 'error.main'}>{candidateFeedbackMessage}</Typography>}
              </Stack>
            </CardContent></Card>
          </Grid>}

          <Grid size={{ xs: 12 }}>
            <Card><CardContent>
              <Typography variant="h6" gutterBottom>Internal HR notes</Typography>
              <TextField multiline minRows={4} fullWidth value={notes} onChange={(event) => { setNotes(event.target.value); setNotesMessage(''); }} inputProps={{ maxLength: 10000 }} placeholder="Add private notes for the HR team" />
              <Stack direction="row" alignItems="center" spacing={2} sx={{ mt: 2 }}>
                <Button variant="contained" startIcon={<SaveRoundedIcon />} onClick={saveNotes} disabled={savingNotes}>{savingNotes ? 'Saving…' : 'Save notes'}</Button>
                {notesMessage && <Typography variant="body2" color={notesMessage === 'Notes saved.' ? 'success.main' : 'error.main'}>{notesMessage}</Typography>}
              </Stack>
            </CardContent></Card>
          </Grid>

          <Dialog open={Boolean(editingInterviewId)} onClose={closeReschedule} fullWidth maxWidth="sm">
            <DialogTitle>Reschedule interview</DialogTitle>
            <DialogContent>
              <Stack spacing={2} sx={{ pt: 1 }}>
                <TextField label="Interview type" value={interviewTypeLabels[editingInterviewType] || editingInterviewType} disabled />
                <TextField select label="Interviewer" value={interviewerId} onChange={(event) => setInterviewerId(event.target.value)}>
                  {interviewers.map((interviewer) => <MenuItem key={interviewer.interviewerId} value={interviewer.interviewerId}>{interviewer.fullName} · {interviewer.email}</MenuItem>)}
                </TextField>
                <TextField label="Date and time" type="datetime-local" value={interviewTime} onChange={(event) => setInterviewTime(event.target.value)} slotProps={{ inputLabel: { shrink: true } }} />
                <TextField label="Meeting URL" value={meetingLink} onChange={(event) => setMeetingLink(event.target.value)} placeholder="https://meet.example.com/interview" />
                {interviewMessage && <Alert severity="error">{interviewMessage}</Alert>}
              </Stack>
            </DialogContent>
            <DialogActions sx={{ px: 3, pb: 2.5 }}>
              <Button onClick={closeReschedule} disabled={interviewBusy}>Cancel</Button>
              <Button variant="contained" onClick={scheduleInterview} disabled={interviewBusy}>{interviewBusy ? 'Saving…' : 'Save changes'}</Button>
            </DialogActions>
          </Dialog>
          <Dialog open={rejectOpen} onClose={() => !candidateFeedbackBusy && setRejectOpen(false)} fullWidth maxWidth="sm">
            <DialogTitle>Reject candidate?</DialogTitle>
            <DialogContent>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>This action ends the candidate&apos;s application. You may optionally provide feedback they can read.</Typography>
              <TextField label="Feedback to candidate" multiline minRows={5} fullWidth value={candidateFeedbackDraft} onChange={(event) => setCandidateFeedbackDraft(event.target.value)} inputProps={{ maxLength: 10000 }} helperText={`${candidateFeedbackDraft.length}/10000 · Optional`} />
            </DialogContent>
            <DialogActions><Button onClick={() => setRejectOpen(false)} disabled={candidateFeedbackBusy}>Cancel</Button><Button color="error" variant="contained" onClick={rejectCandidate} disabled={candidateFeedbackBusy}>{candidateFeedbackBusy ? 'Rejecting…' : 'Reject candidate'}</Button></DialogActions>
          </Dialog>
        </Grid>
      )}
    </PageSkeleton>
  );
}
