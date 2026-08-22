import { Link } from 'react-router-dom';
import { Box, Button, Card, CardContent, Chip, Divider, Stack, TextField, Typography } from '@mui/material';
import LockOutlinedIcon from '@mui/icons-material/LockOutlined';

export default function Login() {
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
        <CardContent sx={{ display: 'grid', gap: 2.25 }}>
          <Stack direction="row" justifyContent="space-between" alignItems="center">
            <Typography variant="h6">Account credentials</Typography>
            <Chip label="UI preview" size="small" variant="outlined" />
          </Stack>
          <TextField label="Email" type="email" disabled fullWidth />
          <TextField label="Password" type="password" disabled fullWidth />
          <Button variant="contained" disabled fullWidth>Log in</Button>
          <Divider />
          <Button component={Link} to="/register">Create an account</Button>
          <Typography variant="caption" color="text.secondary" textAlign="center">
            Authentication is intentionally unavailable in this UI preview.
          </Typography>
        </CardContent>
      </Card>
      </Box>
    </Box>
  );
}
