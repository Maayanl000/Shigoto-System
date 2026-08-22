import { useState } from 'react';
import { Avatar, Box, Button, Chip, Divider, Drawer, IconButton, List, ListItem, ListItemIcon, ListItemText, Stack, TextField, Typography } from '@mui/material';
import CloseRoundedIcon from '@mui/icons-material/CloseRounded';
import EmailOutlinedIcon from '@mui/icons-material/EmailOutlined';
import LocationOnOutlinedIcon from '@mui/icons-material/LocationOnOutlined';
import AssignmentOutlinedIcon from '@mui/icons-material/AssignmentOutlined';
import CalendarMonthOutlinedIcon from '@mui/icons-material/CalendarMonthOutlined';
import NotesOutlinedIcon from '@mui/icons-material/NotesOutlined';
import ActionDialog from './ActionDialog';

export default function CandidateDrawer({ candidate, open, onClose }) {
  const [action, setAction] = useState(null);
  const closeAction = () => setAction(null);

  return (
    <>
      <Drawer anchor="right" open={open} onClose={onClose}>
        <Box sx={{ width: { xs: '100vw', sm: 480 }, maxWidth: '100vw' }} role="dialog" aria-label="Candidate details">
          <Box sx={{ position: 'sticky', top: 0, zIndex: 1, bgcolor: 'background.paper', borderBottom: 1, borderColor: 'divider', px: 3, py: 2 }}>
            <Stack direction="row" justifyContent="space-between" alignItems="center">
              <Box>
                <Typography variant="overline" color="secondary.dark" fontWeight={800}>Candidate details</Typography>
                <Typography variant="h6">Pipeline review</Typography>
              </Box>
              <IconButton aria-label="Close candidate details" onClick={onClose}><CloseRoundedIcon /></IconButton>
            </Stack>
          </Box>

          {candidate && (
            <Box sx={{ p: 3 }}>
              <Stack direction="row" alignItems="center" spacing={2}>
                <Avatar sx={{ width: 58, height: 58, bgcolor: 'primary.main', fontWeight: 800 }}>{candidate.initials}</Avatar>
                <Box>
                  <Typography variant="h6">{candidate.name}</Typography>
                  <Typography variant="body2" color="text.secondary">{candidate.role}</Typography>
                  <Chip label={candidate.stage} color="secondary" size="small" variant="outlined" sx={{ mt: 1 }} />
                </Box>
              </Stack>

              <List dense sx={{ mt: 2 }}>
                <ListItem disableGutters><ListItemIcon sx={{ minWidth: 36 }}><EmailOutlinedIcon fontSize="small" /></ListItemIcon><ListItemText primary="candidate.preview@shigoto.local" secondary="Local frontend mock contact" /></ListItem>
                <ListItem disableGutters><ListItemIcon sx={{ minWidth: 36 }}><LocationOnOutlinedIcon fontSize="small" /></ListItemIcon><ListItemText primary={candidate.location} secondary="Preferred work location" /></ListItem>
              </List>
              <Divider sx={{ my: 2 }} />

              <Typography variant="h6" sx={{ mb: 1 }}>Application summary</Typography>
              <Typography variant="body2" color="text.secondary" sx={{ lineHeight: 1.7 }}>
                Applied for {candidate.role}. Skills, resume details, and application history will be loaded from the existing backend when integration is implemented.
              </Typography>

              <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1} sx={{ my: 3 }}>
                <Button variant="contained" startIcon={<AssignmentOutlinedIcon />} onClick={() => setAction('task')}>Send home task</Button>
                <Button variant="outlined" startIcon={<CalendarMonthOutlinedIcon />} onClick={() => setAction('interview')}>Schedule interview</Button>
              </Stack>

              <Divider sx={{ mb: 2 }} />
              <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 1 }}>
                <NotesOutlinedIcon fontSize="small" color="action" />
                <Typography variant="h6">Internal notes</Typography>
              </Stack>
              <Box sx={{ p: 2, bgcolor: '#f8fafc', border: 1, borderStyle: 'dashed', borderColor: 'divider', borderRadius: 1.5 }}>
                <Typography variant="body2" color="text.secondary">No notes are connected. This drawer uses local frontend mock data.</Typography>
              </Box>
            </Box>
          )}
        </Box>
      </Drawer>

      <ActionDialog open={action === 'task'} onClose={closeAction} title="Send home task" description={candidate ? `Prepare an assessment for ${candidate.name}.` : ''} confirmLabel="Send task">
        <TextField label="Task title" disabled fullWidth />
        <TextField label="Instructions" multiline minRows={4} disabled fullWidth />
        <TextField label="Due date" type="date" disabled fullWidth InputLabelProps={{ shrink: true }} />
      </ActionDialog>
      <ActionDialog open={action === 'interview'} onClose={closeAction} title="Schedule interview" description={candidate ? `Plan an interview with ${candidate.name}.` : ''} confirmLabel="Schedule interview">
        <TextField label="Interview round" disabled fullWidth />
        <TextField label="Date and time" type="datetime-local" disabled fullWidth InputLabelProps={{ shrink: true }} />
        <TextField label="Interviewer" disabled fullWidth />
      </ActionDialog>
    </>
  );
}
