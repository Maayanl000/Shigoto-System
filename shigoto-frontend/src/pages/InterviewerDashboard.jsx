import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link as RouterLink } from 'react-router-dom';
import { Alert, Box, Button, Card, CardContent, Chip, CircularProgress, Dialog, DialogActions, DialogContent, DialogContentText, DialogTitle, Grid, Link as MuiLink, Stack, Tab, Tabs, TextField, Typography } from '@mui/material';
import CalendarMonthOutlinedIcon from '@mui/icons-material/CalendarMonthOutlined';
import EventAvailableOutlinedIcon from '@mui/icons-material/EventAvailableOutlined';
import HistoryRoundedIcon from '@mui/icons-material/HistoryRounded';
import RateReviewOutlinedIcon from '@mui/icons-material/RateReviewOutlined';
import PageSkeleton from '../components/PageSkeleton';
import FeedbackDialog from '../components/FeedbackDialog';
import api from '../services/api';

function formatDateTime(value) {
  if (!value) return 'Date and time unavailable';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? 'Date and time unavailable'
    : new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(date);
}

function EmptyState({ children }) {
  return <Typography variant="body2" color="text.secondary" sx={{ mt: 2 }}>{children}</Typography>;
}

function InterviewItem({ interview, onFeedback, onNotes }) {
  const upcoming = interview.status === 'SCHEDULED';
  return (
    <Box sx={{ p: 2, border: 1, borderColor: 'divider', borderLeft: upcoming ? 4 : 1, borderLeftColor: upcoming ? 'success.main' : 'divider', borderRadius: 1.5 }}>
      <Stack direction={{ xs: 'column', md: 'row' }} justifyContent="space-between" spacing={2}>
        <Box>
          <Typography variant="subtitle1" fontWeight={800}>{interview.candidateName}</Typography>
          <Typography variant="body2" color="text.secondary">{interview.jobTitle} · {interview.companyName}</Typography>
          <Typography variant="body2" sx={{ mt: 1 }}>{interview.interviewType} interview</Typography>
          <Stack direction="row" alignItems="center" spacing={0.75} color="text.secondary" sx={{ mt: 0.75 }}>
            <CalendarMonthOutlinedIcon fontSize="small" />
            <Typography variant="body2">{formatDateTime(interview.scheduledAt)}</Typography>
          </Stack>
        </Box>
        <Chip label={interview.status} color={upcoming ? 'success' : interview.status === 'CANCELED' ? 'default' : 'secondary'} variant={upcoming ? 'filled' : 'outlined'} />
      </Stack>
      {interview.status === 'COMPLETED' && interview.feedback && (
        <Box sx={{ mt: 2, p: 1.5, bgcolor: 'action.hover', borderRadius: 1.5 }}>
          <Typography variant="caption" color="text.secondary">Your feedback</Typography>
          <Typography variant="body2" sx={{ mt: 0.5, whiteSpace: 'pre-wrap' }}>{interview.feedback}</Typography>
        </Box>
      )}
      {upcoming && (
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1} sx={{ mt: 2 }}>
          <Button component={RouterLink} to={`/interviewer/applications/${interview.applicationId}`}>Candidate review</Button>
          {interview.meetingLink && <Button component="a" href={interview.meetingLink} target="_blank" rel="noopener noreferrer" variant="contained">Join interview</Button>}
          <Button variant="outlined" onClick={() => onNotes(interview)}>Private notes</Button>
          <Button variant="outlined" startIcon={<RateReviewOutlinedIcon />} onClick={() => onFeedback(interview)}>Submit feedback</Button>
        </Stack>
      )}
    </Box>
  );
}

export default function InterviewerDashboard() {
  const [dashboardMode, setDashboardMode] = useState('active');
  const [interviews, setInterviews] = useState([]);
  const [interviewsLoading, setInterviewsLoading] = useState(true);
  const [interviewsError, setInterviewsError] = useState('');
  const [tasks, setTasks] = useState([]);
  const [tasksLoading, setTasksLoading] = useState(true);
  const [tasksError, setTasksError] = useState('');
  const [reviewingId, setReviewingId] = useState(null);
  const [selectedInterview, setSelectedInterview] = useState(null);
  const [feedback, setFeedback] = useState('');
  const [feedbackBusy, setFeedbackBusy] = useState(false);
  const [feedbackError, setFeedbackError] = useState('');
  const [notesInterview, setNotesInterview] = useState(null);
  const [privateNotes, setPrivateNotes] = useState('');
  const [notesBusy, setNotesBusy] = useState(false);
  const [notesError, setNotesError] = useState('');
  const [rejectTask, setRejectTask] = useState(null);
  const [notesTask, setNotesTask] = useState(null);
  const [taskNotes, setTaskNotes] = useState('');
  const [taskNotesBusy, setTaskNotesBusy] = useState(false);
  const [taskNotesError, setTaskNotesError] = useState('');

  const loadInterviews = useCallback(async () => {
    setInterviewsLoading(true);
    setInterviewsError('');
    try {
      const response = await api.get('/interviewer/interviews');
      const records = Array.isArray(response.data) ? response.data : [];
      setInterviews(records);
      return records;
    } catch (requestError) {
      setInterviewsError(requestError.response?.data?.message || 'Could not load assigned interviews.');
    } finally {
      setInterviewsLoading(false);
    }
  }, []);

  const loadTasks = useCallback(async () => {
    setTasksLoading(true);
    setTasksError('');
    try {
      const response = await api.get('/interviewer/tasks');
      const records = Array.isArray(response.data) ? response.data : [];
      setTasks(records);
      return records;
    } catch (requestError) {
      setTasksError(requestError.response?.data?.message || 'Could not load submitted tasks.');
    } finally {
      setTasksLoading(false);
    }
  }, []);

  useEffect(() => {
    let active = true;
    api.get('/interviewer/interviews')
      .then((response) => { if (active) setInterviews(Array.isArray(response.data) ? response.data : []); })
      .catch((requestError) => { if (active) setInterviewsError(requestError.response?.data?.message || 'Could not load assigned interviews.'); })
      .finally(() => { if (active) setInterviewsLoading(false); });
    api.get('/interviewer/tasks')
      .then((response) => { if (active) setTasks(Array.isArray(response.data) ? response.data : []); })
      .catch((requestError) => { if (active) setTasksError(requestError.response?.data?.message || 'Could not load submitted tasks.'); })
      .finally(() => { if (active) setTasksLoading(false); });
    return () => { active = false; };
  }, []);

  const upcomingInterviews = useMemo(() => interviews.filter((item) => item.status === 'SCHEDULED'), [interviews]);
  const pastInterviews = useMemo(() => interviews.filter((item) => ['COMPLETED', 'CANCELED'].includes(item.status)), [interviews]);

  const reviewTask = async (applicationId, decision) => {
    setReviewingId(applicationId);
    setTasksError('');
    try {
      const task = tasks.find((item) => item.applicationId === applicationId);
      await api.put(`/interviewer/applications/${applicationId}/task-review`, { decision, version: task?.version });
      setTasks((current) => current.filter((task) => task.applicationId !== applicationId));
    } catch (requestError) {
      if (requestError.response?.status === 409) await loadTasks();
      setTasksError(requestError.response?.data?.message || 'Could not review this task.');
    } finally {
      setReviewingId(null);
    }
  };

  const openFeedback = (interview) => {
    setSelectedInterview(interview);
    setFeedback('');
    setFeedbackError('');
  };

  const closeFeedback = () => {
    if (feedbackBusy) return;
    setSelectedInterview(null);
    setFeedback('');
    setFeedbackError('');
  };

  const submitFeedback = async () => {
    if (!selectedInterview || !feedback.trim()) return;
    setFeedbackBusy(true);
    setFeedbackError('');
    try {
      const response = await api.put(`/interviewer/interviews/${selectedInterview.interviewId}/feedback`, {
        feedback: feedback.trim(), version: selectedInterview.version,
      });
      setInterviews((current) => current.map((item) => item.interviewId === response.data.interviewId ? response.data : item));
      setSelectedInterview(null);
      setFeedback('');
    } catch (requestError) {
      if (requestError.response?.status === 409) {
        const refreshed = await loadInterviews();
        setSelectedInterview(refreshed?.find((item) => item.interviewId === selectedInterview.interviewId) || null);
      }
      setFeedbackError(requestError.response?.data?.message || 'Could not submit interview feedback.');
    } finally {
      setFeedbackBusy(false);
    }
  };

  const openNotes = (interview) => {
    setNotesInterview(interview);
    setPrivateNotes(interview.interviewerNotes || '');
    setNotesError('');
  };

  const saveNotes = async () => {
    setNotesBusy(true);
    setNotesError('');
    try {
      const response = await api.put(`/interviewer/interviews/${notesInterview.interviewId}/notes`, {
        interviewerNotes: privateNotes, version: notesInterview.version,
      });
      setInterviews((current) => current.map((item) => item.interviewId === response.data.interviewId ? response.data : item));
      setNotesInterview(null);
    } catch (requestError) {
      if (requestError.response?.status === 409) {
        const refreshed = await loadInterviews();
        setNotesInterview(refreshed?.find((item) => item.interviewId === notesInterview.interviewId) || null);
      }
      setNotesError(requestError.response?.data?.message || 'Could not save private notes.');
    } finally {
      setNotesBusy(false);
    }
  };

  const openTaskNotes = (task) => {
    setNotesTask(task);
    setTaskNotes(task.taskReviewNotes || '');
    setTaskNotesError('');
  };

  const saveTaskNotes = async () => {
    setTaskNotesBusy(true);
    setTaskNotesError('');
    try {
      const response = await api.put(`/interviewer/applications/${notesTask.applicationId}/task-review-notes`, {
        taskReviewNotes: taskNotes, version: notesTask.version,
      });
      setTasks((current) => current.map((item) => item.applicationId === response.data.applicationId ? response.data : item));
      setNotesTask(null);
    } catch (requestError) {
      if (requestError.response?.status === 409) {
        const refreshed = await loadTasks();
        setNotesTask(refreshed?.find((item) => item.applicationId === notesTask.applicationId) || null);
      }
      setTaskNotesError(requestError.response?.data?.message || 'Could not save private task notes.');
    } finally {
      setTaskNotesBusy(false);
    }
  };

  return (
    <PageSkeleton title="My Interviews" description="Manage assigned interviews and review submitted home tasks.">
      <Grid container spacing={2.5}>
        <Grid size={12}>
          <Tabs value={dashboardMode} onChange={(_, value) => setDashboardMode(value)} aria-label="Interviewer dashboard mode">
            <Tab value="active" label="Active" />
            <Tab value="history" label="History" />
          </Tabs>
        </Grid>
        {dashboardMode === 'active' && <>
        <Grid size={12}>
          <Card><CardContent>
            <Stack direction="row" alignItems="center" spacing={1}><EventAvailableOutlinedIcon color="success" /><Typography variant="h6" component="h2">Upcoming interviews</Typography></Stack>
            {interviewsLoading ? <Box sx={{ py: 4, display: 'grid', placeItems: 'center' }}><CircularProgress size={26} /></Box>
              : interviewsError ? <Alert severity="error" sx={{ mt: 2 }} action={<Button color="inherit" size="small" onClick={loadInterviews}>Retry</Button>}>{interviewsError}</Alert>
                : upcomingInterviews.length === 0 ? <EmptyState>No assigned interviews are currently scheduled.</EmptyState>
                  : <Stack spacing={1.5} sx={{ mt: 2 }}>{upcomingInterviews.map((item) => <InterviewItem key={item.interviewId} interview={item} onFeedback={openFeedback} onNotes={openNotes} />)}</Stack>}
          </CardContent></Card>
        </Grid>

        <Grid size={12}>
          <Card><CardContent>
            <Typography variant="h6" component="h2">Tasks awaiting review</Typography>
            {tasksLoading ? <Box sx={{ py: 4, display: 'grid', placeItems: 'center' }}><CircularProgress size={26} /></Box>
              : tasksError ? <Alert severity="error" sx={{ mt: 2 }} action={<Button color="inherit" size="small" onClick={loadTasks}>Retry</Button>}>{tasksError}</Alert>
                : tasks.length === 0 ? <EmptyState>No submitted home tasks are awaiting review.</EmptyState>
                  : <Stack spacing={1.5} sx={{ mt: 2 }}>{tasks.map((task) => (
                    <Box key={task.applicationId} sx={{ p: 2, border: 1, borderColor: 'divider', borderRadius: 1.5 }}>
                      <Typography variant="subtitle1" fontWeight={750}>{task.candidateName}</Typography>
                      <Typography variant="body2" color="text.secondary">{task.jobTitle}</Typography>
                      <Typography variant="body2" sx={{ mt: 1.5, whiteSpace: 'pre-wrap' }}>{task.taskInstructions}</Typography>
                      <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 1 }}>Deadline: {task.taskDeadline ? new Date(task.taskDeadline).toLocaleString() : 'Not provided'}</Typography>
                      <MuiLink href={task.taskRepoUrl} target="_blank" rel="noopener noreferrer" sx={{ display: 'inline-block', mt: 1 }}>Open submitted repository</MuiLink>
                      <Stack direction="row" spacing={1} sx={{ mt: 2 }}>
                        <Button component={RouterLink} to={`/interviewer/applications/${task.applicationId}`}>Candidate review</Button>
                        <Button variant="outlined" onClick={() => openTaskNotes(task)}>Private notes</Button>
                        <Button variant="contained" disabled={reviewingId === task.applicationId} onClick={() => reviewTask(task.applicationId, 'APPROVE')}>Approve</Button>
                        <Button variant="outlined" color="error" disabled={reviewingId === task.applicationId} onClick={() => setRejectTask(task)}>Reject</Button>
                      </Stack>
                    </Box>
                  ))}</Stack>}
          </CardContent></Card>
        </Grid>
        </>}
        {dashboardMode === 'history' &&
        <Grid size={12}>
          <Card><CardContent>
            <Stack direction="row" alignItems="center" spacing={1}><HistoryRoundedIcon color="primary" /><Typography variant="h6" component="h2">Past interviews</Typography></Stack>
            {interviewsLoading ? <Box sx={{ py: 4, display: 'grid', placeItems: 'center' }}><CircularProgress size={26} /></Box>
              : interviewsError ? <Alert severity="error" sx={{ mt: 2 }}>{interviewsError}</Alert>
                : pastInterviews.length === 0 ? <EmptyState>No completed or canceled interviews yet.</EmptyState>
                  : <Stack spacing={1.5} sx={{ mt: 2 }}>{pastInterviews.map((item) => <InterviewItem key={item.interviewId} interview={item} onFeedback={openFeedback} onNotes={openNotes} />)}</Stack>}
          </CardContent></Card>
        </Grid>}
      </Grid>
      <FeedbackDialog interview={selectedInterview} feedback={feedback} onFeedbackChange={(value) => { setFeedback(value); setFeedbackError(''); }} onClose={closeFeedback} onSubmit={submitFeedback} busy={feedbackBusy} error={feedbackError} />
      <Dialog open={Boolean(notesInterview)} onClose={() => !notesBusy && setNotesInterview(null)} fullWidth maxWidth="sm">
        <DialogTitle>Private notes</DialogTitle>
        <DialogContent><Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>Only you can read these preparation notes. Saving does not change interview status.</Typography>
          {notesError && <Alert severity="error" sx={{ mb: 2 }}>{notesError}</Alert>}
          <TextField autoFocus fullWidth multiline minRows={6} value={privateNotes} onChange={(event) => setPrivateNotes(event.target.value)} inputProps={{ maxLength: 10000 }} helperText={`${privateNotes.length}/10000`} />
        </DialogContent>
        <DialogActions><Button onClick={() => setNotesInterview(null)} disabled={notesBusy}>Cancel</Button><Button variant="contained" onClick={saveNotes} disabled={notesBusy}>{notesBusy ? 'Saving…' : 'Save notes'}</Button></DialogActions>
      </Dialog>
      <Dialog open={Boolean(rejectTask)} onClose={() => setRejectTask(null)}>
        <DialogTitle>Reject candidate?</DialogTitle>
        <DialogContent><DialogContentText>This action rejects and ends the candidate&apos;s application. It cannot be treated as a request for task changes.</DialogContentText></DialogContent>
        <DialogActions><Button onClick={() => setRejectTask(null)}>Cancel</Button><Button color="error" variant="contained" onClick={async () => { const task = rejectTask; setRejectTask(null); await reviewTask(task.applicationId, 'REJECT'); }}>Reject candidate</Button></DialogActions>
      </Dialog>
      <Dialog open={Boolean(notesTask)} onClose={() => !taskNotesBusy && setNotesTask(null)} fullWidth maxWidth="sm">
        <DialogTitle>Private task notes</DialogTitle>
        <DialogContent>
          {notesTask && <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>{notesTask.candidateName} · {notesTask.jobTitle}</Typography>}
          {taskNotesError && <Alert severity="error" sx={{ mb: 2 }}>{taskNotesError}</Alert>}
          <TextField autoFocus fullWidth multiline minRows={7} value={taskNotes} onChange={(event) => setTaskNotes(event.target.value)} inputProps={{ maxLength: 10000 }} helperText={`${taskNotes.length}/10000 · Private to authorized interviewers.`} />
        </DialogContent>
        <DialogActions><Button onClick={() => setNotesTask(null)} disabled={taskNotesBusy}>Cancel</Button><Button variant="contained" onClick={saveTaskNotes} disabled={taskNotesBusy}>{taskNotesBusy ? 'Saving…' : 'Save'}</Button></DialogActions>
      </Dialog>
    </PageSkeleton>
  );
}
