import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Box, Button, Container, Divider, ListItemIcon, ListItemText, Menu, MenuItem, Stack, Typography } from '@mui/material';
import DeveloperModeOutlinedIcon from '@mui/icons-material/DeveloperModeOutlined';
import PersonOutlineRoundedIcon from '@mui/icons-material/PersonOutlineRounded';
import ViewKanbanOutlinedIcon from '@mui/icons-material/ViewKanbanOutlined';
import RateReviewOutlinedIcon from '@mui/icons-material/RateReviewOutlined';

const footerLinks = [
  { label: 'About Us', to: '/about' },
  { label: 'Contact Us', to: '/contact' },
  { label: 'Login', to: '/login' },
  { label: 'Register', to: '/register' },
];

const previewLinks = [
  { label: 'Candidate workspace', to: '/candidate', icon: <PersonOutlineRoundedIcon fontSize="small" /> },
  { label: 'HR workspace', to: '/hr', icon: <ViewKanbanOutlinedIcon fontSize="small" /> },
  { label: 'Interviewer workspace', to: '/interviewer', icon: <RateReviewOutlinedIcon fontSize="small" /> },
];

export default function PublicFooter() {
  const [previewAnchor, setPreviewAnchor] = useState(null);
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
          <Typography variant="caption" sx={{ color: 'rgba(255,255,255,0.52)' }}>Shigoto ATS frontend preview. Interactive services are not connected yet.</Typography>
          <Stack direction="row" alignItems="center" spacing={1.25}>
            <Typography variant="caption" sx={{ color: 'rgba(255,255,255,0.46)' }}>Development Preview</Typography>
            <Button
              size="small"
              variant="outlined"
              startIcon={<DeveloperModeOutlinedIcon />}
              onClick={(event) => setPreviewAnchor(event.currentTarget)}
              aria-controls={previewAnchor ? 'workspace-preview-menu' : undefined}
              aria-haspopup="true"
              aria-expanded={previewAnchor ? 'true' : undefined}
              sx={{ color: 'rgba(255,255,255,0.78)', borderColor: 'rgba(255,255,255,0.24)', '&:hover': { borderColor: 'rgba(255,255,255,0.5)', bgcolor: 'rgba(255,255,255,0.06)' } }}
            >
              Preview Workspaces
            </Button>
          </Stack>
        </Stack>
      </Container>

      <Menu id="workspace-preview-menu" anchorEl={previewAnchor} open={Boolean(previewAnchor)} onClose={() => setPreviewAnchor(null)}>
        <Box sx={{ px: 2, py: 1 }}>
          <Typography variant="overline" color="secondary.dark" fontWeight={800}>Development Preview</Typography>
          <Typography variant="caption" color="text.secondary" display="block">Direct UI routes, not a login.</Typography>
        </Box>
        <Divider />
        {previewLinks.map((link) => (
          <MenuItem key={link.to} component={Link} to={link.to} onClick={() => setPreviewAnchor(null)} sx={{ py: 1.25 }}>
            <ListItemIcon>{link.icon}</ListItemIcon>
            <ListItemText primary={link.label} />
          </MenuItem>
        ))}
      </Menu>
    </Box>
  );
}
