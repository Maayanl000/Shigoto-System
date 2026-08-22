import { useState } from 'react';
import { Avatar, Box, Button, Card, CardContent, Chip, Grid, List, ListItem, ListItemText, Stack, Typography } from '@mui/material';
import CalendarMonthOutlinedIcon from '@mui/icons-material/CalendarMonthOutlined';
import RateReviewOutlinedIcon from '@mui/icons-material/RateReviewOutlined';
import PageSkeleton from '../components/PageSkeleton';
import FeedbackDialog from '../components/FeedbackDialog';

export default function InterviewDetails() {
  const [feedbackOpen, setFeedbackOpen] = useState(false);

  return (
    <PageSkeleton title="Interview Details" description="Review the candidate, role expectations, and interview guide before recording feedback.">
      <Grid container spacing={2.5}>
        <Grid size={{ xs: 12, lg: 8 }}>
          <Stack spacing={2.5}>
            <Card>
              <CardContent>
                <Stack direction={{ xs: 'column', sm: 'row' }} alignItems={{ sm: 'center' }} spacing={2}>
                  <Avatar sx={{ width: 58, height: 58, bgcolor: 'primary.main' }}>MS</Avatar>
                  <Box sx={{ flex: 1 }}><Typography variant="h5" fontWeight={750}>Maya Shalev</Typography><Typography variant="body2" color="text.secondary">Full Stack Developer · Local candidate preview</Typography></Box>
                  <Chip label="Technical interview" color="secondary" variant="outlined" />
                </Stack>
              </CardContent>
            </Card>
            <Card>
              <CardContent>
                <Typography variant="h6" component="h2">Candidate snapshot</Typography>
                <Typography variant="body2" color="text.secondary" sx={{ mt: 1.5, lineHeight: 1.75 }}>Professional summary, resume highlights, application notes, and completed home-task references will appear here when candidate data is connected.</Typography>
              </CardContent>
            </Card>
            <Card>
              <CardContent>
                <Typography variant="h6" component="h2">Interview guide</Typography>
                <List dense sx={{ mt: 1 }}>
                  {['Discuss recent React and Java project experience', 'Review system design and technical trade-offs', 'Leave time for candidate questions'].map((item, index) => (
                    <ListItem key={item} disableGutters><ListItemText primary={`${index + 1}. ${item}`} secondary="Suggested preview topic" /></ListItem>
                  ))}
                </List>
              </CardContent>
            </Card>
          </Stack>
        </Grid>
        <Grid size={{ xs: 12, lg: 4 }}>
          <Stack spacing={2.5}>
            <Card>
              <CardContent>
                <Stack direction="row" spacing={1} alignItems="center"><CalendarMonthOutlinedIcon color="secondary" /><Typography variant="h6">Interview overview</Typography></Stack>
                <Typography variant="body2" fontWeight={700} sx={{ mt: 2 }}>Tomorrow · 10:00–11:00</Typography>
                <Typography variant="body2" color="text.secondary" sx={{ mt: 0.75 }}>Scheduling integration is not connected.</Typography>
              </CardContent>
            </Card>
            <Card sx={{ borderColor: 'secondary.main' }}>
              <CardContent>
                <RateReviewOutlinedIcon color="secondary" />
                <Typography variant="h6" sx={{ mt: 1.5 }}>Ready after the interview?</Typography>
                <Typography variant="body2" color="text.secondary" sx={{ mt: 1, lineHeight: 1.7 }}>Open the feedback dialog to preview the planned evaluation form.</Typography>
                <Button variant="contained" fullWidth sx={{ mt: 2.5 }} onClick={() => setFeedbackOpen(true)}>Give feedback</Button>
              </CardContent>
            </Card>
          </Stack>
        </Grid>
      </Grid>
      <FeedbackDialog open={feedbackOpen} onClose={() => setFeedbackOpen(false)} candidateName="Maya Shalev" />
    </PageSkeleton>
  );
}
