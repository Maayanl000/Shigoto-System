import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Box, Button, Container, Divider, Stack, Typography } from '@mui/material';

const footerLinks = [
  { label: 'About Us', to: '/about' },
  { label: 'Contact Us', to: '/contact' },
  { label: 'Login', to: '/login' },
  { label: 'Register', to: '/register' },
];

export default function PublicFooter() {
  const navigate = useNavigate();
  const location = useLocation();

  const goHome = () => {
    if (location.pathname === '/' && !location.hash) {
      window.scrollTo({ top: 0, behavior: 'smooth' });
      return;
    }
    navigate('/');
  };
  const goJobs = () => {
    if (location.pathname === '/') {
      if (location.hash !== '#jobs') navigate('/#jobs');
      window.requestAnimationFrame(() => document.getElementById('jobs')?.scrollIntoView({ behavior: 'smooth', block: 'start' }));
      return;
    }
    navigate('/#jobs');
  };

  return (
    <Box component="footer" sx={{ mt: 'auto', bgcolor: 'primary.dark', color: 'primary.contrastText' }}>
      <Container maxWidth="xl" sx={{ py: 5 }}>
        <Stack direction={{ xs: 'column', md: 'row' }} justifyContent="space-between" alignItems={{ md: 'center' }} spacing={3}>
          <Box>
            <Typography fontWeight={900} letterSpacing="0.2em">SHIGOTO</Typography>
            <Typography variant="body2" sx={{ mt: 1, color: 'rgba(255,255,255,0.68)' }}>A clearer path through technology recruitment.</Typography>
          </Box>
          <Stack direction="row" flexWrap="wrap" useFlexGap spacing={0.5}>
            <Button onClick={goHome} sx={{ color: 'rgba(255,255,255,0.78)' }}>Home</Button>
            <Button onClick={goJobs} sx={{ color: 'rgba(255,255,255,0.78)' }}>Jobs</Button>
            {footerLinks.map((link) => (
              <Button key={link.to} component={Link} to={link.to} sx={{ color: 'rgba(255,255,255,0.78)' }}>{link.label}</Button>
            ))}
          </Stack>
        </Stack>
        <Divider sx={{ my: 3, borderColor: 'rgba(255,255,255,0.12)' }} />
        <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" alignItems={{ sm: 'center' }} spacing={2}>
          <Typography variant="caption" sx={{ color: 'rgba(255,255,255,0.52)' }}>(c) 2026 Shigoto. Technology recruitment, clearly organized.</Typography>
        </Stack>
      </Container>
    </Box>
  );
}
