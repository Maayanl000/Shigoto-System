import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Alert, Box, Button, Card, CardContent, Chip, CircularProgress, FormControl,
  Grid, InputLabel, MenuItem, Select, Stack, TextField, Typography,
} from '@mui/material';
import AddRoundedIcon from '@mui/icons-material/AddRounded';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import PageSkeleton from '../components/PageSkeleton';
import ActionDialog from '../components/ActionDialog';
import api from '../services/api';
import { jobSaveErrorMessage } from '../utils/validationFeedback';

const emptyForm = { title: '', description: '', location: '', status: 'OPEN' };

const statusColor = {
  OPEN: 'secondary',
  PAUSED: 'warning',
  CLOSED: 'default',
};

function errorMessage(error, fallback) {
  return error.response?.data?.message || fallback;
}

export default function JobManagement() {
  const [jobs, setJobs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [dialog, setDialog] = useState(null);
  const [selectedJob, setSelectedJob] = useState(null);
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [form, setForm] = useState(emptyForm);
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState('');

  const loadJobs = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const response = await api.get('/hr/jobs');
      setJobs(response.data);
    } catch (requestError) {
      setError(errorMessage(requestError, 'Could not load jobs.'));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    let active = true;
    api.get('/hr/jobs')
      .then((response) => {
        if (active) setJobs(response.data);
      })
      .catch((requestError) => {
        if (active) setError(errorMessage(requestError, 'Could not load jobs.'));
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, []);

  const openCreate = () => {
    setSelectedJob(null);
    setForm(emptyForm);
    setFormError('');
    setDialog('create');
  };

  const openEdit = (job) => {
    setSelectedJob(job);
    setForm({
      title: job.title,
      description: job.description,
      location: job.location,
      status: job.status,
      version: job.version,
    });
    setFormError('');
    setDialog('edit');
  };

  const closeDialog = () => {
    if (!saving) setDialog(null);
  };

  const updateField = (event) => {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
  };

  const formIsInvalid = !form.title.trim() || !form.description.trim() || !form.location.trim();

  const saveJob = async () => {
    setSaving(true);
    setFormError('');
    try {
      if (dialog === 'create') {
        await api.post('/hr/jobs', {
          title: form.title,
          description: form.description,
          location: form.location,
        });
      } else {
        await api.put(`/hr/jobs/${selectedJob.id}`, { ...form, version: selectedJob.version });
      }
      setDialog(null);
      await loadJobs();
    } catch (requestError) {
      if (requestError.response?.status === 409) await loadJobs();
      setFormError(jobSaveErrorMessage(requestError));
    } finally {
      setSaving(false);
    }
  };

  const counts = jobs.reduce((result, job) => ({
    ...result,
    [job.status]: (result[job.status] || 0) + 1,
  }), {});
  const filteredJobs = useMemo(() => statusFilter === 'ALL'
    ? jobs
    : jobs.filter((job) => job.status === statusFilter), [jobs, statusFilter]);

  return (
    <PageSkeleton title="Job Management" description="Create and maintain technology openings and their hiring teams.">
      <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" alignItems={{ sm: 'center' }} spacing={2} sx={{ mb: 2.5 }}>
        <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
          <Chip label={`${jobs.length} all`} onClick={() => setStatusFilter('ALL')} color="primary" variant={statusFilter === 'ALL' ? 'filled' : 'outlined'} />
          <Chip label={`${counts.OPEN || 0} open`} onClick={() => setStatusFilter('OPEN')} color="secondary" variant={statusFilter === 'OPEN' ? 'filled' : 'outlined'} />
          <Chip label={`${counts.PAUSED || 0} paused`} onClick={() => setStatusFilter('PAUSED')} color="warning" variant={statusFilter === 'PAUSED' ? 'filled' : 'outlined'} />
          <Chip label={`${counts.CLOSED || 0} closed`} onClick={() => setStatusFilter('CLOSED')} variant={statusFilter === 'CLOSED' ? 'filled' : 'outlined'} />
        </Stack>
        <Button variant="contained" startIcon={<AddRoundedIcon />} onClick={openCreate}>Create job</Button>
      </Stack>

      {loading && <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}><CircularProgress /></Box>}
      {!loading && error && <Alert severity="error" action={<Button color="inherit" onClick={loadJobs}>Retry</Button>}>{error}</Alert>}
      {!loading && !error && jobs.length === 0 && (
        <Box sx={{ py: 8, textAlign: 'center' }}>
          <Typography variant="h6">No jobs yet</Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>Create the first job for your company.</Typography>
        </Box>
      )}

      {!loading && !error && jobs.length > 0 && filteredJobs.length === 0 && (
        <Box sx={{ py: 8, textAlign: 'center' }}>
          <Typography variant="h6">No {statusFilter.toLowerCase()} jobs</Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>Choose another status or return to All.</Typography>
          <Button variant="text" onClick={() => setStatusFilter('ALL')} sx={{ mt: 1 }}>Show all jobs</Button>
        </Box>
      )}

      {!loading && !error && filteredJobs.length > 0 && (
        <Grid container spacing={2}>
          {filteredJobs.map((job) => (
            <Grid key={job.id} size={{ xs: 12, lg: 4 }}>
              <Card sx={{ height: '100%' }}>
                <CardContent>
                  <Stack direction="row" justifyContent="space-between" alignItems="flex-start" spacing={2}>
                    <Chip label={job.status} size="small" color={statusColor[job.status]} variant="outlined" />
                    <Button size="small" startIcon={<EditOutlinedIcon />} onClick={() => openEdit(job)}>Edit</Button>
                  </Stack>
                  <Typography variant="h6" component="h2" sx={{ mt: 2 }}>{job.title}</Typography>
                  <Typography variant="body2" color="text.secondary" sx={{ mt: 0.75 }}>{job.location}</Typography>
                  <Box sx={{ mt: 3, pt: 2, borderTop: 1, borderColor: 'divider' }}>
                    <Typography variant="body2" color="text.secondary" sx={{ whiteSpace: 'pre-wrap' }}>
                      {job.description}
                    </Typography>
                  </Box>
                </CardContent>
              </Card>
            </Grid>
          ))}
        </Grid>
      )}

      <ActionDialog
        open={dialog === 'create'} onClose={closeDialog} title="Create job"
        description="Set up a new technology opening." confirmLabel="Create job"
        onConfirm={saveJob} confirmDisabled={formIsInvalid} loading={saving}
      >
        {formError && <Alert severity="error">{formError}</Alert>}
        <JobFields form={form} updateField={updateField} showStatus={false} />
      </ActionDialog>

      <ActionDialog
        open={dialog === 'edit'} onClose={closeDialog} title="Edit job"
        description={selectedJob?.title || 'Selected position'} confirmLabel="Save changes"
        onConfirm={saveJob} confirmDisabled={formIsInvalid} loading={saving}
      >
        {formError && <Alert severity="error">{formError}</Alert>}
        <JobFields form={form} updateField={updateField} showStatus />
      </ActionDialog>
    </PageSkeleton>
  );
}

function JobFields({ form, updateField, showStatus }) {
  return (
    <>
      <TextField label="Job title" name="title" value={form.title} onChange={updateField} inputProps={{ maxLength: 255 }} required fullWidth />
      <TextField label="Location" name="location" value={form.location} onChange={updateField} inputProps={{ maxLength: 255 }} required fullWidth />
      <TextField label="Role description / requirements" name="description" value={form.description} onChange={updateField} multiline minRows={4} required fullWidth />
      {showStatus && (
        <FormControl fullWidth>
          <InputLabel id="job-status-label">Status</InputLabel>
          <Select labelId="job-status-label" label="Status" name="status" value={form.status} onChange={updateField}>
            <MenuItem value="OPEN">Open</MenuItem>
            <MenuItem value="PAUSED">Paused</MenuItem>
            <MenuItem value="CLOSED">Closed</MenuItem>
          </Select>
        </FormControl>
      )}
    </>
  );
}
