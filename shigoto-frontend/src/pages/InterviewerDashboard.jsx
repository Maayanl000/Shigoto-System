import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Avatar, Box, Button, Card, CardContent, Chip, Grid, Stack, Typography } from '@mui/material';
import CalendarMonthOutlinedIcon from '@mui/icons-material/CalendarMonthOutlined';
import ArrowForwardRoundedIcon from '@mui/icons-material/ArrowForwardRounded';
import RateReviewOutlinedIcon from '@mui/icons-material/RateReviewOutlined';
import PageSkeleton from '../components/PageSkeleton';
import FeedbackDialog from '../components/FeedbackDialog';

// Local frontend-only interview examples.
const mockInterviews = [
  { candidate: 'Maya Shalev', initials: 'MS', role: 'Full Stack Developer', round: 'Technical interview', time: 'Tomorrow · 10:00', status: 'Upcoming' },
  { candidate: 'Eitan Bar', initials: 'EB', role: 'Backend Engineer', round: 'System design', time: 'Thursday · 14:30', status: 'Upcoming' },
];

export default function InterviewerDashboard() {
  const [feedbackCandidate, setFeedbackCandidate] = useState(null);

  return (
    <PageSkeleton title="My Interviews" description="Review upcoming technical interviews and complete outstanding candidate feedback.">
      <Grid container spacing={2.5}>
        <Grid size={{ xs: 12, md: 8 }}>
          <Card>
            <CardContent>
              <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 2.5 }}>
                <Box><Typography variant="h6" component="h2">Upcoming interviews</Typography><Typography variant="caption" color="text.secondary">Local frontend schedule preview</Typography></Box>
                <Chip label="2 scheduled" color="secondary" variant="outlined" />
              </Stack>
              <Stack spacing={1.5}>
                {mockInterviews.map((interview) => (
                  <Box key={interview.candidate} sx={{ display: 'flex', flexDirection: { xs: 'column', sm: 'row' }, alignItems: { sm: 'center' }, gap: 2, p: 2, border: 1, borderColor: 'divider', borderRadius: 1.5 }}>
                    <Avatar sx={{ bgcolor: 'primary.main' }}>{interview.initials}</Avatar>
                    <Box sx={{ flex: 1 }}>
                      <Typography variant="body2" fontWeight={750}>{interview.candidate}</Typography>
                      <Typography variant="caption" color="text.secondary">{interview.role} · {interview.round}</Typography>
                    </Box>
                    <Stack direction="row" alignItems="center" spacing={0.75} color="text.secondary"><CalendarMonthOutlinedIcon fontSize="small" /><Typography variant="body2">{interview.time}</Typography></Stack>
                    <Button component={Link} to="/interviewer/interviews/demo" endIcon={<ArrowForwardRoundedIcon />}>Review</Button>
                  </Box>
                ))}
              </Stack>
            </CardContent>
          </Card>
        </Grid>

        <Grid size={{ xs: 12, md: 4 }}>
          <Card sx={{ height: '100%', borderColor: 'secondary.main' }}>
            <CardContent>
              <Box sx={{ display: 'grid', placeItems: 'center', width: 44, height: 44, borderRadius: 2, bgcolor: 'secondary.light', color: 'secondary.dark' }}><RateReviewOutlinedIcon /></Box>
              <Typography variant="h6" component="h2" sx={{ mt: 2.5 }}>Feedback requested</Typography>
              <Typography variant="h3" fontWeight={800} sx={{ mt: 1 }}>1</Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mt: 1, lineHeight: 1.7 }}>A completed interview is waiting for your review. This is local preview data.</Typography>
              <Button variant="contained" fullWidth sx={{ mt: 3 }} onClick={() => setFeedbackCandidate('Amit Mizrahi')}>Give feedback</Button>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
      <FeedbackDialog open={Boolean(feedbackCandidate)} onClose={() => setFeedbackCandidate(null)} candidateName={feedbackCandidate || undefined} />
    </PageSkeleton>
  );
}
