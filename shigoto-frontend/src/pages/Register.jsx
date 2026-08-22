import { Link } from 'react-router-dom';
import { Box, Button, Card, CardContent, TextField, Typography } from '@mui/material';

export default function Register() {
  return (
    <Box sx={{ mt: 6, maxWidth: 560, mx: 'auto', textAlign: 'left' }}>
      <Typography variant="h4" component="h1" fontWeight="bold" color="primary" gutterBottom>
        Register
      </Typography>
      <Typography color="text.secondary" sx={{ mb: 3 }}>
        Candidate account creation skeleton. Registration is not active yet.
      </Typography>
      <Card variant="outlined">
        <CardContent sx={{ display: 'grid', gap: 2 }}>
          <Typography variant="h6">Profile and credentials</Typography>
          <TextField label="Full name" disabled fullWidth />
          <TextField label="Email" type="email" disabled fullWidth />
          <TextField label="Password" type="password" disabled fullWidth />
          <Button variant="contained" disabled>Create account</Button>
          <Button component={Link} to="/login">Already registered? Log in</Button>
        </CardContent>
      </Card>
    </Box>
  );
}
