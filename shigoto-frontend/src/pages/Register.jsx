import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Alert, Box, Button, Card, CardContent, Chip, Divider, Grid, Stack, TextField, Typography } from '@mui/material';
import PersonAddOutlinedIcon from '@mui/icons-material/PersonAddOutlined';
import { useAuth } from '../auth/authContext';
import { isValidGithubProfile } from '../utils/githubProfile';
import { registrationErrorMessage } from '../utils/validationFeedback';

export default function Register() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [values, setValues] = useState({ firstName: '', lastName: '', email: '', password: '', githubProfileUrl: '' });
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  const handleChange = (field) => (event) => {
    setValues((current) => ({ ...current, [field]: event.target.value }));
    setError('');
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    const emailValid = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(values.email.trim());
    if (!values.firstName.trim() || !values.lastName.trim()) {
      setError('First name and last name are required.');
      return;
    }
    if (!emailValid) {
      setError('Enter a valid email address.');
      return;
    }
    if (values.password.length < 8) {
      setError('Password must be at least 8 characters.');
      return;
    }
    if (!isValidGithubProfile(values.githubProfileUrl)) {
      setError('Enter a valid GitHub profile URL, such as https://github.com/username.');
      return;
    }

    setSubmitting(true);
    setError('');
    try {
      await register({
        firstName: values.firstName.trim(),
        lastName: values.lastName.trim(),
        email: values.email.trim(),
        password: values.password,
        githubProfileUrl: values.githubProfileUrl.trim(),
      });
      const returnPath = typeof location.state?.from === 'string' ? location.state.from : '/candidate';
      navigate(returnPath, { replace: true });
    } catch (requestError) {
      setError(registrationErrorMessage(requestError));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Box sx={{ minHeight: { xs: 690, md: 760 }, px: 2, py: { xs: 5, md: 8 }, bgcolor: 'background.default' }}>
      <Box sx={{ maxWidth: 520, mx: 'auto', textAlign: 'left' }}>
      <Box sx={{ textAlign: 'center', mb: 3 }}>
        <Box sx={{ display: 'grid', placeItems: 'center', width: 48, height: 48, mx: 'auto', mb: 2, borderRadius: 2.5, bgcolor: 'secondary.light', color: 'secondary.dark' }}>
          <PersonAddOutlinedIcon />
        </Box>
        <Typography variant="h4" component="h1" gutterBottom>Create your profile</Typography>
        <Typography color="text.secondary">Start a candidate account for Shigoto opportunities.</Typography>
      </Box>
      <Card sx={{ bgcolor: 'background.paper', borderColor: 'divider', borderTop: 3, borderTopColor: 'secondary.main', boxShadow: '0 14px 38px rgba(16,35,61,0.10)' }}>
        <CardContent component="form" noValidate onSubmit={handleSubmit} sx={{ display: 'grid', gap: 2.25 }}>
          <Stack direction="row" justifyContent="space-between" alignItems="center">
            <Typography variant="h6">Profile and credentials</Typography>
            <Chip label="Candidate account" size="small" variant="outlined" />
          </Stack>
          {error && <Alert severity="error">{error}</Alert>}
          <Grid container spacing={2}>
            <Grid size={{ xs: 12, sm: 6 }}><TextField label="First name" value={values.firstName} onChange={handleChange('firstName')} inputProps={{ maxLength: 255 }} autoComplete="given-name" required disabled={submitting} fullWidth /></Grid>
            <Grid size={{ xs: 12, sm: 6 }}><TextField label="Last name" value={values.lastName} onChange={handleChange('lastName')} inputProps={{ maxLength: 255 }} autoComplete="family-name" required disabled={submitting} fullWidth /></Grid>
          </Grid>
          <TextField label="Email" type="email" value={values.email} onChange={handleChange('email')} inputProps={{ maxLength: 255 }} autoComplete="email" required disabled={submitting} fullWidth />
          <TextField label="GitHub Profile URL" type="url" value={values.githubProfileUrl} onChange={handleChange('githubProfileUrl')} inputProps={{ maxLength: 255 }} placeholder="https://github.com/username" autoComplete="url" required disabled={submitting} fullWidth />
          <TextField label="Password" type="password" value={values.password} onChange={handleChange('password')} helperText="Use at least 8 characters." autoComplete="new-password" required disabled={submitting} fullWidth />
          <Button type="submit" variant="contained" disabled={submitting} fullWidth>{submitting ? 'Creating account…' : 'Create account'}</Button>
          <Divider />
          <Button component={Link} to="/login" state={location.state} disabled={submitting}>Already registered? Log in</Button>
        </CardContent>
      </Card>
      </Box>
    </Box>
  );
}
