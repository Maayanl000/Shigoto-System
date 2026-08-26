import { useRef, useState } from 'react';
import { Alert, Box, Button, Dialog, DialogActions, DialogContent, DialogTitle, Divider, FormHelperText, IconButton, Stack, TextField, Typography } from '@mui/material';
import CloseRoundedIcon from '@mui/icons-material/CloseRounded';
import UploadFileOutlinedIcon from '@mui/icons-material/UploadFileOutlined';
import api from '../services/api';
import { useAuth } from '../auth/authContext';

const MAX_CV_SIZE = 5 * 1024 * 1024;

function isPdf(file) {
  return file?.type === 'application/pdf' || file?.name.toLowerCase().endsWith('.pdf');
}

export default function ApplicationDialog({ open, onClose, job, onSubmitted }) {
  const { user } = useAuth();
  const [coverNote, setCoverNote] = useState('');
  const [cvFile, setCvFile] = useState(null);
  const [cvTouched, setCvTouched] = useState(false);
  const [submissionStatus, setSubmissionStatus] = useState('idle');
  const [submissionError, setSubmissionError] = useState('');
  const fileInputRef = useRef(null);

  const cvError = !cvFile
    ? 'A CV in PDF format is required.'
    : cvFile.size > MAX_CV_SIZE
      ? 'CV must not exceed 5 MB.'
      : isPdf(cvFile) ? '' : 'Select a PDF file only.';
  const canSubmitJob = Boolean(job?.id) && !job?.isFallback;
  const isSubmitting = submissionStatus === 'submitting';
  const isSuccessful = submissionStatus === 'success';

  const clearSubmissionFeedback = () => {
    setSubmissionStatus('idle');
    setSubmissionError('');
  };

  const handleFileChange = (event) => {
    setCvFile(event.target.files?.[0] || null);
    setCvTouched(true);
    clearSubmissionFeedback();
  };

  const resetForm = () => {
    setCoverNote('');
    setCvFile(null);
    setCvTouched(false);
    clearSubmissionFeedback();
    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  const handleClose = () => {
    resetForm();
    onClose();
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setCvTouched(true);
    if (cvError || !canSubmitJob || isSubmitting) return;

    setSubmissionStatus('submitting');
    setSubmissionError('');
    try {
      const formData = new FormData();
      formData.append('jobId', String(job.id));
      formData.append('coverLetter', coverNote);
      formData.append('cv', cvFile);
      const response = await api.post('/applications', formData);
      setSubmissionStatus('success');
      onSubmitted?.(response.data);
    } catch (error) {
      if (error.response?.status === 401) {
        window.location.assign('/login');
        return;
      }
      if (error.response?.status === 409) {
        setSubmissionError('You have already applied for this position.');
      } else {
        setSubmissionError('We could not submit your application. Please try again.');
      }
      setSubmissionStatus('error');
    }
  };

  return (
    <Dialog open={open} onClose={isSubmitting ? undefined : handleClose} fullWidth maxWidth="md">
      <Box component="form" noValidate onSubmit={handleSubmit}>
        <DialogTitle component="div">
          <Stack direction="row" justifyContent="space-between" alignItems="flex-start" spacing={2}>
            <div>
              <Typography variant="overline" color="secondary.dark" fontWeight={800}>Apply to {job?.companyName || 'Shigoto'}</Typography>
              <Typography variant="h6">{job?.title || 'Selected position'}</Typography>
              <Typography variant="body2" color="text.secondary">{job?.location || 'Location pending'} · {job?.type || 'Full-time'}</Typography>
            </div>
            <IconButton aria-label="Close application dialog" onClick={handleClose} disabled={isSubmitting} size="small"><CloseRoundedIcon /></IconButton>
          </Stack>
        </DialogTitle>
        <Divider />
        <DialogContent sx={{ display: 'grid', gap: 2.5, pt: 3 }}>
          <Alert severity="info" icon={false}>Your selected PDF CV will be securely attached to this application.</Alert>
          {!canSubmitJob && <Alert severity="warning">This preview job is not connected to the backend and cannot accept applications.</Alert>}
          {submissionStatus === 'success' && <Alert severity="success">Your application was submitted successfully.</Alert>}
          {submissionStatus === 'error' && <Alert severity="error">{submissionError}</Alert>}

          <Box sx={{ p: 1.75, borderRadius: 1.5, bgcolor: 'background.default' }}>
            <Typography variant="body2" fontWeight={700}>{user?.firstName} {user?.lastName}</Typography>
            <Typography variant="caption" color="text.secondary">{user?.email} · {user?.githubProfileUrl}</Typography>
          </Box>

          <TextField
            label="Cover Letter"
            value={coverNote}
            onChange={(event) => { setCoverNote(event.target.value); clearSubmissionFeedback(); }}
            multiline
            minRows={4}
            disabled={isSuccessful}
            fullWidth
          />

          <Box sx={{ p: 2.5, border: 1, borderStyle: 'dashed', borderColor: cvTouched && cvError ? 'error.main' : 'divider', borderRadius: 2, bgcolor: 'background.default' }}>
            <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" alignItems={{ xs: 'flex-start', sm: 'center' }} spacing={2}>
              <Box>
                <Typography variant="body2" fontWeight={800}>CV / resume</Typography>
                <Typography variant="caption" color="text.secondary">Select one PDF file up to 5 MB.</Typography>
              </Box>
              <Button component="label" variant="outlined" startIcon={<UploadFileOutlinedIcon />} disabled={isSuccessful}>
                Choose PDF
                <input ref={fileInputRef} type="file" accept="application/pdf,.pdf" hidden onChange={handleFileChange} />
              </Button>
            </Stack>
            {cvFile && <Typography variant="body2" sx={{ mt: 1.5 }} noWrap>Selected: {cvFile.name}</Typography>}
            {cvTouched && cvError && <FormHelperText error sx={{ mt: 1 }}>{cvError}</FormHelperText>}
          </Box>
        </DialogContent>
        <DialogActions sx={{ px: 3, py: 2.5 }}>
          {isSuccessful ? (
            <Button type="button" onClick={handleClose} variant="contained">Close</Button>
          ) : (
            <>
              <Button type="button" onClick={handleClose} color="inherit" disabled={isSubmitting}>Cancel</Button>
              <Button type="submit" variant="contained" disabled={Boolean(cvError) || !canSubmitJob || isSubmitting}>
                {isSubmitting ? 'Submitting…' : 'Submit application'}
              </Button>
            </>
          )}
        </DialogActions>
      </Box>
    </Dialog>
  );
}
