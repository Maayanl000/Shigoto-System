import { Alert, Button, Dialog, DialogActions, DialogContent, DialogTitle, Divider, Grid, IconButton, Stack, TextField, Typography } from '@mui/material';
import CloseRoundedIcon from '@mui/icons-material/CloseRounded';

export default function ApplicationDialog({ open, onClose, job }) {
  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="md">
      <DialogTitle component="div">
        <Stack direction="row" justifyContent="space-between" alignItems="flex-start" spacing={2}>
          <div>
            <Typography variant="overline" color="secondary.dark" fontWeight={800}>Apply to Shigoto</Typography>
            <Typography variant="h6">{job?.title || 'Selected position'}</Typography>
            <Typography variant="body2" color="text.secondary">{job?.location || 'Location pending'} · {job?.type || 'Full-time'}</Typography>
          </div>
          <IconButton aria-label="Close application dialog" onClick={onClose} size="small"><CloseRoundedIcon /></IconButton>
        </Stack>
      </DialogTitle>
      <Divider />
      <DialogContent sx={{ display: 'grid', gap: 2.5, pt: 3 }}>
        <Alert severity="info" icon={false}>Application submission is not connected yet. Fields are disabled in this UI preview.</Alert>
        <Grid container spacing={2}>
          <Grid size={{ xs: 12, md: 6 }}><TextField label="Full name" disabled fullWidth /></Grid>
          <Grid size={{ xs: 12, md: 6 }}><TextField label="Email address" type="email" disabled fullWidth /></Grid>
        </Grid>
        <TextField label="Cover note" multiline minRows={4} disabled fullWidth />
        <Typography variant="body2" color="text.secondary">Resume upload will be added after backend integration. No files can be attached yet.</Typography>
      </DialogContent>
      <DialogActions sx={{ px: 3, py: 2.5 }}>
        <Button onClick={onClose} color="inherit">Cancel</Button>
        <Button variant="contained" disabled>Submit application</Button>
      </DialogActions>
    </Dialog>
  );
}
