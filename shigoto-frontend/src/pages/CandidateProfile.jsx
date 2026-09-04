import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Alert, Box, Button, Card, CardContent, Checkbox, Divider, FormControlLabel,
  Grid, MenuItem, Stack, TextField, Typography,
} from '@mui/material';
import SaveOutlinedIcon from '@mui/icons-material/SaveOutlined';
import PageSkeleton from '../components/PageSkeleton';
import { useAuth } from '../auth/authContext';
import { isValidGithubProfile } from '../utils/githubProfile';

function validateName(value, label) {
  if (!value.trim()) return `${label} is required.`;
  if (value.trim().length > 255) return `${label} must be at most 255 characters.`;
  if (/\p{Nd}/u.test(value)) return `${label} must not contain digits.`;
  return null;
}

const employmentTypes = [
  { value: 'FULL_TIME', label: 'Full-time' },
  { value: 'PART_TIME', label: 'Part-time' },
  { value: 'STUDENT', label: 'Student position' },
  { value: 'INTERNSHIP', label: 'Internship' },
];

export default function CandidateProfile() {
  const { user, updateProfile } = useAuth();
  const navigate = useNavigate();
  const [values, setValues] = useState(() => ({
    firstName: user?.firstName || '',
    lastName: user?.lastName || '',
    githubProfileUrl: user?.githubProfileUrl || '',
    currentTitle: user?.currentTitle || '',
    desiredRole: user?.desiredRole || '',
    employmentType: user?.employmentType || '',
    student: Boolean(user?.student),
  }));
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);

  const handleChange = (field) => (event) => {
    setValues((current) => ({ ...current, [field]: event.target.value }));
    setError('');
    setSuccess(false);
  };

  const handleStudentChange = (event) => {
    setValues((current) => ({ ...current, student: event.target.checked }));
    setError('');
    setSuccess(false);
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    const firstNameError = validateName(values.firstName, 'First name');
    const lastNameError = validateName(values.lastName, 'Last name');
    if (firstNameError || lastNameError) {
      setError(firstNameError || lastNameError);
      return;
    }
    if (!isValidGithubProfile(values.githubProfileUrl)) {
      setError('Enter a valid GitHub profile URL, such as https://github.com/username.');
      return;
    }
    if (values.githubProfileUrl.trim().length > 255) {
      setError('GitHub profile URL must be at most 255 characters.');
      return;
    }
    if (values.currentTitle.trim().length > 100 || values.desiredRole.trim().length > 100) {
      setError('Current title and desired role must be 100 characters or fewer.');
      return;
    }
    setSaving(true);
    setError('');
    setSuccess(false);
    try {
      const updatedUser = await updateProfile({
        firstName: values.firstName.trim(),
        lastName: values.lastName.trim(),
        githubProfileUrl: values.githubProfileUrl.trim(),
        currentTitle: values.currentTitle.trim() || null,
        desiredRole: values.desiredRole.trim() || null,
        employmentType: values.employmentType || null,
        student: values.student,
      });
      setValues({
        firstName: updatedUser.firstName,
        lastName: updatedUser.lastName,
        githubProfileUrl: updatedUser.githubProfileUrl,
        currentTitle: updatedUser.currentTitle || '',
        desiredRole: updatedUser.desiredRole || '',
        employmentType: updatedUser.employmentType || '',
        student: Boolean(updatedUser.student),
      });
      setSuccess(true);
    } catch (requestError) {
      if (requestError.response?.status === 401) {
        navigate('/login', { replace: true });
        return;
      }
      setError(requestError.response?.status === 400
        ? 'Check the profile details and try again.'
        : 'We could not save your profile. Please try again.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <PageSkeleton title="My Profile" description="Keep your Candidate details current for future applications.">
      <Card sx={{ maxWidth: 820 }}>
        <CardContent component="form" noValidate onSubmit={handleSubmit} sx={{ p: { xs: 2.5, sm: 3.5 } }}>
          <Stack spacing={3}>
            <Box>
              <Typography variant="h6">Personal details</Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
                These details belong to your Candidate profile and are reused across applications.
              </Typography>
            </Box>

            {success && <Alert severity="success">Your profile was updated successfully.</Alert>}
            {error && <Alert severity="error">{error}</Alert>}

            <Grid container spacing={2}>
              <Grid size={{ xs: 12, sm: 6 }}>
                <TextField label="First name" value={values.firstName} onChange={handleChange('firstName')} inputProps={{ maxLength: 255 }} autoComplete="given-name" required disabled={saving} fullWidth />
              </Grid>
              <Grid size={{ xs: 12, sm: 6 }}>
                <TextField label="Last name" value={values.lastName} onChange={handleChange('lastName')} inputProps={{ maxLength: 255 }} autoComplete="family-name" required disabled={saving} fullWidth />
              </Grid>
            </Grid>

            <Grid container spacing={2}>
              <Grid size={{ xs: 12, sm: 6 }}>
                <TextField
                  label="Email"
                  value={user?.email || ''}
                  helperText="This is your account login email and cannot be changed here."
                  slotProps={{ input: { readOnly: true } }}
                  fullWidth
                />
              </Grid>
              <Grid size={{ xs: 12, sm: 6 }}>
                <TextField
                  label="GitHub Profile URL"
                  value={values.githubProfileUrl}
                  onChange={handleChange('githubProfileUrl')}
                  inputProps={{ maxLength: 255 }}
                  placeholder="https://github.com/username"
                  autoComplete="url"
                  required
                  disabled={saving}
                  fullWidth
                />
              </Grid>
            </Grid>

            <Divider />

            <Box>
              <Typography variant="h6">Career preferences</Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
                Add a little context about the opportunities you are looking for.
              </Typography>
            </Box>

            <Grid container spacing={2}>
              <Grid size={{ xs: 12, sm: 6 }}>
                <TextField label="Current title" value={values.currentTitle} onChange={handleChange('currentTitle')} inputProps={{ maxLength: 100 }} disabled={saving} fullWidth />
              </Grid>
              <Grid size={{ xs: 12, sm: 6 }}>
                <TextField label="Desired role" value={values.desiredRole} onChange={handleChange('desiredRole')} inputProps={{ maxLength: 100 }} disabled={saving} fullWidth />
              </Grid>
              <Grid size={{ xs: 12, sm: 6 }}>
                <TextField select label="Employment preference" value={values.employmentType} onChange={handleChange('employmentType')} disabled={saving} fullWidth>
                  <MenuItem value="">No preference</MenuItem>
                  {employmentTypes.map((option) => (
                    <MenuItem key={option.value} value={option.value}>{option.label}</MenuItem>
                  ))}
                </TextField>
              </Grid>
              <Grid size={{ xs: 12, sm: 6 }} sx={{ display: 'flex', alignItems: 'center' }}>
                <FormControlLabel
                  control={<Checkbox checked={values.student} onChange={handleStudentChange} disabled={saving} />}
                  label="I am currently a student"
                />
              </Grid>
            </Grid>

            <Box>
              <Button type="submit" variant="contained" startIcon={<SaveOutlinedIcon />} disabled={saving}>
                {saving ? 'Saving…' : 'Save profile'}
              </Button>
            </Box>
          </Stack>
        </CardContent>
      </Card>
    </PageSkeleton>
  );
}
