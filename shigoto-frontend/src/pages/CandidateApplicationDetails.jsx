import { Box, Button, Card, CardContent, Chip, Grid, LinearProgress, Stack, Typography } from '@mui/material';
import AssignmentOutlinedIcon from '@mui/icons-material/AssignmentOutlined';
import CalendarMonthOutlinedIcon from '@mui/icons-material/CalendarMonthOutlined';
import CheckCircleOutlineRoundedIcon from '@mui/icons-material/CheckCircleOutlineRounded';
import PageSkeleton from '../components/PageSkeleton';

export default function CandidateApplicationDetails() {
  return (
    <PageSkeleton title="Application Details" description="Follow one application, its current recruitment stage, and assigned tasks.">
      <Grid container spacing={2.5}>
        <Grid size={{ xs: 12, lg: 8 }}>
          <Stack spacing={2.5}>
            <Card>
              <CardContent>
                <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" alignItems={{ sm: 'flex-start' }} spacing={2}>
                  <Box><Typography variant="overline" color="secondary.dark" fontWeight={800}>Local application preview</Typography><Typography variant="h5" fontWeight={750} sx={{ mt: 0.5 }}>Frontend Developer (React)</Typography><Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>UI/UX · Remote · Full-time</Typography></Box>
                  <Chip label="Technical review" color="secondary" sx={{ bgcolor: 'secondary.light', color: 'secondary.dark' }} />
                </Stack>
                <Box sx={{ mt: 4 }}>
                  <Stack direction="row" justifyContent="space-between" sx={{ mb: 1 }}><Typography variant="body2" fontWeight={700}>Application progress</Typography><Typography variant="caption" color="text.secondary">2 of 5 stages</Typography></Stack>
                  <LinearProgress variant="determinate" value={40} color="secondary" sx={{ height: 7, borderRadius: 8 }} />
                </Box>
              </CardContent>
            </Card>
            <Card sx={{ borderColor: 'secondary.main' }}>
              <CardContent>
                <Stack direction="row" justifyContent="space-between" alignItems="center"><Stack direction="row" spacing={1} alignItems="center"><AssignmentOutlinedIcon color="secondary" /><Typography variant="h6">Technical home task</Typography></Stack><Chip label="Not assigned" size="small" variant="outlined" /></Stack>
                <Typography variant="body2" color="text.secondary" sx={{ mt: 2, lineHeight: 1.75 }}>Assessment instructions, repository details, and a due date will appear here when HR assigns a task.</Typography>
                <Box sx={{ mt: 2.5, p: 2, bgcolor: '#f8fafc', border: 1, borderStyle: 'dashed', borderColor: 'divider', borderRadius: 1.5 }}><Typography variant="caption" color="text.secondary">Task submission and file upload are intentionally unavailable.</Typography></Box>
                <Button variant="contained" disabled sx={{ mt: 2.5 }}>Open task</Button>
              </CardContent>
            </Card>
          </Stack>
        </Grid>
        <Grid size={{ xs: 12, lg: 4 }}>
          <Stack spacing={2.5}>
            <Card><CardContent><Stack direction="row" spacing={1} alignItems="center"><CalendarMonthOutlinedIcon color="secondary" /><Typography variant="h6">Interview</Typography></Stack><Chip label="Not scheduled" size="small" variant="outlined" sx={{ mt: 2 }} /><Typography variant="body2" color="text.secondary" sx={{ mt: 1.5 }}>Interview information will appear here when available.</Typography></CardContent></Card>
            <Card><CardContent><Typography variant="h6">Activity</Typography><Stack spacing={2} sx={{ mt: 2 }}>{['Application received', 'Moved to technical review'].map((event) => <Stack key={event} direction="row" spacing={1.5}><CheckCircleOutlineRoundedIcon color="secondary" fontSize="small" /><Box><Typography variant="body2" fontWeight={700}>{event}</Typography><Typography variant="caption" color="text.secondary">Preview timeline event</Typography></Box></Stack>)}</Stack></CardContent></Card>
          </Stack>
        </Grid>
      </Grid>
    </PageSkeleton>
  );
}
