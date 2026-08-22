import { Alert, Button, Dialog, DialogActions, DialogContent, DialogTitle, Divider, IconButton, Stack, Typography } from '@mui/material';
import CloseRoundedIcon from '@mui/icons-material/CloseRounded';

export default function ActionDialog({ open, onClose, title, description, confirmLabel, children }) {
  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm">
      <DialogTitle component="div" sx={{ pb: 2 }}>
        <Stack direction="row" justifyContent="space-between" alignItems="flex-start" spacing={2}>
          <BoxTitle title={title} description={description} />
          <IconButton aria-label="Close dialog" onClick={onClose} size="small"><CloseRoundedIcon /></IconButton>
        </Stack>
      </DialogTitle>
      <Divider />
      <DialogContent sx={{ display: 'grid', gap: 2, pt: 3 }}>
        <Alert severity="info" icon={false}>UI preview only. This action is not connected to the backend.</Alert>
        {children}
      </DialogContent>
      <DialogActions sx={{ px: 3, py: 2.5 }}>
        <Button onClick={onClose} color="inherit">Cancel</Button>
        <Button variant="contained" disabled>{confirmLabel}</Button>
      </DialogActions>
    </Dialog>
  );
}

function BoxTitle({ title, description }) {
  return (
    <div>
      <Typography variant="h6">{title}</Typography>
      {description && <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>{description}</Typography>}
    </div>
  );
}
