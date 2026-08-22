import { Alert, Button, Dialog, DialogActions, DialogContent, DialogTitle, Divider, FormControl, IconButton, InputLabel, MenuItem, Select, Stack, TextField, Typography } from '@mui/material';
import CloseRoundedIcon from '@mui/icons-material/CloseRounded';

export default function FeedbackDialog({ open, onClose, candidateName = 'Selected candidate' }) {
  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm">
      <DialogTitle component="div">
        <Stack direction="row" justifyContent="space-between" alignItems="flex-start" spacing={2}>
          <div>
            <Typography variant="overline" color="secondary.dark" fontWeight={800}>Interview feedback</Typography>
            <Typography variant="h6">{candidateName}</Typography>
          </div>
          <IconButton aria-label="Close feedback dialog" onClick={onClose} size="small"><CloseRoundedIcon /></IconButton>
        </Stack>
      </DialogTitle>
      <Divider />
      <DialogContent sx={{ display: 'grid', gap: 2, pt: 3 }}>
        <Alert severity="info" icon={false}>Feedback cannot be saved or submitted in this UI preview.</Alert>
        <FormControl fullWidth size="small" disabled>
          <InputLabel id="feedback-recommendation-label">Recommendation</InputLabel>
          <Select labelId="feedback-recommendation-label" label="Recommendation" value="">
            <MenuItem value="advance">Advance</MenuItem>
            <MenuItem value="hold">Hold</MenuItem>
            <MenuItem value="decline">Do not advance</MenuItem>
          </Select>
        </FormControl>
        <TextField label="Strengths" multiline minRows={3} disabled />
        <TextField label="Concerns" multiline minRows={3} disabled />
        <TextField label="Additional notes" multiline minRows={3} disabled />
      </DialogContent>
      <DialogActions sx={{ px: 3, py: 2.5 }}>
        <Button onClick={onClose} color="inherit">Cancel</Button>
        <Button variant="contained" disabled>Submit feedback</Button>
      </DialogActions>
    </Dialog>
  );
}
