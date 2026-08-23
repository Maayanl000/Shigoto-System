import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Alert, Avatar, Box, Button, Card, CardContent, Chip, CircularProgress, Grid, LinearProgress, Stack, Typography } from '@mui/material';
import ArrowForwardRoundedIcon from '@mui/icons-material/ArrowForwardRounded';
import AssignmentOutlinedIcon from '@mui/icons-material/AssignmentOutlined';
import WorkOutlineRoundedIcon from '@mui/icons-material/WorkOutlineRounded';
import ScheduleOutlinedIcon from '@mui/icons-material/ScheduleOutlined';
import PageSkeleton from '../components/PageSkeleton';
import api from '../services/api';
import { getApplicationStatusDisplay, recruitmentStages } from '../utils/applicationStatus';

// TODO: Replace this development ID with the authenticated current user's ID when authentication is implemented.
const DEVELOPMENT_CANDIDATE_ID = 2;

const inactiveStatuses = new Set(['REJECTED', 'OFFER']);

function formatAppliedDate(value) {
  if (!value) return 'Date unavailable';
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? 'Date unavailable'
    : new Intl.DateTimeFormat(undefined, { dateStyle: 'medium' }).format(date);
}

export default function CandidateDashboard() {
  const [applications, setApplications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(false);

  useEffect(() => {
    let isCurrent = true;

    api.get(`/applications/candidate/${DEVELOPMENT_CANDIDATE_ID}`)
      .then((response) => {
        if (!isCurrent) return;
        setApplications(Array.isArray(response.data) ? response.data : []);
        setLoadError(false);
      })
      .catch(() => {
        if (!isCurrent) return;
        setApplications([]);
        setLoadError(true);
      })
      .finally(() => {
        if (isCurrent) setLoading(false);
      });

    return () => {
      isCurrent = false;
    };
  }, []);

  const activeApplicationCount = applications.filter((application) => !inactiveStatuses.has(application.status)).length;

  return (
    <PageSkeleton title="Candidate Dashboard" description="Track active applications, current recruitment status, and pending tasks.">
      <Grid container spacing={2.5}>
        {[
          { label: 'Active applications', value: loading || loadError ? '—' : activeApplicationCount, icon: <WorkOutlineRoundedIcon />, tone: 'primary' },
          { label: 'Pending tasks · Preview', value: '—', icon: <AssignmentOutlinedIcon />, tone: 'secondary' },
          { label: 'Upcoming interviews · Preview', value: '—', icon: <ScheduleOutlinedIcon />, tone: 'primary' },
        ].map((item) => (
          <Grid key={item.label} size={{ xs: 12, sm: 4 }}>
            <Card sx={{ height: '100%' }}>
              <CardContent>
                <Stack direction="row" justifyContent="space-between" alignItems="center">
                  <Box>
                    <Typography variant="body2" color="text.secondary">{item.label}</Typography>
                    <Typography variant="h3" fontWeight={800} sx={{ mt: 0.75 }}>{item.value}</Typography>
                  </Box>
                  <Avatar sx={{ bgcolor: `${item.tone}.light`, color: `${item.tone}.main` }}>{item.icon}</Avatar>
                </Stack>
              </CardContent>
            </Card>
          </Grid>
        ))}

        <Grid size={{ xs: 12, lg: 8 }}>
          <Stack spacing={2.5}>
            {loading && (
              <Card><CardContent sx={{ minHeight: 220, display: 'grid', placeItems: 'center' }}><Stack alignItems="center" spacing={2}><CircularProgress size={30} /><Typography variant="body2" color="text.secondary">Loading your applications…</Typography></Stack></CardContent></Card>
            )}

            {!loading && loadError && (
              <Alert severity="error">We could not load your applications. Please try again later.</Alert>
            )}

            {!loading && !loadError && applications.length === 0 && (
              <Card><CardContent sx={{ py: 6, textAlign: 'center' }}><Typography variant="h6">No applications yet</Typography><Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>Browse open positions when you are ready to apply.</Typography><Button component={Link} to="/jobs" variant="contained" sx={{ mt: 3 }}>Browse jobs</Button></CardContent></Card>
            )}

            {!loading && !loadError && applications.map((application) => {
              const display = getApplicationStatusDisplay(application.status);
              const jobContext = [application.companyName, application.location].filter(Boolean).join(' · ');

              return (
                <Card key={application.id}>
                  <CardContent>
                    <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" alignItems={{ sm: 'flex-start' }} spacing={2}>
                      <Box>
                        <Typography variant="overline" color="secondary.dark" fontWeight={800}>Application #{application.id}</Typography>
                        <Typography variant="h5" component="h2" fontWeight={750} sx={{ mt: 0.5 }}>{application.jobTitle}</Typography>
                        <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>{jobContext || 'Job details unavailable'}</Typography>
                        <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 1.25 }}>Applied {formatAppliedDate(application.appliedAt)}</Typography>
                      </Box>
                      <Chip label={display.label} color={display.color} variant={display.color === 'default' ? 'outlined' : 'filled'} />
                    </Stack>
                    <Box sx={{ mt: 4 }}>
                      <Stack direction="row" justifyContent="space-between" sx={{ mb: 1 }}>
                        <Typography variant="body2" fontWeight={700}>Recruitment progress</Typography>
                        <Typography variant="caption" color="text.secondary">{display.progressStage} of 5 stages</Typography>
                      </Stack>
                      <LinearProgress variant="determinate" value={display.progressStage * 20} color={display.color === 'default' ? 'secondary' : display.color} sx={{ height: 7, borderRadius: 8 }} />
                      <Stack direction="row" justifyContent="space-between" sx={{ mt: 1 }}>
                        {recruitmentStages.map((stage, index) => (
                          <Typography key={stage} variant="caption" color={index < display.progressStage ? 'secondary.dark' : 'text.disabled'}>{stage}</Typography>
                        ))}
                      </Stack>
                    </Box>
                    <Button component={Link} to={`/candidate/applications/${application.id}`} endIcon={<ArrowForwardRoundedIcon />} sx={{ mt: 3, px: 0 }}>View application details</Button>
                  </CardContent>
                </Card>
              );
            })}
          </Stack>
        </Grid>

        <Grid size={{ xs: 12, lg: 4 }}>
          <Card sx={{ height: '100%', borderColor: 'secondary.main' }}>
            <CardContent>
              <Stack direction="row" justifyContent="space-between" alignItems="center">
                <Typography variant="h6" component="h2">Pending task</Typography>
                <Chip label="Preview" size="small" variant="outlined" />
              </Stack>
              <Typography fontWeight={700} sx={{ mt: 3 }}>Technical home task</Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mt: 1, lineHeight: 1.7 }}>Task instructions and a due date will appear here once assigned by HR.</Typography>
              <Box sx={{ mt: 3, p: 1.5, bgcolor: '#f8fafc', borderRadius: 1.5 }}>
                <Typography variant="caption" color="text.secondary">Submission controls are intentionally unavailable.</Typography>
              </Box>
              <Button variant="outlined" disabled fullWidth sx={{ mt: 2 }}>Open task</Button>
            </CardContent>
          </Card>
        </Grid>

        <Grid size={12}>
          <Card>
            <CardContent sx={{ display: 'flex', flexDirection: { xs: 'column', sm: 'row' }, alignItems: { sm: 'center' }, justifyContent: 'space-between', gap: 2 }}>
              <Box><Typography variant="h6">Explore another opportunity</Typography><Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>Browse current backend jobs and apply to a real open position.</Typography></Box>
              <Button component={Link} to="/jobs" variant="contained">Apply to a role</Button>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </PageSkeleton>
  );
}
