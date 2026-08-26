import { useCallback, useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { Alert, Box, Button, Card, CardContent, Chip, CircularProgress, Grid, Link, Stack, TextField, Typography } from '@mui/material';
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

  const loadRecord = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const response = await api.get(`/hr/applications/${applicationId}`);
      setRecord(response.data);
      setNotes(response.data.hrNotes || '');
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
                <Detail label="Deadline" value={formatDate(record.taskDeadline)} />
                {record.taskRepoUrl ? <Link href={record.taskRepoUrl} target="_blank" rel="noopener noreferrer">Submitted repository</Link> : <Detail label="Repository" value={null} />}
              </Stack>
            </CardContent></Card>
          </Grid>
          <Grid size={{ xs: 12, md: 6 }}>
            <Card sx={{ height: '100%' }}><CardContent>
              <Typography variant="h6" gutterBottom>Interview history</Typography>
              <Typography variant="body2" color="text.secondary">Interview records will appear here when the HR interview flow is connected.</Typography>
            </CardContent></Card>
          </Grid>

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
        </Grid>
      )}
    </PageSkeleton>
  );
}
