import { Typography, Box, Button } from '@mui/material';

export default function Login() {
  return (
    <Box sx={{ mt: 8, textAlign: 'center' }}>
      <Typography variant="h4" gutterBottom color="primary">
        Login / Register
      </Typography>
      <Typography variant="subtitle1" paragraph>
        You must be a registered user to apply for jobs.
      </Typography>
      <Button variant="contained" color="secondary" sx={{ mt: 2 }}>
        Sign Up with GitHub
      </Button>
    </Box>
  );
}