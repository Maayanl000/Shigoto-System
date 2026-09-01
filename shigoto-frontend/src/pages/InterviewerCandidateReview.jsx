import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Alert, Box, Button, Card, CardContent, Chip, CircularProgress, Dialog, DialogActions, DialogContent, DialogTitle, Link, Stack, TextField, Typography } from '@mui/material';
import ArrowBackRoundedIcon from '@mui/icons-material/ArrowBackRounded';
import PageSkeleton from '../components/PageSkeleton';
import api from '../services/api';
import { hasDisplayValue } from '../utils/displayValue';

const show = (value) => value || 'Not provided';

const formatDate = (value) => {
  if (!value) return 'Not provided';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? 'Not provided' : date.toLocaleString();
};

function GithubAnalysis({ analysis }) {
  if (!analysis) return null;
  return <Box sx={{ pt: 1.5, mt: 0.5, borderTop: 1, borderColor: 'divider' }}>
    <Stack direction="row" justifyContent="space-between" alignItems="center" spacing={1}>
      <Typography variant="subtitle2">GitHub analysis</Typography>
      <Chip size="small" variant="outlined" label={analysis.status.replaceAll('_', ' ')} />
    </Stack>
    {analysis.topLanguages?.length > 0 && <Box sx={{ mt: 1, minWidth: 0 }}>
      <Typography variant="caption" color="text.secondary">Languages</Typography>
      <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.75, width: '100%', minWidth: 0, mt: 0.5 }}>
        {analysis.topLanguages.map((language) => <Chip key={language} size="small" label={language} />)}
      </Box>
    </Box>}
    <Stack spacing={0.5} sx={{ mt: 1 }}>
      <Typography variant="caption" color="text.secondary">Public repositories: {analysis.publicRepositoryCount ?? 'Not provided'}</Typography>
      <Typography variant="caption" color="text.secondary">Latest public push: {formatDate(analysis.latestPushAt)}</Typography>
      <Typography variant="caption" color="text.secondary">Analyzed: {formatDate(analysis.analyzedAt)}</Typography>
    </Stack>
  </Box>;
}

export default function InterviewerCandidateReview() {
  const { applicationId } = useParams();
  const navigate = useNavigate();
  const [record, setRecord] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [notesOpen, setNotesOpen] = useState(false);
  const [taskNotes, setTaskNotes] = useState('');
  const [notesBusy, setNotesBusy] = useState(false);
  const [notesError, setNotesError] = useState('');

  const openNotes = () => {
    setTaskNotes(record.taskReviewNotes || '');
    setNotesError('');
    setNotesOpen(true);
  };

  const saveNotes = async () => {
    setNotesBusy(true);
    setNotesError('');
    try {
      const response = await api.put(`/interviewer/applications/${applicationId}/task-review-notes`, { taskReviewNotes: taskNotes });
      setRecord((current) => ({ ...current, taskReviewNotes: response.data.taskReviewNotes }));
      setNotesOpen(false);
    } catch (requestError) {
      setNotesError(requestError.response?.data?.message || 'Could not save private task notes.');
    } finally {
      setNotesBusy(false);
    }
  };

  useEffect(() => {
    let active = true;
    api.get(`/interviewer/applications/${applicationId}`)
      .then((response) => { if (active) setRecord(response.data); })
      .catch((requestError) => { if (active) setError(requestError.response?.data?.message || 'Could not load candidate review.'); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [applicationId]);

  return (
    <PageSkeleton title="Candidate Review" description="Review candidate and application context for your assigned work.">
      <Button startIcon={<ArrowBackRoundedIcon />} onClick={() => navigate(-1)} sx={{ mb: 2 }}>Back</Button>
      {loading && <Box sx={{ py: 8, display: 'grid', placeItems: 'center' }}><CircularProgress /></Box>}
      {!loading && error && <Alert severity="error">{error}</Alert>}
      {!loading && record && <Stack spacing={2.5}>
        <Card><CardContent><Typography variant="h6">{record.candidateName}</Typography>
          <Typography color="text.secondary">{record.jobTitle} · {record.companyName}</Typography>
          <Stack spacing={1} sx={{ mt: 2 }}>
            <Typography variant="body2">Email: {show(record.email)}</Typography>
            {hasDisplayValue(record.currentTitle) && <Typography variant="body2">Current title: {record.currentTitle}</Typography>}
            {hasDisplayValue(record.desiredRole) && <Typography variant="body2">Desired role: {record.desiredRole}</Typography>}
            {hasDisplayValue(record.githubProfileUrl) && <Link href={record.githubProfileUrl} target="_blank" rel="noopener noreferrer">GitHub profile</Link>}
            <GithubAnalysis analysis={record.githubAnalysis} />
          </Stack>
        </CardContent></Card>
        {(record.taskInstructions || record.taskRepoUrl) && <Card><CardContent>
          <Typography variant="h6">Home task</Typography>
          <Typography variant="body2" sx={{ mt: 1.5, whiteSpace: 'pre-wrap' }}>{record.taskInstructions}</Typography>
          <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 1 }}>Deadline: {record.taskDeadline ? new Date(record.taskDeadline).toLocaleString() : 'Not provided'}</Typography>
          {record.taskRepoUrl && <Link href={record.taskRepoUrl} target="_blank" rel="noopener noreferrer" sx={{ display: 'inline-block', mt: 1 }}>Submitted repository</Link>}
          {record.status === 'TASK_SUBMITTED' && <Button variant="outlined" onClick={openNotes} sx={{ display: 'block', mt: 2 }}>Private notes</Button>}
        </CardContent></Card>}
      </Stack>}
      <Dialog open={notesOpen} onClose={() => !notesBusy && setNotesOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>Private task notes</DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>{record?.candidateName} · {record?.jobTitle}</Typography>
          {notesError && <Alert severity="error" sx={{ mb: 2 }}>{notesError}</Alert>}
          <TextField autoFocus fullWidth multiline minRows={7} value={taskNotes} onChange={(event) => setTaskNotes(event.target.value)} inputProps={{ maxLength: 10000 }} helperText={`${taskNotes.length}/10000 · Never visible to the candidate.`} />
        </DialogContent>
        <DialogActions><Button onClick={() => setNotesOpen(false)} disabled={notesBusy}>Cancel</Button><Button variant="contained" onClick={saveNotes} disabled={notesBusy}>{notesBusy ? 'Saving…' : 'Save'}</Button></DialogActions>
      </Dialog>
    </PageSkeleton>
  );
}
