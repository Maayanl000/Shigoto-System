import { useState } from 'react';
import { NavLink, useLocation, useNavigate } from 'react-router-dom';
import { AppBar, Box, Button, Chip, Container, Divider, Drawer, IconButton, Stack, Toolbar, Typography } from '@mui/material';
import MenuRoundedIcon from '@mui/icons-material/MenuRounded';
import CloseRoundedIcon from '@mui/icons-material/CloseRounded';
import { useAuth } from '../auth/authContext';

const standardLinks = [
  { label: 'About Us', to: '/about', side: 'left' },
  { label: 'Contact Us', to: '/contact', side: 'left' },
];

const roleHomes = { CANDIDATE: '/candidate', HR: '/hr', INTERVIEWER: '/interviewer' };

const navButtonSx = {
  color: 'text.secondary',
  px: 1.25,
  whiteSpace: 'nowrap',
  '&:hover': { color: 'primary.main', bgcolor: 'primary.light' },
  '&.active': { color: 'primary.main' },
};

export default function PublicHeader() {
  const { user, loading, logout } = useAuth();
  const [mobileOpen, setMobileOpen] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();

  const closeMenu = () => setMobileOpen(false);
  const dashboardPath = roleHomes[user?.role] || '/';
  const handleLogout = async () => {
    closeMenu();
    await logout();
  };
  const goHome = () => {
    closeMenu();
    if (location.pathname === '/' && !location.hash) {
      window.scrollTo({ top: 0, behavior: 'smooth' });
      return;
    }
    navigate('/');
  };
  const goJobs = () => {
    closeMenu();
    if (location.pathname === '/') {
      if (location.hash !== '#jobs') navigate('/#jobs');
      window.requestAnimationFrame(() => document.getElementById('jobs')?.scrollIntoView({ behavior: 'smooth', block: 'start' }));
      return;
    }
    navigate('/#jobs');
  };

  return (
    <>
      <AppBar position="sticky" color="inherit" sx={{ borderBottom: 1, borderColor: 'divider', bgcolor: 'rgba(255,255,255,0.96)' }}>
        <Container maxWidth={false} sx={{ px: { xs: 2, sm: 3, lg: 4 } }}>
          <Toolbar
            disableGutters
            sx={{
              minHeight: { xs: 64, lg: 76 },
              position: 'relative',
              display: 'grid',
              gridTemplateColumns: { xs: '1fr auto', lg: 'minmax(0, 1fr) auto minmax(0, 1fr)' },
              columnGap: { lg: 2 },
            }}
          >
            <Stack direction="row" spacing={0.25} sx={{ display: { xs: 'none', lg: 'flex' }, minWidth: 0 }}>
              <Button onClick={goHome} sx={{ ...navButtonSx, color: location.pathname === '/' && !location.hash ? 'primary.main' : 'text.secondary' }}>Home</Button>
              <Button onClick={goJobs} sx={{ ...navButtonSx, color: location.pathname === '/' && location.hash === '#jobs' ? 'primary.main' : 'text.secondary' }}>Jobs</Button>
              {standardLinks.filter((link) => link.side === 'left').map((link) => (
                <Button key={link.to} component={NavLink} to={link.to} sx={navButtonSx}>{link.label}</Button>
              ))}
            </Stack>

            <Typography
              component="button"
              type="button"
              onClick={goHome}
              aria-label="Shigoto home"
              sx={{
                gridColumn: { xs: 1, lg: 2 },
                justifySelf: { xs: 'start', lg: 'center' },
                p: 0,
                border: 0,
                bgcolor: 'transparent',
                color: 'primary.dark',
                fontFamily: 'inherit',
                fontSize: { xs: '1.2rem', lg: '1.35rem' },
                fontWeight: 900,
                letterSpacing: '0.22em',
                cursor: 'pointer',
                whiteSpace: 'nowrap',
                '&:focus-visible': { outline: '3px solid rgba(8,127,140,0.24)', outlineOffset: 4, borderRadius: 0.5 },
              }}
            >
              SHIGOTO
            </Typography>

            <Stack
              direction="row"
              justifyContent="flex-end"
              alignItems="center"
              spacing={0.25}
              sx={{ display: { xs: 'none', lg: 'flex' }, position: 'absolute', right: 0, top: '50%', transform: 'translateY(-50%)', minWidth: 0 }}
            >
              {!loading && !user && (
                <>
                  <Button component={NavLink} to="/login" sx={navButtonSx}>Login</Button>
                  <Button component={NavLink} to="/register" variant="contained" sx={{ ml: 1.25, whiteSpace: 'nowrap' }}>Sign Up</Button>
                </>
              )}
              {!loading && user && (
                <>
                  <Chip label={`Hi, ${user.firstName}`} size="small" variant="outlined" />
                  <Button component={NavLink} to={dashboardPath} sx={navButtonSx}>Dashboard</Button>
                  {user.role === 'HR' && <Button component={NavLink} to="/hr/jobs" sx={navButtonSx}>Job Management</Button>}
                  <Button onClick={handleLogout} color="inherit">Logout</Button>
                </>
              )}
            </Stack>

            <IconButton aria-label="Open navigation menu" onClick={() => setMobileOpen(true)} sx={{ display: { lg: 'none' }, gridColumn: 2, ml: 'auto' }}>
              <MenuRoundedIcon />
            </IconButton>
          </Toolbar>
        </Container>
      </AppBar>

      <Drawer anchor="right" open={mobileOpen} onClose={closeMenu}>
        <Box sx={{ width: 300, p: 2 }} role="navigation" aria-label="Mobile public navigation">
          <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 2 }}>
            <Typography fontWeight={900} letterSpacing="0.18em" color="primary.dark">SHIGOTO</Typography>
            <IconButton aria-label="Close navigation menu" onClick={closeMenu}><CloseRoundedIcon /></IconButton>
          </Stack>
          <Divider sx={{ mb: 1.5 }} />
          <Stack spacing={0.5}>
            <Button onClick={goHome} sx={{ justifyContent: 'flex-start', color: 'text.primary' }}>Home</Button>
            <Button onClick={goJobs} sx={{ justifyContent: 'flex-start', color: 'text.primary' }}>Jobs</Button>
            {standardLinks.map((link) => (
              <Button key={link.to} component={NavLink} to={link.to} onClick={closeMenu} sx={{ justifyContent: 'flex-start', color: 'text.primary' }}>{link.label}</Button>
            ))}
            {!loading && !user && (
              <>
                <Button component={NavLink} to="/login" onClick={closeMenu} sx={{ justifyContent: 'flex-start', color: 'text.primary' }}>Login</Button>
                <Button component={NavLink} to="/register" variant="contained" onClick={closeMenu} sx={{ mt: 1 }}>Sign Up</Button>
              </>
            )}
            {!loading && user && (
              <>
                <Typography variant="body2" color="text.secondary" sx={{ px: 1, py: 0.75 }}>Signed in as {user.firstName}</Typography>
                <Button component={NavLink} to={dashboardPath} onClick={closeMenu} sx={{ justifyContent: 'flex-start', color: 'text.primary' }}>Dashboard</Button>
                {user.role === 'HR' && <Button component={NavLink} to="/hr/jobs" onClick={closeMenu} sx={{ justifyContent: 'flex-start', color: 'text.primary' }}>Job Management</Button>}
                <Button onClick={handleLogout} sx={{ justifyContent: 'flex-start', color: 'text.primary' }}>Logout</Button>
              </>
            )}
          </Stack>
        </Box>
      </Drawer>
    </>
  );
}
