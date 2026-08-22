import { Button, Card, CardContent, TextField, Typography } from '@mui/material';
import PageSkeleton from '../components/PageSkeleton';

export default function ApplicationForm() {
  return (
    <PageSkeleton title="Job Application" description="Application form skeleton for a selected open position.">
      <Card variant="outlined">
        <CardContent sx={{ display: 'grid', gap: 2 }}>
          <Typography variant="h6">Selected Position</Typography>
          <Typography color="text.secondary">Job title and description will be loaded here.</Typography>
          <Typography variant="h6" sx={{ mt: 1 }}>Candidate Information</Typography>
          <TextField label="Full name" disabled />
          <TextField label="Email" disabled />
          <TextField label="Cover note" multiline minRows={4} disabled />
          <Typography variant="h6" sx={{ mt: 1 }}>Documents</Typography>
          <Typography color="text.secondary">Resume attachment controls will be added later. File upload is not active.</Typography>
          <Button variant="contained" disabled sx={{ justifySelf: 'start' }}>Submit application</Button>
        </CardContent>
      </Card>
    </PageSkeleton>
  );
}
