import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Alert, Box, Button, Card, CardContent, Chip, Divider, Stack, TextField, Typography } from '@mui/material';
import LockOutlinedIcon from '@mui/icons-material/LockOutlined';
import { useAuth } from '../auth/authContext';

const roleHomes = { CANDIDATE: '/candidate', HR: '/hr', INTERVIEWER: '/interviewer' };

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const emailValid = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim());

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError('');
    if (!emailValid || !password) {
      setError('Enter a valid email address and password.');
      return;
    }

    setSubmitting(true);
    try {
      const user = await login({ email: email.trim(), password });
      const returnPath = user.role === 'CANDIDATE' && typeof location.state?.from === 'string'
        ? location.state.from
        : roleHomes[user.role] || '/';
      navigate(returnPath, { replace: true });
    } catch (requestError) {
      setError(requestError.response?.status === 401
        ? 'The email or password is incorrect.'
        : 'We could not log you in. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Box sx={{ minHeight: { xs: 640, md: 720 }, px: 2, py: { xs: 5, md: 8 }, bgcolor: 'background.default' }}>
      <Box sx={{ maxWidth: 460, mx: 'auto', textAlign: 'left' }}>
      <Box sx={{ textAlign: 'center', mb: 3 }}>
        <Box sx={{ display: 'grid', placeItems: 'center', width: 48, height: 48, mx: 'auto', mb: 2, borderRadius: 2.5, bgcolor: 'primary.light', color: 'primary.main' }}>
          <LockOutlinedIcon />
        </Box>
        <Typography variant="h4" component="h1" gutterBottom>Welcome back</Typography>
        <Typography color="text.secondary">Sign in to access your Shigoto workspace.</Typography>
      </Box>
      <Card sx={{ bgcolor: 'background.paper', borderColor: 'divider', borderTop: 3, borderTopColor: 'secondary.main', boxShadow: '0 14px 38px rgba(16,35,61,0.10)' }}>
        <CardContent component="form" noValidate onSubmit={handleSubmit} sx={{ display: 'grid', gap: 2.25 }}>
          <Stack direction="row" justifyContent="space-between" alignItems="center">
            <Typography variant="h6">Account credentials</Typography>
            <Chip label="Secure session" size="small" variant="outlined" />
          </Stack>
          {error && <Alert severity="error">{error}</Alert>}
          <TextField label="Email" type="email" value={email} onChange={(event) => setEmail(event.target.value)} autoComplete="email" required disabled={submitting} fullWidth />
          <TextField label="Password" type="password" value={password} onChange={(event) => setPassword(event.target.value)} autoComplete="current-password" required disabled={submitting} fullWidth />
          <Button type="submit" variant="contained" disabled={submitting} fullWidth>{submitting ? 'Logging in…' : 'Log in'}</Button>
          <Divider />
          <Button component={Link} to="/register" state={location.state} disabled={submitting}>Create an account</Button>
        </CardContent>
      </Card>
      </Box>
    </Box>
  );
}
