import { useRef, useState } from 'react';
import { Alert, Box, Button, Dialog, DialogActions, DialogContent, DialogTitle, Divider, FormHelperText, Grid, IconButton, Stack, TextField, Typography } from '@mui/material';
import CloseRoundedIcon from '@mui/icons-material/CloseRounded';
import UploadFileOutlinedIcon from '@mui/icons-material/UploadFileOutlined';

const initialFormValues = {
  fullName: '',
  email: '',
  githubUrl: '',
  coverNote: '',
};

function isValidEmail(value) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
}

function isValidFullName(value) {
  return /\p{L}/u.test(value) && /^[\p{L}\p{M}\p{Zs}\p{Pd}'’]+$/u.test(value);
}

function isValidGithubUrl(value) {
  try {
    const url = new URL(value);
    const hostname = url.hostname.toLowerCase();
    const username = url.pathname.split('/')[1];
    const isAllowedHostname = hostname === 'github.com' || hostname === 'www.github.com';
    return ['http:', 'https:'].includes(url.protocol) && isAllowedHostname && Boolean(username);
  } catch {
    return false;
  }
}

function isPdf(file) {
  return file?.type === 'application/pdf' || file?.name.toLowerCase().endsWith('.pdf');
}

export default function ApplicationDialog({ open, onClose, job }) {
  const [values, setValues] = useState(initialFormValues);
  const [cvFile, setCvFile] = useState(null);
  const [touched, setTouched] = useState({});
  const [submitted, setSubmitted] = useState(false);
  const fileInputRef = useRef(null);

  const errors = {
    fullName: !values.fullName.trim() ? 'Full name is required.' : isValidFullName(values.fullName.trim()) ? '' : 'Use letters, spaces, hyphens, and apostrophes only.',
    email: !values.email.trim() ? 'Email address is required.' : isValidEmail(values.email.trim()) ? '' : 'Enter a valid email address.',
    githubUrl: !values.githubUrl.trim() ? 'GitHub Profile URL is required.' : isValidGithubUrl(values.githubUrl.trim()) ? '' : 'Enter a valid GitHub profile URL.',
    cv: !cvFile ? 'A CV in PDF format is required.' : isPdf(cvFile) ? '' : 'Select a PDF file only.',
  };
  const isFormValid = Object.values(errors).every((error) => !error);

  const handleChange = (field) => (event) => {
    setValues((current) => ({ ...current, [field]: event.target.value }));
    setSubmitted(false);
  };

  const handleBlur = (field) => () => {
    setTouched((current) => ({ ...current, [field]: true }));
  };

  const handleFileChange = (event) => {
    setCvFile(event.target.files?.[0] || null);
    setTouched((current) => ({ ...current, cv: true }));
    setSubmitted(false);
  };

  const resetForm = () => {
    setValues(initialFormValues);
    setCvFile(null);
    setTouched({});
    setSubmitted(false);
    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  const handleClose = () => {
    resetForm();
    onClose();
  };

  const handleSubmit = (event) => {
    event.preventDefault();
    setTouched({ fullName: true, email: true, githubUrl: true, cv: true });
    if (isFormValid) setSubmitted(true);
  };

  return (
    <Dialog open={open} onClose={handleClose} fullWidth maxWidth="md">
      <Box component="form" noValidate onSubmit={handleSubmit}>
        <DialogTitle component="div">
          <Stack direction="row" justifyContent="space-between" alignItems="flex-start" spacing={2}>
            <div>
              <Typography variant="overline" color="secondary.dark" fontWeight={800}>Apply to Shigoto</Typography>
              <Typography variant="h6">{job?.title || 'Selected position'}</Typography>
              <Typography variant="body2" color="text.secondary">{job?.location || 'Location pending'} · {job?.type || 'Full-time'}</Typography>
            </div>
            <IconButton aria-label="Close application dialog" onClick={handleClose} size="small"><CloseRoundedIcon /></IconButton>
          </Stack>
        </DialogTitle>
        <Divider />
        <DialogContent sx={{ display: 'grid', gap: 2.5, pt: 3 }}>
          <Alert severity="info" icon={false}>Preview mode: this form validates locally, but submission does not create an application.</Alert>
          {submitted && <Alert severity="success">Application preview completed. No application was sent.</Alert>}
          <Grid container spacing={2}>
            <Grid size={{ xs: 12, md: 6 }}>
              <TextField
                label="Full name"
                value={values.fullName}
                onChange={handleChange('fullName')}
                onBlur={handleBlur('fullName')}
                error={touched.fullName && Boolean(errors.fullName)}
                helperText={touched.fullName ? errors.fullName : ' '}
                required
                autoComplete="name"
                fullWidth
              />
            </Grid>
            <Grid size={{ xs: 12, md: 6 }}>
              <TextField
                label="Email address"
                type="email"
                value={values.email}
                onChange={handleChange('email')}
                onBlur={handleBlur('email')}
                error={touched.email && Boolean(errors.email)}
                helperText={touched.email ? errors.email : ' '}
                required
                autoComplete="email"
                fullWidth
              />
            </Grid>
          </Grid>
          <TextField
            label="GitHub Profile URL"
            placeholder="https://github.com/username"
            type="url"
            value={values.githubUrl}
            onChange={handleChange('githubUrl')}
            onBlur={handleBlur('githubUrl')}
            error={touched.githubUrl && Boolean(errors.githubUrl)}
            helperText={touched.githubUrl ? errors.githubUrl : 'Use your public GitHub profile URL.'}
            required
            autoComplete="url"
            fullWidth
          />
          <TextField
            label="Cover note"
            value={values.coverNote}
            onChange={handleChange('coverNote')}
            multiline
            minRows={4}
            fullWidth
          />
          <Box sx={{ p: 2.5, border: 1, borderStyle: 'dashed', borderColor: touched.cv && errors.cv ? 'error.main' : 'divider', borderRadius: 2, bgcolor: 'background.default' }}>
            <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" alignItems={{ xs: 'flex-start', sm: 'center' }} spacing={2}>
              <Box>
                <Typography variant="body2" fontWeight={800}>CV / resume</Typography>
                <Typography variant="caption" color="text.secondary">Upload one PDF file for this application preview.</Typography>
              </Box>
              <Button component="label" variant="outlined" startIcon={<UploadFileOutlinedIcon />}>
                Choose PDF
                <input ref={fileInputRef} type="file" accept="application/pdf,.pdf" hidden onChange={handleFileChange} />
              </Button>
            </Stack>
            {cvFile && <Typography variant="body2" sx={{ mt: 1.5 }} noWrap>Selected: {cvFile.name}</Typography>}
            {touched.cv && errors.cv && <FormHelperText error sx={{ mt: 1 }}>{errors.cv}</FormHelperText>}
          </Box>
        </DialogContent>
        <DialogActions sx={{ px: 3, py: 2.5 }}>
          <Button type="button" onClick={handleClose} color="inherit">Cancel</Button>
          <Button type="submit" variant="contained" disabled={!isFormValid}>Submit application</Button>
        </DialogActions>
      </Box>
    </Dialog>
  );
}
