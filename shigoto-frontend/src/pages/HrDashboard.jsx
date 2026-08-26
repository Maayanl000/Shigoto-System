import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Alert, Box, Button, Card, CardActionArea, CardContent, Chip, CircularProgress, Divider, Paper, Stack, Typography } from '@mui/material';
import AccessTimeRoundedIcon from '@mui/icons-material/AccessTimeRounded';
import PageSkeleton from '../components/PageSkeleton';
import api from '../services/api';

const columnDefinitions = [
  { title: 'Applied', color: '#64748b', statuses: ['APPLIED'] },
  { title: 'Screening', color: '#2563eb', statuses: ['HR_INTERVIEW'] },
  { title: 'Task', color: '#7c3aed', statuses: ['TASK_SENT', 'TASK_SUBMITTED', 'TASK_APPROVED'] },
  { title: 'Interview', color: '#087f8c', statuses: ['TECH_INTERVIEW_SCHEDULED'] },
  { title: 'Decision', color: '#d97706', statuses: ['OFFER', 'REJECTED'] },
];

const statusLabels = {
  APPLIED: 'Applied',
  HR_INTERVIEW: 'HR interview',
  TASK_SENT: 'Task sent',
  TASK_SUBMITTED: 'Task submitted',
  TASK_APPROVED: 'Task approved',
  TECH_INTERVIEW_SCHEDULED: 'Technical interview scheduled',
  OFFER: 'Offer',
  REJECTED: 'Rejected',
};

function formatAppliedAt(value) {
  if (!value) return 'Date unavailable';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return 'Date unavailable';
  return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium' }).format(date);
}

export default function HrDashboard() {
  const navigate = useNavigate();
  const [applications, setApplications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadApplications = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const response = await api.get('/applications');
      setApplications(response.data);
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Could not load the hiring pipeline.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    let active = true;
    api.get('/applications')
      .then((response) => {
        if (active) setApplications(response.data);
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

  return (
    <PageSkeleton title="HR Dashboard" description="Review your company's applications across the hiring pipeline.">
      <Paper variant="outlined" sx={{ display: 'flex', alignItems: 'center', flexWrap: 'wrap', gap: 2, p: 2, mb: 2.5 }}>
        <Box>
          <Typography variant="caption" color="text.secondary">Pipeline view</Typography>
          <Typography variant="body2" fontWeight={700}>All company applications</Typography>
        </Box>
        <Divider orientation="vertical" flexItem />
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
        <Alert severity="error" action={<Button color="inherit" size="small" onClick={loadApplications}>Retry</Button>}>
          {error}
        </Alert>
      )}

      {!loading && !error && applications.length === 0 && (
        <Paper variant="outlined" sx={{ py: 8, px: 3, textAlign: 'center' }}>
          <Typography variant="h6">No applications yet</Typography>
          <Typography color="text.secondary" sx={{ mt: 1 }}>Applications for your company's jobs will appear here.</Typography>
        </Paper>
      )}

      {!loading && !error && applications.length > 0 && (
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
                          <Chip label={statusLabels[application.status] || application.status} size="small" variant="outlined" sx={{ display: 'flex', width: 'fit-content', mt: 1.5 }} />
                          <Stack direction="row" alignItems="center" spacing={0.4} color="text.secondary" sx={{ mt: 1.25 }}>
                            <AccessTimeRoundedIcon sx={{ fontSize: 14 }} />
                            <Typography variant="caption">Applied {formatAppliedAt(application.appliedAt)}</Typography>
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
