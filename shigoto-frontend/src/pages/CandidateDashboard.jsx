import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { Alert, Avatar, Box, Button, Card, CardActionArea, CardContent, Chip, CircularProgress, Grid, Stack, Tab, Tabs, Typography } from '@mui/material';
import ArrowForwardRoundedIcon from '@mui/icons-material/ArrowForwardRounded';
import AssignmentOutlinedIcon from '@mui/icons-material/AssignmentOutlined';
import EventAvailableOutlinedIcon from '@mui/icons-material/EventAvailableOutlined';
import HistoryRoundedIcon from '@mui/icons-material/HistoryRounded';
import ScheduleOutlinedIcon from '@mui/icons-material/ScheduleOutlined';
import WorkOutlineRoundedIcon from '@mui/icons-material/WorkOutlineRounded';
import PageSkeleton from '../components/PageSkeleton';
import api from '../services/api';
import { getApplicationStatusDisplay } from '../utils/applicationStatus';

const activeStatuses = new Set([
  'APPLIED', 'HR_INTERVIEW', 'TASK_SENT', 'TASK_SUBMITTED', 'TASK_APPROVED', 'TECH_INTERVIEW_SCHEDULED',
  'OFFER',
]);
const pastStatuses = new Set(['HIRED', 'REJECTED']);

function fetchCandidateDashboard() {
  return Promise.all([api.get('/applications/mine'), api.get('/interviews/mine'), api.get('/notifications/mine')]);
}

function formatDate(value, includeTime = false) {
  if (!value) return includeTime ? 'Date and time unavailable' : 'Date unavailable';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return includeTime ? 'Date and time unavailable' : 'Date unavailable';
  return new Intl.DateTimeFormat(undefined, includeTime
    ? { dateStyle: 'medium', timeStyle: 'short' }
    : { dateStyle: 'medium' }).format(date);
}

function EmptyState({ title, description }) {
  return (
    <Card variant="outlined">
      <CardContent sx={{ py: 5, textAlign: 'center' }}>
        <Typography variant="h6">{title}</Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>{description}</Typography>
      </CardContent>
    </Card>
  );
}

function ApplicationCard({ application, actionRequired = false }) {
  const status = getApplicationStatusDisplay(application.status);
  return (
    <Card sx={actionRequired ? { borderLeft: 4, borderLeftColor: 'warning.main' } : undefined}>
      <CardActionArea component={Link} to={`/candidate/applications/${application.id}`}>
        <CardContent>
          <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" alignItems={{ sm: 'flex-start' }} spacing={2}>
            <Box>
              {actionRequired && (
                <Stack direction="row" alignItems="center" spacing={0.75} sx={{ mb: 1 }}>
                  <AssignmentOutlinedIcon color="warning" fontSize="small" />
                  <Typography variant="overline" color="warning.dark" fontWeight={800}>Action required</Typography>
                </Stack>
              )}
              <Typography variant="h6" component="h3">{application.jobTitle}</Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
                {[application.companyName, application.location].filter(Boolean).join(' · ') || 'Job details unavailable'}
              </Typography>
              {actionRequired ? (
                <>
                  <Typography variant="body2" fontWeight={700} sx={{ mt: 1.5 }}>Submit your home task from the application details page.</Typography>
                  <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 0.75 }}>Due {formatDate(application.taskDeadline, true)}</Typography>
                </>
              ) : (
                <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 1.25 }}>Applied {formatDate(application.appliedAt)}</Typography>
              )}
            </Box>
            <Chip label={status.label} color={status.color} variant={status.color === 'default' ? 'outlined' : 'filled'} />
          </Stack>
          <Stack direction="row" alignItems="center" spacing={0.5} sx={{ mt: 2.5, color: 'primary.main' }}>
            <Typography variant="button">{actionRequired ? 'Open task' : 'View application'}</Typography>
            <ArrowForwardRoundedIcon fontSize="small" />
          </Stack>
        </CardContent>
      </CardActionArea>
    </Card>
  );
}

function InterviewCard({ interview, upcoming = false }) {
  return (
    <Card sx={upcoming ? { borderLeft: 4, borderLeftColor: 'success.main', bgcolor: 'action.hover' } : undefined}>
      <CardContent>
        <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" spacing={2}>
          <Box>
            {upcoming && (
              <Stack direction="row" alignItems="center" spacing={0.75} sx={{ mb: 1 }}>
                <EventAvailableOutlinedIcon color="success" fontSize="small" />
                <Typography variant="overline" color="success.dark" fontWeight={800}>Upcoming interview</Typography>
              </Stack>
            )}
            <Typography variant="h6" component="h3">{interview.jobTitle}</Typography>
            <Typography variant="body2" color="text.secondary">{interview.companyName}</Typography>
            <Typography variant="body2" fontWeight={700} sx={{ mt: 1.5 }}>{formatDate(interview.scheduledAt, true)}</Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
              {interview.type} interview{interview.interviewerName ? ` · ${interview.interviewerName}` : ''}
            </Typography>
          </Box>
          <Chip label={interview.status} color={upcoming ? 'success' : 'default'} variant={upcoming ? 'filled' : 'outlined'} />
        </Stack>
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1} sx={{ mt: 2.5 }}>
          <Button component={Link} to={`/candidate/applications/${interview.applicationId}`} endIcon={<ArrowForwardRoundedIcon />}>View application</Button>
          {upcoming && interview.meetingLink && (
            <Button component="a" href={interview.meetingLink} target="_blank" rel="noopener noreferrer" variant="contained">Join interview</Button>
          )}
        </Stack>
      </CardContent>
    </Card>
  );
}

export default function CandidateDashboard() {
  const [applications, setApplications] = useState([]);
  const [interviews, setInterviews] = useState([]);
  const [notifications, setNotifications] = useState([]);
  const [dashboardMode, setDashboardMode] = useState('active');
  const [selectedView, setSelectedView] = useState('active');
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState('');

  const loadDashboard = useCallback(async () => {
    setLoading(true);
    setLoadError('');
    try {
      const [applicationsResponse, interviewsResponse, notificationsResponse] = await fetchCandidateDashboard();
      setApplications(Array.isArray(applicationsResponse.data) ? applicationsResponse.data : []);
      setInterviews(Array.isArray(interviewsResponse.data) ? interviewsResponse.data : []);
      const loadedNotifications = Array.isArray(notificationsResponse.data) ? notificationsResponse.data : [];
      setNotifications(loadedNotifications);
      setDashboardMode(loadedNotifications.some((item) => !item.read) ? 'updates' : 'active');
    } catch (requestError) {
      setApplications([]);
      setInterviews([]);
      setLoadError(requestError.response?.data?.message || 'We could not load your dashboard.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    let active = true;
    fetchCandidateDashboard()
      .then(([applicationsResponse, interviewsResponse, notificationsResponse]) => {
        if (!active) return;
        setApplications(Array.isArray(applicationsResponse.data) ? applicationsResponse.data : []);
        setInterviews(Array.isArray(interviewsResponse.data) ? interviewsResponse.data : []);
        const loadedNotifications = Array.isArray(notificationsResponse.data) ? notificationsResponse.data : [];
        setNotifications(loadedNotifications);
        setDashboardMode(loadedNotifications.some((item) => !item.read) ? 'updates' : 'active');
      })
      .catch((requestError) => {
        if (!active) return;
        setApplications([]);
        setInterviews([]);
        setLoadError(requestError.response?.data?.message || 'We could not load your dashboard.');
      })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, []);

  const activeApplications = useMemo(() => applications.filter((item) => activeStatuses.has(item.status)), [applications]);
  const pendingTasks = useMemo(() => applications.filter((item) => item.status === 'TASK_SENT'), [applications]);
  const pastApplications = useMemo(() => applications.filter((item) => pastStatuses.has(item.status)), [applications]);
  const upcomingInterviews = useMemo(() => interviews.filter((item) => item.status === 'SCHEDULED'), [interviews]);
  const interviewHistory = useMemo(() => interviews.filter((item) => ['COMPLETED', 'CANCELED'].includes(item.status)), [interviews]);
  const unreadCount = notifications.filter((item) => !item.read).length;

  const openUpdate = async (notification) => {
    if (!notification.read) {
      await api.put(`/notifications/${notification.notificationId}/read`);
      setNotifications((items) => items.map((item) => item.notificationId === notification.notificationId ? { ...item, read: true } : item));
      window.dispatchEvent(new Event('shigoto:notifications-changed'));
    }
  };

  const summaryCards = [
    { id: 'active', label: 'Active applications', value: activeApplications.length, icon: <WorkOutlineRoundedIcon />, tone: 'primary' },
    { id: 'tasks', label: 'Pending tasks', value: pendingTasks.length, icon: <AssignmentOutlinedIcon />, tone: 'warning' },
    { id: 'interviews', label: 'Upcoming interviews', value: upcomingInterviews.length, icon: <ScheduleOutlinedIcon />, tone: 'success' },
  ];

  return (
    <PageSkeleton title="Candidate Dashboard" description="Track active applications, actions, interviews, and recruitment history.">
      <Grid container spacing={2.5}>
        <Grid size={12}>
          <Tabs value={dashboardMode} onChange={(_, value) => setDashboardMode(value)} aria-label="Candidate dashboard mode">
            <Tab value="updates" label={`Updates${unreadCount ? ` (${unreadCount})` : ''}`} />
            <Tab value="active" label="Active" />
            <Tab value="history" label="History" />
          </Tabs>
        </Grid>

        {dashboardMode === 'active' && summaryCards.map((item) => {
          const selected = selectedView === item.id;
          return (
            <Grid key={item.id} size={{ xs: 12, sm: 4 }}>
              <Card sx={{ height: '100%', border: 2, borderColor: selected ? `${item.tone}.main` : 'transparent', bgcolor: selected ? 'action.selected' : 'background.paper', transition: 'transform 160ms ease, border-color 160ms ease', '&:hover': { transform: 'translateY(-2px)' } }}>
                <CardActionArea onClick={() => setSelectedView(item.id)} aria-pressed={selected} sx={{ height: '100%' }}>
                  <CardContent sx={{ position: 'relative', pr: 9 }}>
                      <Box>
                        <Typography variant="body2" color="text.secondary">{item.label}</Typography>
                        <Typography variant="h3" fontWeight={800} sx={{ mt: 0.75 }}>{loading || loadError ? '—' : item.value}</Typography>
                      </Box>
                      <Avatar sx={{ position: 'absolute', top: 16, right: 16, bgcolor: `${item.tone}.light`, color: `${item.tone}.dark` }}>{item.icon}</Avatar>
                    {selected && <Typography variant="caption" fontWeight={800} sx={{ display: 'block', mt: 1.5 }}>Selected view</Typography>}
                  </CardContent>
                </CardActionArea>
              </Card>
            </Grid>
          );
        })}

        <Grid size={12}>
          {loading && <Card><CardContent sx={{ py: 7, display: 'grid', placeItems: 'center' }}><Stack alignItems="center" spacing={2}><CircularProgress size={30} /><Typography variant="body2" color="text.secondary">Loading your dashboard…</Typography></Stack></CardContent></Card>}
          {!loading && loadError && <Alert severity="error" action={<Button color="inherit" size="small" onClick={loadDashboard}>Retry</Button>}>{loadError}</Alert>}
          {!loading && !loadError && dashboardMode === 'active' && selectedView === 'active' && (
            <Stack spacing={2}>
              <Typography variant="h5">Active applications</Typography>
              {activeApplications.length ? activeApplications.map((item) => <ApplicationCard key={item.id} application={item} />)
                : <EmptyState title="No active applications" description="Your active applications will appear here." />}
            </Stack>
          )}
          {!loading && !loadError && dashboardMode === 'updates' && (
            <Stack spacing={2}>
              <Box><Typography variant="h5">What's new</Typography><Typography variant="body2" color="text.secondary" sx={{ mt: .5 }}>Unread updates appear first. Read state is saved to your account.</Typography></Box>
              {notifications.length ? notifications.map((item) => <Card key={item.notificationId} variant={item.read ? 'outlined' : undefined}
                sx={{ borderLeft: 4, borderLeftColor: item.read ? 'divider' : 'primary.main', bgcolor: item.read ? 'background.paper' : 'action.hover' }}>
                <CardActionArea component={Link} to={item.applicationId ? `/candidate/applications/${item.applicationId}` : '/candidate'} onClick={() => openUpdate(item)}>
                  <CardContent><Stack direction="row" justifyContent="space-between" spacing={2}><Box><Typography variant="h6">{item.title}{!item.read && <Typography component="span" variant="caption" fontWeight={900}> · New</Typography>}</Typography><Typography variant="body2" sx={{ mt: .75 }}>{item.message}</Typography><Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 1 }}>{formatDate(item.createdAt, true)}</Typography></Box><ArrowForwardRoundedIcon color="primary" /></Stack></CardContent>
                </CardActionArea>
              </Card>) : <EmptyState title="No updates yet" description="Recruitment updates will appear here when there is something new." />}
            </Stack>
          )}
          {!loading && !loadError && dashboardMode === 'active' && selectedView === 'tasks' && (
            <Stack spacing={2}>
              <Typography variant="h5">Pending tasks</Typography>
              {pendingTasks.length ? pendingTasks.map((item) => <ApplicationCard key={item.id} application={item} actionRequired />)
                : <EmptyState title="No pending tasks" description="You have no home task action to complete right now." />}
            </Stack>
          )}
          {!loading && !loadError && dashboardMode === 'active' && selectedView === 'interviews' && (
            <Stack spacing={2}>
              <Typography variant="h5">Upcoming interviews</Typography>
              {upcomingInterviews.length ? upcomingInterviews.map((item) => <InterviewCard key={item.id} interview={item} upcoming />)
                : <EmptyState title="No upcoming interviews" description="Scheduled interviews will appear here." />}
            </Stack>
          )}
          {!loading && !loadError && dashboardMode === 'history' && (
            <Stack spacing={2.5}>
            <Stack direction="row" alignItems="center" spacing={1} sx={{ mb: 2 }}>
              <HistoryRoundedIcon color="primary" />
              <Typography variant="h4" component="h2">History</Typography>
            </Stack>
            <Grid container spacing={2.5}>
              <Grid size={{ xs: 12, lg: 6 }}>
                <Stack spacing={2}>
                  <Typography variant="h5">Past applications</Typography>
                  {pastApplications.length ? pastApplications.map((item) => <ApplicationCard key={item.id} application={item} />)
                    : <EmptyState title="No past applications" description="Hired and rejected applications will remain available here." />}
                </Stack>
              </Grid>
              <Grid size={{ xs: 12, lg: 6 }}>
                <Stack spacing={2}>
                  <Typography variant="h5">Interview history</Typography>
                  {interviewHistory.length ? interviewHistory.map((item) => <InterviewCard key={item.id} interview={item} />)
                    : <EmptyState title="No interview history" description="Completed and canceled interviews will remain available here." />}
                </Stack>
              </Grid>
            </Grid>
            </Stack>
          )}
        </Grid>

        <Grid size={12}>
          <Card>
            <CardContent sx={{ display: 'flex', flexDirection: { xs: 'column', sm: 'row' }, alignItems: { sm: 'center' }, justifyContent: 'space-between', gap: 2 }}>
              <Box><Typography variant="h6">Explore another opportunity</Typography><Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>Browse current open jobs and apply to another role.</Typography></Box>
              <Button component={Link} to="/jobs" variant="contained">Browse jobs</Button>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </PageSkeleton>
  );
}
