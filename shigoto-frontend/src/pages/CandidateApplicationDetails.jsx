import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { Alert, Box, Button, Card, CardContent, Chip, CircularProgress, Grid, LinearProgress, Stack, Typography } from '@mui/material';
import ArrowBackRoundedIcon from '@mui/icons-material/ArrowBackRounded';
import AssignmentOutlinedIcon from '@mui/icons-material/AssignmentOutlined';
import CalendarMonthOutlinedIcon from '@mui/icons-material/CalendarMonthOutlined';
import PageSkeleton from '../components/PageSkeleton';
import api from '../services/api';
import { getApplicationStatusDisplay, recruitmentStages } from '../utils/applicationStatus';

const taskStatuses = new Set(['TASK_SENT', 'TASK_SUBMITTED', 'TASK_APPROVED']);
const interviewStatuses = new Set(['HR_INTERVIEW', 'TECH_INTERVIEW_SCHEDULED']);

function formatDateTime(value) {
  if (!value) return null;
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? null
    : new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(date);
}

export default function CandidateApplicationDetails() {
  const { applicationId } = useParams();
  const [application, setApplication] = useState(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);
  const [loadedApplicationId, setLoadedApplicationId] = useState(null);

  useEffect(() => {
    let isCurrent = true;

    api.get(`/applications/${applicationId}`)
      .then((response) => {
        if (!isCurrent) return;
        setApplication(response.data);
        setLoadError(null);
      })
      .catch((error) => {
        if (!isCurrent) return;
        setApplication(null);
        setLoadError(error.response?.status === 404 ? 'not-found' : 'error');
      })
      .finally(() => {
        if (!isCurrent) return;
        setLoadedApplicationId(applicationId);
        setLoading(false);
      });

    return () => {
      isCurrent = false;
    };
  }, [applicationId]);

  const isLoading = loading || loadedApplicationId !== applicationId;
  const status = application ? getApplicationStatusDisplay(application.status) : null;
  const hasTask = application && (Boolean(application.taskDeadline) || taskStatuses.has(application.status));
  const hasInterviewStage = application && interviewStatuses.has(application.status);
  const appliedAt = formatDateTime(application?.appliedAt);
  const taskDeadline = formatDateTime(application?.taskDeadline);
  const jobContext = application
    ? [application.companyName, application.location].filter(Boolean).join(' · ')
    : '';

  return (
    <PageSkeleton title="Application Details" description="Follow one application, its current recruitment stage, and assigned tasks.">
      <Button component={Link} to="/candidate" startIcon={<ArrowBackRoundedIcon />} sx={{ mb: 2.5, px: 0 }}>Back to Candidate Dashboard</Button>

      {isLoading && (
        <Card><CardContent sx={{ minHeight: 280, display: 'grid', placeItems: 'center' }}><Stack alignItems="center" spacing={2}><CircularProgress size={32} /><Typography variant="body2" color="text.secondary">Loading application details…</Typography></Stack></CardContent></Card>
      )}

      {!isLoading && loadError === 'not-found' && (
        <Alert severity="warning"><Typography fontWeight={700}>Application not found</Typography><Typography variant="body2">The requested application does not exist or is no longer available.</Typography></Alert>
      )}

      {!isLoading && loadError === 'error' && (
        <Alert severity="error">We could not load this application. Please try again later.</Alert>
      )}

      {!isLoading && application && (
        <Grid container spacing={2.5}>
          <Grid size={{ xs: 12, lg: 8 }}>
            <Stack spacing={2.5}>
              <Card>
                <CardContent>
                  <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" alignItems={{ sm: 'flex-start' }} spacing={2}>
                    <Box>
                      <Typography variant="overline" color="secondary.dark" fontWeight={800}>Application #{application.id}</Typography>
                      <Typography variant="h5" fontWeight={750} sx={{ mt: 0.5 }}>{application.jobTitle}</Typography>
                      <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>{jobContext || 'Job details unavailable'}</Typography>
                      <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 1.25 }}>{appliedAt ? `Applied ${appliedAt}` : 'Application date unavailable'}</Typography>
                    </Box>
                    <Chip label={status.label} color={status.color} variant={status.color === 'default' ? 'outlined' : 'filled'} />
                  </Stack>
                  <Box sx={{ mt: 4 }}>
                    <Stack direction="row" justifyContent="space-between" sx={{ mb: 1 }}><Typography variant="body2" fontWeight={700}>Application progress</Typography><Typography variant="caption" color="text.secondary">{status.progressStage} of 5 stages</Typography></Stack>
                    <LinearProgress variant="determinate" value={status.progressStage * 20} color={status.color === 'default' ? 'secondary' : status.color} sx={{ height: 7, borderRadius: 8 }} />
                    <Stack direction="row" justifyContent="space-between" sx={{ mt: 1 }}>
                      {recruitmentStages.map((stage, index) => <Typography key={stage} variant="caption" color={index < status.progressStage ? 'secondary.dark' : 'text.disabled'}>{stage}</Typography>)}
                    </Stack>
                  </Box>
                </CardContent>
              </Card>

              <Card>
                <CardContent>
                  <Typography variant="h6">Cover letter</Typography>
                  <Typography variant="body2" color="text.secondary" sx={{ mt: 2, lineHeight: 1.8, whiteSpace: 'pre-wrap' }}>{application.coverLetter?.trim() || 'No cover letter was submitted.'}</Typography>
                </CardContent>
              </Card>

              <Card sx={{ borderColor: hasTask ? 'secondary.main' : 'divider' }}>
                <CardContent>
                  <Stack direction="row" justifyContent="space-between" alignItems="center"><Stack direction="row" spacing={1} alignItems="center"><AssignmentOutlinedIcon color="secondary" /><Typography variant="h6">Technical home task</Typography></Stack><Chip label={hasTask ? 'Task stage' : 'Not assigned'} size="small" variant="outlined" /></Stack>
                  {!hasTask ? (
                    <Typography variant="body2" color="text.secondary" sx={{ mt: 2, lineHeight: 1.75 }}>No technical task has been assigned yet.</Typography>
                  ) : (
                    <Stack spacing={1.5} sx={{ mt: 2 }}>
                      <Typography variant="body2"><strong>Deadline:</strong> {taskDeadline || 'Deadline not available'}</Typography>
                      {application.taskRepoUrl ? (
                        <Box><Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>A repository submission is available.</Typography><Button component="a" href={application.taskRepoUrl} target="_blank" rel="noreferrer" variant="outlined">Open submitted repository</Button></Box>
                      ) : (
                        <Typography variant="body2" color="text.secondary">No repository submission has been made.</Typography>
                      )}
                    </Stack>
                  )}
                  <Box sx={{ mt: 2.5, p: 2, bgcolor: '#f8fafc', border: 1, borderStyle: 'dashed', borderColor: 'divider', borderRadius: 1.5 }}><Typography variant="caption" color="text.secondary">Task submission is not available yet.</Typography></Box>
                </CardContent>
              </Card>
            </Stack>
          </Grid>

          <Grid size={{ xs: 12, lg: 4 }}>
            <Stack spacing={2.5}>
              <Card>
                <CardContent>
                  <Stack direction="row" spacing={1} alignItems="center"><CalendarMonthOutlinedIcon color="secondary" /><Typography variant="h6">Interview</Typography></Stack>
                  <Chip label={hasInterviewStage ? 'Details unavailable' : 'Not scheduled'} size="small" variant="outlined" sx={{ mt: 2 }} />
                  <Typography variant="body2" color="text.secondary" sx={{ mt: 1.5 }}>{hasInterviewStage ? 'The application is at an interview stage, but scheduling information is not included in the current application data.' : 'No interview information is currently available for this application.'}</Typography>
                </CardContent>
              </Card>

              <Card>
                <CardContent>
                  <Typography variant="h6">Application summary</Typography>
                  <Stack spacing={1.25} sx={{ mt: 2 }}>
                    <Typography variant="body2"><strong>Application ID:</strong> {application.id}</Typography>
                    <Typography variant="body2"><strong>Job ID:</strong> {application.jobId}</Typography>
                    <Typography variant="body2"><strong>Status:</strong> {status.label}</Typography>
                    <Typography variant="body2"><strong>Applied:</strong> {appliedAt || 'Date unavailable'}</Typography>
                  </Stack>
                </CardContent>
              </Card>
            </Stack>
          </Grid>
        </Grid>
      )}
    </PageSkeleton>
  );
}
