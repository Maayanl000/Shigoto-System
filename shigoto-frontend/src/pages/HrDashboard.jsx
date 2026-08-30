import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Alert, Box, Button, Card, CardActionArea, CardContent, Chip, CircularProgress, Divider, FormControl, InputLabel, MenuItem, Paper, Select, Stack, Typography } from '@mui/material';
import AccessTimeRoundedIcon from '@mui/icons-material/AccessTimeRounded';
import PageSkeleton from '../components/PageSkeleton';
import api from '../services/api';
import { getKanbanDatePresentation, getKanbanStatusLabel } from '../utils/hrKanban';

const columnDefinitions = [
  { title: 'Applied', color: '#64748b', statuses: ['APPLIED'] },
  { title: 'Screening', color: '#2563eb', statuses: ['HR_INTERVIEW'] },
  { title: 'Task', color: '#7c3aed', statuses: ['TASK_SENT', 'TASK_SUBMITTED', 'TASK_APPROVED'] },
  { title: 'Interview', color: '#087f8c', statuses: ['TECH_INTERVIEW_SCHEDULED'] },
  { title: 'Decision', color: '#d97706', statuses: ['OFFER', 'REJECTED'] },
];

const SELECT_JOB_PLACEHOLDER = '__select_job__';

function formatKanbanDate(value) {
  if (!value) return 'Date unavailable';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return 'Date unavailable';
  return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium' }).format(date);
}

function formatKanbanTimestamp(application) {
  const presentation = getKanbanDatePresentation(application);
  return `${presentation.label} ${formatKanbanDate(presentation.date)}`;
}

export default function HrDashboard() {
  const navigate = useNavigate();
  const [applications, setApplications] = useState([]);
  const [jobs, setJobs] = useState([]);
  const [selectedJobId, setSelectedJobId] = useState(SELECT_JOB_PLACEHOLDER);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadPipeline = useCallback(async (jobId = '') => {
    setLoading(true);
    setError('');
    try {
      const [applicationsResponse, jobsResponse] = await Promise.all([
        api.get('/applications', { params: jobId ? { jobId } : {} }),
        api.get('/hr/jobs'),
      ]);
      setApplications(Array.isArray(applicationsResponse.data) ? applicationsResponse.data : []);
      setJobs(Array.isArray(jobsResponse.data) ? jobsResponse.data : []);
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Could not load the hiring pipeline.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    let active = true;
    Promise.all([api.get('/applications'), api.get('/hr/jobs')])
      .then(([applicationsResponse, jobsResponse]) => {
        if (!active) return;
        setApplications(Array.isArray(applicationsResponse.data) ? applicationsResponse.data : []);
        setJobs(Array.isArray(jobsResponse.data) ? jobsResponse.data : []);
      })
      .catch((requestError) => {
        if (active) setError(requestError.response?.data?.message || 'Could not load the hiring pipeline.');
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, []);

  const columns = useMemo(() => columnDefinitions.map((column) => ({
    ...column,
    applications: applications.filter((application) => column.statuses.includes(application.status)),
  })), [applications]);
  const activeJobId = selectedJobId === SELECT_JOB_PLACEHOLDER ? '' : selectedJobId;
  const hasJobFilter = Boolean(activeJobId);
  const selectedJob = jobs.find((job) => String(job.id) === String(activeJobId));

  const changeJob = (event) => {
    const jobId = event.target.value;
    setSelectedJobId(jobId);
    loadPipeline(jobId);
  };

  return (
    <PageSkeleton title="HR Dashboard" description="Review your company's applications across the hiring pipeline.">
      <Paper variant="outlined" sx={{ display: 'flex', alignItems: 'center', flexWrap: 'wrap', gap: 2, p: 2, mb: 2.5 }}>
        <Box>
          <Typography variant="caption" color="text.secondary">Pipeline view</Typography>
          <Typography variant="body2" fontWeight={700}>{selectedJob ? selectedJob.title : 'All company applications'}</Typography>
        </Box>
        <Divider orientation="vertical" flexItem />
        <FormControl size="small" sx={{ minWidth: 240 }}>
          <InputLabel id="pipeline-job-filter-label" shrink>Job</InputLabel>
          <Select
            labelId="pipeline-job-filter-label"
            label="Job"
            value={selectedJobId}
            onChange={changeJob}
            displayEmpty
            renderValue={(value) => {
              if (value === SELECT_JOB_PLACEHOLDER) return 'Select job';
              if (value === '') return 'All jobs';
              return jobs.find((job) => String(job.id) === String(value))?.title || '';
            }}
          >
            <MenuItem value={SELECT_JOB_PLACEHOLDER} disabled>Select job</MenuItem>
            <MenuItem value="">All jobs</MenuItem>
            {jobs.map((job) => (
              <MenuItem key={job.id} value={String(job.id)}>{job.title} ({job.status})</MenuItem>
            ))}
          </Select>
        </FormControl>
        <Chip label={`${applications.length} application${applications.length === 1 ? '' : 's'}`} size="small" color="secondary" variant="outlined" />
        <Typography variant="caption" color="text.secondary" sx={{ ml: { md: 'auto' } }}>
          Cards are grouped by current application status
        </Typography>
      </Paper>

      {loading && (
        <Box sx={{ minHeight: 300, display: 'grid', placeItems: 'center' }}>
          <Stack alignItems="center" spacing={1.5}><CircularProgress size={32} /><Typography color="text.secondary">Loading hiring pipeline…</Typography></Stack>
        </Box>
      )}

      {!loading && error && (
        <Alert severity="error" action={<Button color="inherit" size="small" onClick={() => loadPipeline(activeJobId)}>Retry</Button>}>
          {error}
        </Alert>
      )}

      {!loading && !error && applications.length === 0 && !hasJobFilter && (
        <Paper variant="outlined" sx={{ py: 8, px: 3, textAlign: 'center' }}>
          <Typography variant="h6">No applications yet</Typography>
          <Typography color="text.secondary" sx={{ mt: 1 }}>Applications for your company's jobs will appear here.</Typography>
        </Paper>
      )}

      {!loading && !error && applications.length === 0 && hasJobFilter && (
        <Paper variant="outlined" sx={{ py: 4, px: 3, mb: 2.5, textAlign: 'center' }}>
          <Typography variant="h6">No applications for this job</Typography>
          <Typography color="text.secondary" sx={{ mt: 1 }}>Choose another job or return to All jobs.</Typography>
          <Button variant="text" onClick={() => { setSelectedJobId(''); loadPipeline(); }} sx={{ mt: 1 }}>Show all jobs</Button>
        </Paper>
      )}

      {!loading && !error && (applications.length > 0 || hasJobFilter) && (
        <Box sx={{ overflowX: 'auto', pb: 1 }}>
          <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(5, minmax(210px, 1fr))', gap: 2, minWidth: 1120 }}>
            {columns.map((column) => (
              <Box key={column.title}>
                <Paper variant="outlined" sx={{ p: 1.5, minHeight: 390, bgcolor: '#f8fafc', borderTop: 3, borderTopColor: column.color }}>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1.5 }}>
                    <Typography variant="subtitle1" fontWeight="bold">{column.title}</Typography>
                    <Chip label={column.applications.length} size="small" sx={{ bgcolor: 'background.paper' }} />
                  </Box>
                  {column.applications.map((application) => (
                    <Card key={application.applicationId} sx={{ mb: 1.25, bgcolor: 'background.paper' }}>
                      <CardActionArea
                        onClick={() => navigate(`/hr/applications/${application.applicationId}`)}
                        aria-label={`Open candidate record for ${application.candidateName}`}
                      >
                        <CardContent sx={{ p: 2, '&:last-child': { pb: 2 } }}>
                          <Typography variant="body2" fontWeight={700} sx={{ mb: 0.5 }}>{application.candidateName}</Typography>
                          <Typography variant="caption" color="text.secondary">{application.jobTitle}</Typography>
                          <Chip label={getKanbanStatusLabel(application)} size="small" variant="outlined" sx={{ display: 'flex', width: 'fit-content', mt: 1.5 }} />
                          <Stack direction="row" alignItems="center" spacing={0.4} color="text.secondary" sx={{ mt: 1.25 }}>
                            <AccessTimeRoundedIcon sx={{ fontSize: 14 }} />
                            <Typography variant="caption">{formatKanbanTimestamp(application)}</Typography>
                          </Stack>
                        </CardContent>
                      </CardActionArea>
                    </Card>
                  ))}
                  {column.applications.length === 0 && (
                    <Box sx={{ py: 4, px: 2, textAlign: 'center', border: 1, borderStyle: 'dashed', borderColor: 'divider', borderRadius: 2, bgcolor: 'background.paper' }}>
                      <Typography variant="body2" color="text.secondary">No candidates in this stage</Typography>
                    </Box>
                  )}
                </Paper>
              </Box>
            ))}
          </Box>
        </Box>
      )}
    </PageSkeleton>
  );
}
