import { useState } from 'react';
import { Box, Button, Card, CardContent, Chip, Grid, Stack, TextField, Typography } from '@mui/material';
import AddRoundedIcon from '@mui/icons-material/AddRounded';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import PeopleOutlineRoundedIcon from '@mui/icons-material/PeopleOutlineRounded';
import PageSkeleton from '../components/PageSkeleton';
import ActionDialog from '../components/ActionDialog';

// Local frontend-only records for the job management layout.
const mockJobs = [
  { title: 'Frontend Developer (React)', department: 'Engineering', location: 'Remote', status: 'Open', candidates: 12 },
  { title: 'Backend Engineer (Spring Boot)', department: 'Engineering', location: 'Hybrid', status: 'Open', candidates: 8 },
  { title: 'Product Designer', department: 'Design', location: 'Tel Aviv', status: 'Draft', candidates: 0 },
];

export default function JobManagement() {
  const [dialog, setDialog] = useState(null);
  const [selectedJob, setSelectedJob] = useState(null);

  const openEdit = (job) => {
    setSelectedJob(job);
    setDialog('edit');
  };

  return (
    <PageSkeleton title="Job Management" description="Create and maintain technology openings and their hiring teams.">
      <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" alignItems={{ sm: 'center' }} spacing={2} sx={{ mb: 2.5 }}>
        <Stack direction="row" spacing={1}>
          <Chip label="2 open" color="secondary" variant="outlined" />
          <Chip label="1 draft" variant="outlined" />
          <Chip label="Local mock records" variant="outlined" />
        </Stack>
        <Button variant="contained" startIcon={<AddRoundedIcon />} onClick={() => setDialog('create')}>Create job</Button>
      </Stack>

      <Grid container spacing={2}>
        {mockJobs.map((job) => (
          <Grid key={job.title} size={{ xs: 12, lg: 4 }}>
            <Card sx={{ height: '100%' }}>
              <CardContent>
                <Stack direction="row" justifyContent="space-between" alignItems="flex-start" spacing={2}>
                  <Chip label={job.status} size="small" color={job.status === 'Open' ? 'secondary' : 'default'} variant="outlined" />
                  <Button size="small" startIcon={<EditOutlinedIcon />} onClick={() => openEdit(job)}>Edit</Button>
                </Stack>
                <Typography variant="h6" component="h2" sx={{ mt: 2 }}>{job.title}</Typography>
                <Typography variant="body2" color="text.secondary" sx={{ mt: 0.75 }}>{job.department} · {job.location}</Typography>
                <Box sx={{ mt: 3, pt: 2, borderTop: 1, borderColor: 'divider' }}>
                  <Stack direction="row" alignItems="center" spacing={1} color="text.secondary">
                    <PeopleOutlineRoundedIcon fontSize="small" />
                    <Typography variant="body2">{job.candidates} candidates</Typography>
                  </Stack>
                </Box>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>

      <ActionDialog open={dialog === 'create'} onClose={() => setDialog(null)} title="Create job" description="Set up a new technology opening." confirmLabel="Create job">
        <TextField label="Job title" disabled fullWidth />
        <Grid container spacing={2}>
          <Grid size={{ xs: 12, sm: 6 }}><TextField label="Department" disabled fullWidth /></Grid>
          <Grid size={{ xs: 12, sm: 6 }}><TextField label="Location" disabled fullWidth /></Grid>
        </Grid>
        <TextField label="Role description" multiline minRows={4} disabled fullWidth />
      </ActionDialog>

      <ActionDialog open={dialog === 'edit'} onClose={() => setDialog(null)} title="Edit job" description={selectedJob?.title || 'Selected position'} confirmLabel="Save changes">
        <TextField label="Job title" value={selectedJob?.title || ''} disabled fullWidth />
        <Grid container spacing={2}>
          <Grid size={{ xs: 12, sm: 6 }}><TextField label="Department" value={selectedJob?.department || ''} disabled fullWidth /></Grid>
          <Grid size={{ xs: 12, sm: 6 }}><TextField label="Location" value={selectedJob?.location || ''} disabled fullWidth /></Grid>
        </Grid>
        <TextField label="Role description" multiline minRows={4} disabled fullWidth />
      </ActionDialog>
    </PageSkeleton>
  );
}
