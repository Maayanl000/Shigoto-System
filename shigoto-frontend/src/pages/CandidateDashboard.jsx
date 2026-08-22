import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Avatar, Box, Button, Card, CardContent, Chip, Grid, LinearProgress, Stack, Typography } from '@mui/material';
import ArrowForwardRoundedIcon from '@mui/icons-material/ArrowForwardRounded';
import AssignmentOutlinedIcon from '@mui/icons-material/AssignmentOutlined';
import WorkOutlineRoundedIcon from '@mui/icons-material/WorkOutlineRounded';
import ScheduleOutlinedIcon from '@mui/icons-material/ScheduleOutlined';
import PageSkeleton from '../components/PageSkeleton';
import ApplicationDialog from '../components/ApplicationDialog';

// Local frontend-only data used to communicate the planned candidate experience.
const mockApplication = {
  title: 'Frontend Developer (React)',
  department: 'UI/UX',
  location: 'Remote',
  type: 'Full-time',
  stage: 'Technical review',
};

export default function CandidateDashboard() {
  const [applyOpen, setApplyOpen] = useState(false);

  return (
    <PageSkeleton title="Candidate Dashboard" description="Track active applications, current recruitment status, and pending tasks.">
      <Grid container spacing={2.5}>
        {[
          { label: 'Active applications', value: '1', icon: <WorkOutlineRoundedIcon />, tone: 'primary' },
          { label: 'Pending tasks', value: '1', icon: <AssignmentOutlinedIcon />, tone: 'secondary' },
          { label: 'Upcoming interviews', value: '0', icon: <ScheduleOutlinedIcon />, tone: 'primary' },
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
          <Card sx={{ height: '100%' }}>
            <CardContent>
              <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" alignItems={{ sm: 'flex-start' }} spacing={2}>
                <Box>
                  <Typography variant="overline" color="secondary.dark" fontWeight={800}>Active application · Local mock</Typography>
                  <Typography variant="h5" component="h2" fontWeight={750} sx={{ mt: 0.5 }}>{mockApplication.title}</Typography>
                  <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>{mockApplication.department} · {mockApplication.location}</Typography>
                </Box>
                <Chip label={mockApplication.stage} color="secondary" sx={{ bgcolor: 'secondary.light', color: 'secondary.dark' }} />
              </Stack>
              <Box sx={{ mt: 4 }}>
                <Stack direction="row" justifyContent="space-between" sx={{ mb: 1 }}>
                  <Typography variant="body2" fontWeight={700}>Recruitment progress</Typography>
                  <Typography variant="caption" color="text.secondary">2 of 5 stages</Typography>
                </Stack>
                <LinearProgress variant="determinate" value={40} color="secondary" sx={{ height: 7, borderRadius: 8 }} />
                <Stack direction="row" justifyContent="space-between" sx={{ mt: 1 }}>
                  {['Applied', 'Review', 'Task', 'Interview', 'Decision'].map((stage, index) => (
                    <Typography key={stage} variant="caption" color={index < 2 ? 'secondary.dark' : 'text.disabled'}>{stage}</Typography>
                  ))}
                </Stack>
              </Box>
              <Button component={Link} to="/candidate/applications/demo" endIcon={<ArrowForwardRoundedIcon />} sx={{ mt: 3, px: 0 }}>View application details</Button>
            </CardContent>
          </Card>
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
              <Box><Typography variant="h6">Explore another opportunity</Typography><Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>Open the candidate application dialog using local preview job data.</Typography></Box>
              <Button variant="contained" onClick={() => setApplyOpen(true)}>Apply to a role</Button>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
      <ApplicationDialog open={applyOpen} onClose={() => setApplyOpen(false)} job={mockApplication} />
    </PageSkeleton>
  );
}
