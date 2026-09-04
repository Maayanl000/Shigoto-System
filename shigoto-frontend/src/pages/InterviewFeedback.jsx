import { Alert, Button, Card, CardContent, Divider, FormControl, InputLabel, MenuItem, Select, TextField, Typography } from '@mui/material';
import PageSkeleton from '../components/PageSkeleton';

export default function InterviewFeedback() {
  return (
    <PageSkeleton title="Interview Feedback" description="Record structured feedback for an assigned interview.">
      <Card sx={{ maxWidth: 880 }}>
        <CardContent sx={{ display: 'grid', gap: 2.5 }}>
          <Alert severity="info" icon={false}>Open an assigned interview from your workspace to submit feedback.</Alert>
          <Typography variant="h6">Evaluation</Typography>
          <FormControl fullWidth disabled>
            <InputLabel id="recommendation-label">Recommendation</InputLabel>
            <Select labelId="recommendation-label" label="Recommendation" value="">
              <MenuItem value="advance">Advance</MenuItem>
              <MenuItem value="hold">Hold</MenuItem>
              <MenuItem value="decline">Do not advance</MenuItem>
            </Select>
          </FormControl>
          <Divider />
          <Typography variant="h6">Interview notes</Typography>
          <TextField label="Strengths" multiline minRows={3} disabled />
          <TextField label="Concerns" multiline minRows={3} disabled />
          <TextField label="Additional notes" multiline minRows={3} disabled />
          <Button variant="contained" disabled sx={{ justifySelf: 'start' }}>Submit feedback</Button>
        </CardContent>
      </Card>
    </PageSkeleton>
  );
}
