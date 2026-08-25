import { useState } from 'react';
import { Link, NavLink, useLocation, useNavigate } from 'react-router-dom';
import { AppBar, Avatar, Box, Chip, Divider, Drawer, IconButton, List, ListItemButton, ListItemIcon, ListItemText, Stack, Toolbar, Tooltip, Typography } from '@mui/material';
import DashboardOutlinedIcon from '@mui/icons-material/DashboardOutlined';
import ViewKanbanOutlinedIcon from '@mui/icons-material/ViewKanbanOutlined';
import WorkOutlineRoundedIcon from '@mui/icons-material/WorkOutlineRounded';
import GroupsOutlinedIcon from '@mui/icons-material/GroupsOutlined';
import RateReviewOutlinedIcon from '@mui/icons-material/RateReviewOutlined';
import MenuRoundedIcon from '@mui/icons-material/MenuRounded';
import PublicRoundedIcon from '@mui/icons-material/PublicRounded';
import ContactSupportOutlinedIcon from '@mui/icons-material/ContactSupportOutlined';
import ChevronLeftRoundedIcon from '@mui/icons-material/ChevronLeftRounded';
import ChevronRightRoundedIcon from '@mui/icons-material/ChevronRightRounded';
import LogoutRoundedIcon from '@mui/icons-material/LogoutRounded';
import { useAuth } from '../auth/authContext';

const expandedWidth = 248;
const collapsedWidth = 76;

const areaConfig = {
  candidate: {
    label: 'Candidate portal',
    initials: 'CA',
    links: [
      { label: 'Overview', to: '/candidate', icon: <DashboardOutlinedIcon /> },
      { label: 'Browse jobs', to: '/jobs', icon: <WorkOutlineRoundedIcon /> },
    ],
  },
  hr: {
    label: 'HR workspace',
    initials: 'HR',
    links: [
      { label: 'Hiring pipeline', to: '/hr', icon: <ViewKanbanOutlinedIcon /> },
      { label: 'Job management', to: '/hr/jobs', icon: <WorkOutlineRoundedIcon /> },
      { label: 'Candidate record', to: '/hr/candidates/demo', icon: <GroupsOutlinedIcon /> },
    ],
  },
  interviewer: {
    label: 'Interviewer workspace',
    initials: 'IV',
    links: [
      { label: 'My interviews', to: '/interviewer', icon: <GroupsOutlinedIcon /> },
      { label: 'Candidate review', to: '/interviewer/interviews/demo', icon: <RateReviewOutlinedIcon /> },
    ],
  },
};

function getArea(pathname) {
  if (pathname.startsWith('/hr')) return 'hr';
  if (pathname.startsWith('/interviewer')) return 'interviewer';
  return 'candidate';
}

export default function DashboardShell({ children }) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [mobileOpen, setMobileOpen] = useState(false);
  const [collapsed, setCollapsed] = useState(false);
  const { pathname } = useLocation();
  const activeArea = getArea(pathname);
  const config = areaConfig[activeArea];
  const desktopWidth = collapsed ? collapsedWidth : expandedWidth;
  const userInitials = `${user?.firstName?.[0] || ''}${user?.lastName?.[0] || ''}`.toUpperCase() || config.initials;

  const handleLogout = async () => {
    try {
      await logout();
    } finally {
      navigate('/', { replace: true });
    }
  };

  const drawerContent = (isCollapsed) => {
    const labelSx = {
      opacity: isCollapsed ? 0 : 1,
      width: isCollapsed ? 0 : 'auto',
      overflow: 'hidden',
      whiteSpace: 'nowrap',
      transition: 'opacity 140ms ease, width 180ms ease',
    };
    const navItemSx = {
      mb: 0.5,
      minHeight: 44,
      px: isCollapsed ? 1.5 : 2,
      justifyContent: isCollapsed ? 'center' : 'flex-start',
      borderRadius: 1.5,
      color: 'rgba(255,255,255,0.72)',
      '& .MuiListItemIcon-root': { color: 'inherit', minWidth: isCollapsed ? 0 : 38, justifyContent: 'center' },
      '&:hover': { bgcolor: 'rgba(255,255,255,0.08)', color: '#fff' },
      '&.active': { bgcolor: 'rgba(8,127,140,0.34)', color: '#fff' },
    };

    return (
      <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column', overflowX: 'hidden', bgcolor: 'primary.dark', color: 'primary.contrastText' }}>
        <Stack direction="row" alignItems="center" justifyContent={isCollapsed ? 'center' : 'space-between'} spacing={1} sx={{ px: isCollapsed ? 1 : 2, height: 72, flexShrink: 0 }}>
          {!isCollapsed && (
            <Stack direction="row" alignItems="center" spacing={1.25} sx={{ minWidth: 0 }}>
              <Box sx={{ display: 'grid', placeItems: 'center', width: 34, height: 34, flexShrink: 0, borderRadius: 1.5, bgcolor: 'secondary.main' }}><WorkOutlineRoundedIcon fontSize="small" /></Box>
              <Typography fontWeight={900} letterSpacing="0.16em" sx={{ whiteSpace: 'nowrap' }}>SHIGOTO</Typography>
            </Stack>
          )}
          <Tooltip title={isCollapsed ? 'Expand sidebar' : 'Collapse sidebar'} placement="right">
            <IconButton
              aria-label={isCollapsed ? 'Expand sidebar' : 'Collapse sidebar'}
              onClick={() => setCollapsed((current) => !current)}
              sx={{ display: { xs: 'none', md: 'inline-flex' }, color: 'rgba(255,255,255,0.76)', '&:hover': { bgcolor: 'rgba(255,255,255,0.1)', color: '#fff' } }}
            >
              {isCollapsed ? <ChevronRightRoundedIcon /> : <ChevronLeftRoundedIcon />}
            </IconButton>
          </Tooltip>
          {isCollapsed && <WorkOutlineRoundedIcon fontSize="small" sx={{ display: { md: 'none' } }} />}
        </Stack>
        <Divider sx={{ borderColor: 'rgba(255,255,255,0.12)' }} />

        <Box sx={{ px: 2.5, pt: isCollapsed ? 0 : 2.5, pb: isCollapsed ? 0 : 1, maxHeight: isCollapsed ? 0 : 76, opacity: isCollapsed ? 0 : 1, overflow: 'hidden', transition: 'max-height 180ms ease, opacity 140ms ease, padding 180ms ease' }}>
          <Typography variant="overline" sx={{ color: 'rgba(255,255,255,0.5)', fontWeight: 700, whiteSpace: 'nowrap' }}>Current area</Typography>
          <Typography variant="body2" fontWeight={700} sx={{ whiteSpace: 'nowrap' }}>{config.label}</Typography>
        </Box>

        <List sx={{ px: 1.25, pt: isCollapsed ? 2 : 1 }}>
          {config.links.map((link) => (
            <Tooltip key={link.to} title={isCollapsed ? link.label : ''} placement="right">
              <ListItemButton component={NavLink} to={link.to} end={link.to === `/${activeArea}`} onClick={() => setMobileOpen(false)} aria-label={link.label} sx={navItemSx}>
                <ListItemIcon>{link.icon}</ListItemIcon>
                <ListItemText primary={link.label} primaryTypographyProps={{ variant: 'body2', fontWeight: 650 }} sx={labelSx} />
              </ListItemButton>
            </Tooltip>
          ))}
        </List>

        <Box sx={{ mt: 'auto', px: 1.25, pb: 2 }}>
          <Divider sx={{ mb: 1, borderColor: 'rgba(255,255,255,0.12)' }} />
          <Tooltip title={isCollapsed ? 'Contact Us' : ''} placement="right">
            <ListItemButton component={Link} to="/contact" onClick={() => setMobileOpen(false)} aria-label="Contact Us" sx={navItemSx}>
              <ListItemIcon><ContactSupportOutlinedIcon /></ListItemIcon>
              <ListItemText primary="Contact Us" primaryTypographyProps={{ variant: 'body2', fontWeight: 650 }} sx={labelSx} />
            </ListItemButton>
          </Tooltip>
          <Tooltip title={isCollapsed ? 'Public website' : ''} placement="right">
            <ListItemButton component={Link} to="/" onClick={() => setMobileOpen(false)} aria-label="Public website" sx={navItemSx}>
              <ListItemIcon><PublicRoundedIcon /></ListItemIcon>
              <ListItemText primary="Public website" primaryTypographyProps={{ variant: 'body2', fontWeight: 650 }} sx={labelSx} />
            </ListItemButton>
          </Tooltip>
          <Tooltip title={isCollapsed ? 'Log out' : ''} placement="right">
            <ListItemButton onClick={handleLogout} aria-label="Log out" sx={navItemSx}>
              <ListItemIcon><LogoutRoundedIcon /></ListItemIcon>
              <ListItemText primary="Log out" primaryTypographyProps={{ variant: 'body2', fontWeight: 650 }} sx={labelSx} />
            </ListItemButton>
          </Tooltip>
        </Box>
      </Box>
    );
  };

  return (
    <Box sx={{ minHeight: '100vh', bgcolor: '#F1F5F9' }}>
      <AppBar
        position="fixed"
        color="inherit"
        sx={{
          zIndex: (theme) => theme.zIndex.drawer + 1,
          ml: { md: `${desktopWidth}px` },
          width: { md: `calc(100% - ${desktopWidth}px)` },
          borderBottom: 1,
          borderColor: 'divider',
          transition: (theme) => theme.transitions.create(['margin-left', 'width'], { duration: theme.transitions.duration.shorter }),
        }}
      >
        <Toolbar sx={{ minHeight: 64 }}>
          <IconButton aria-label="Open workspace navigation" onClick={() => setMobileOpen(true)} sx={{ display: { md: 'none' }, mr: 1 }}><MenuRoundedIcon /></IconButton>
          <Box sx={{ flex: 1 }}><Typography variant="body2" fontWeight={700}>{config.label}</Typography><Typography variant="caption" color="text.secondary">{user?.firstName} {user?.lastName}</Typography></Box>
          <Chip label={user?.role} size="small" variant="outlined" sx={{ mr: 1.5 }} />
          <Avatar sx={{ width: 34, height: 34, bgcolor: 'secondary.main', fontSize: 13 }}>{userInitials}</Avatar>
        </Toolbar>
      </AppBar>

      <Drawer
        variant="permanent"
        open
        sx={{
          display: { xs: 'none', md: 'block' },
          width: desktopWidth,
          flexShrink: 0,
          transition: (theme) => theme.transitions.create('width', { duration: theme.transitions.duration.shorter }),
          '& .MuiDrawer-paper': { width: desktopWidth, overflowX: 'hidden', border: 0, transition: (theme) => theme.transitions.create('width', { duration: theme.transitions.duration.shorter }) },
        }}
      >
        {drawerContent(collapsed)}
      </Drawer>
      <Drawer variant="temporary" open={mobileOpen} onClose={() => setMobileOpen(false)} sx={{ display: { md: 'none' }, '& .MuiDrawer-paper': { width: expandedWidth, border: 0 } }}>
        {drawerContent(false)}
      </Drawer>

      <Box component="main" sx={{ ml: { md: `${desktopWidth}px` }, pt: 8, minHeight: '100vh', transition: (theme) => theme.transitions.create('margin-left', { duration: theme.transitions.duration.shorter }) }}>
        <Box sx={{ width: '100%', maxWidth: 1440, mx: 'auto', px: { xs: 2, sm: 3, lg: 4 }, pb: 7 }}>{children}</Box>
      </Box>
    </Box>
  );
}
