import { Alert, Button, Card, CardContent, Divider, Grid, TextField, Typography } from '@mui/material';
import PageSkeleton from '../components/PageSkeleton';

export default function ApplicationForm() {
  return (
    <PageSkeleton title="Job Application" description="Apply for a selected open position.">
      <Card sx={{ maxWidth: 880 }}>
        <CardContent sx={{ display: 'grid', gap: 2.5 }}>
          <Alert severity="info" icon={false}>Choose an open job from the Jobs page to submit an application.</Alert>
          <Typography variant="h6">Selected Position</Typography>
          <Typography color="text.secondary">Job title, team, location, and description will be loaded here.</Typography>
          <Divider />
          <Typography variant="h6">Candidate Information</Typography>
          <Grid container spacing={2}>
            <Grid size={{ xs: 12, md: 6 }}><TextField label="Full name" disabled fullWidth /></Grid>
            <Grid size={{ xs: 12, md: 6 }}><TextField label="Email" disabled fullWidth /></Grid>
          </Grid>
          <TextField label="Cover note" multiline minRows={4} disabled />
          <Divider />
          <Typography variant="h6">Documents</Typography>
          <Typography color="text.secondary">Resume attachment controls will be added later. File upload is not active.</Typography>
          <Button variant="contained" disabled sx={{ justifySelf: 'start' }}>Submit application</Button>
        </CardContent>
      </Card>
    </PageSkeleton>
  );
}
