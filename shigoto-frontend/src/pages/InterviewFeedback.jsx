import { Button, Card, CardContent, FormControl, InputLabel, MenuItem, Select, TextField, Typography } from '@mui/material';
import PageSkeleton from '../components/PageSkeleton';

export default function InterviewFeedback() {
  return (
    <PageSkeleton title="Interview Feedback" description="Feedback form skeleton. Responses are not saved or submitted yet.">
      <Card variant="outlined">
        <CardContent sx={{ display: 'grid', gap: 2 }}>
          <Typography variant="h6">Evaluation</Typography>
          <FormControl fullWidth disabled>
            <InputLabel id="recommendation-label">Recommendation</InputLabel>
            <Select labelId="recommendation-label" label="Recommendation" value="">
              <MenuItem value="advance">Advance</MenuItem>
              <MenuItem value="hold">Hold</MenuItem>
              <MenuItem value="decline">Do not advance</MenuItem>
            </Select>
          </FormControl>
          <TextField label="Strengths" multiline minRows={3} disabled />
          <TextField label="Concerns" multiline minRows={3} disabled />
          <TextField label="Additional notes" multiline minRows={3} disabled />
          <Button variant="contained" disabled sx={{ justifySelf: 'start' }}>Submit feedback</Button>
        </CardContent>
      </Card>
    </PageSkeleton>
  );
}
