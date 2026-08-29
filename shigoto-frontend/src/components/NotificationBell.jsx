import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Badge, Box, CircularProgress, Divider, IconButton, List, ListItemButton, ListItemText, Menu, Typography } from '@mui/material';
import NotificationsNoneRoundedIcon from '@mui/icons-material/NotificationsNoneRounded';
import api from '../services/api';

const formatDate = (value) => value ? new Intl.DateTimeFormat(undefined,
  { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value)) : '';

export default function NotificationBell() {
  const navigate = useNavigate();
  const [anchor, setAnchor] = useState(null);
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const response = await api.get('/notifications/mine');
      setNotifications(Array.isArray(response.data) ? response.data : []);
    } finally { setLoading(false); }
  }, []);

  useEffect(() => {
    const frame = window.requestAnimationFrame(load);
    window.addEventListener('shigoto:notifications-changed', load);
    return () => {
      window.cancelAnimationFrame(frame);
      window.removeEventListener('shigoto:notifications-changed', load);
    };
  }, [load]);

  const openNotification = async (notification) => {
    if (!notification.read) {
      await api.put(`/notifications/${notification.notificationId}/read`);
      setNotifications((items) => items.map((item) => item.notificationId === notification.notificationId
        ? { ...item, read: true } : item));
      window.dispatchEvent(new Event('shigoto:notifications-changed'));
    }
    setAnchor(null);
    navigate(notification.applicationId ? `/candidate/applications/${notification.applicationId}` : '/candidate');
  };
  const unread = notifications.filter((item) => !item.read).length;

  return <>
    <IconButton aria-label={`Notifications${unread ? `, ${unread} unread` : ''}`} onClick={(event) => setAnchor(event.currentTarget)} sx={{ mr: 1 }}>
      <Badge badgeContent={unread} color="error" invisible={!unread}><NotificationsNoneRoundedIcon /></Badge>
    </IconButton>
    <Menu anchorEl={anchor} open={Boolean(anchor)} onClose={() => setAnchor(null)}
      slotProps={{ paper: { sx: { width: { xs: 320, sm: 390 }, maxHeight: 480 } } }}>
      <Box sx={{ px: 2, py: 1 }}><Typography variant="subtitle1" fontWeight={800}>Notifications</Typography></Box>
      <Divider />
      {loading && !notifications.length && <Box sx={{ py: 4, display: 'grid', placeItems: 'center' }}><CircularProgress size={24} /></Box>}
      {!loading && !notifications.length && <Typography color="text.secondary" variant="body2" sx={{ px: 2, py: 3 }}>You have no notifications yet.</Typography>}
      <List disablePadding>{notifications.map((item) => <ListItemButton key={item.notificationId} onClick={() => openNotification(item)}
        sx={{ alignItems: 'flex-start', borderLeft: 3, borderLeftColor: item.read ? 'transparent' : 'primary.main', bgcolor: item.read ? 'transparent' : 'action.hover' }}>
        <ListItemText primary={<Typography variant="body2" fontWeight={item.read ? 600 : 850}>{item.title}{!item.read && ' · New'}</Typography>}
          secondary={<><Typography component="span" variant="body2" color="text.secondary">{item.message}</Typography><Typography component="span" display="block" variant="caption" color="text.secondary" sx={{ mt: .5 }}>{formatDate(item.createdAt)}</Typography></>} />
      </ListItemButton>)}</List>
    </Menu>
  </>;
}
