import { Alert, Button, Card, CardContent, Divider, FormControl, InputLabel, MenuItem, Select, TextField, Typography } from '@mui/material';
import PageSkeleton from '../components/PageSkeleton';

export default function InterviewFeedback() {
  return (
    <PageSkeleton title="Interview Feedback" description="Feedback form skeleton. Responses are not saved or submitted yet.">
      <Card sx={{ maxWidth: 880 }}>
        <CardContent sx={{ display: 'grid', gap: 2.5 }}>
          <Alert severity="info" icon={false}>Preview only — interview feedback cannot be saved or submitted yet.</Alert>
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
