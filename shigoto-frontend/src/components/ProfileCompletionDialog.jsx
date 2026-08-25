import { useState } from 'react';
import { Alert, Button, Dialog, DialogActions, DialogContent, DialogTitle, Stack, TextField, Typography } from '@mui/material';
import { useAuth } from '../auth/authContext';
import { isValidGithubProfile } from '../utils/githubProfile';

export default function ProfileCompletionDialog({ open, onClose, onSaved }) {
  const { user, updateProfile } = useAuth();
  const [githubProfileUrl, setGithubProfileUrl] = useState('');
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  const handleSave = async () => {
    if (!isValidGithubProfile(githubProfileUrl)) {
      setError('Enter a valid GitHub profile URL, such as https://github.com/username.');
      return;
    }
    setSaving(true);
    setError('');
    try {
      const updatedUser = await updateProfile({
        firstName: user.firstName,
        lastName: user.lastName,
        githubProfileUrl: githubProfileUrl.trim(),
      });
      setGithubProfileUrl('');
      onSaved(updatedUser);
    } catch {
      setError('We could not save your profile. Please try again.');
    } finally {
      setSaving(false);
    }
  };

  const handleClose = () => {
    if (saving) return;
    setGithubProfileUrl('');
    setError('');
    onClose();
  };

  return (
    <Dialog open={open} onClose={handleClose} fullWidth maxWidth="sm">
      <DialogTitle>Complete your Candidate profile</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ pt: 1 }}>
          <Typography color="text.secondary">
            Add your GitHub profile once. It will be reused for future applications.
          </Typography>
          {error && <Alert severity="error">{error}</Alert>}
          <TextField
            label="GitHub Profile URL"
            placeholder="https://github.com/username"
            value={githubProfileUrl}
            onChange={(event) => { setGithubProfileUrl(event.target.value); setError(''); }}
            autoComplete="url"
            disabled={saving}
            required
            fullWidth
          />
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={handleClose} disabled={saving}>Cancel</Button>
        <Button onClick={handleSave} variant="contained" disabled={saving}>
          {saving ? 'Saving…' : 'Save and continue'}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
