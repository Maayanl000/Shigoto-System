import { Alert, Button, Dialog, DialogActions, DialogContent, DialogTitle, Divider, IconButton, Stack, TextField, Typography } from '@mui/material';
import CloseRoundedIcon from '@mui/icons-material/CloseRounded';

export default function FeedbackDialog({ interview, feedback, onFeedbackChange, onClose, onSubmit, busy, error }) {
  return (
    <Dialog open={Boolean(interview)} onClose={busy ? undefined : onClose} fullWidth maxWidth="sm">
      <DialogTitle component="div">
        <Stack direction="row" justifyContent="space-between" alignItems="flex-start" spacing={2}>
          <div>
            <Typography variant="overline" color="secondary.dark" fontWeight={800}>Interview feedback</Typography>
            <Typography variant="h6">{interview?.candidateName || 'Selected candidate'}</Typography>
            {interview && <Typography variant="body2" color="text.secondary">{interview.jobTitle} · {interview.interviewType} interview</Typography>}
          </div>
          <IconButton aria-label="Close feedback dialog" onClick={onClose} disabled={busy} size="small"><CloseRoundedIcon /></IconButton>
        </Stack>
      </DialogTitle>
      <Divider />
      <DialogContent sx={{ pt: 3 }}>
        <TextField
          label="Feedback"
          value={feedback}
          onChange={(event) => onFeedbackChange(event.target.value)}
          multiline
          minRows={6}
          fullWidth
          autoFocus
          inputProps={{ maxLength: 10000 }}
          helperText={`${feedback.length}/10000 · Submitting will mark this interview completed.`}
          disabled={busy}
        />
        {error && <Alert severity="error" sx={{ mt: 2 }}>{error}</Alert>}
      </DialogContent>
      <DialogActions sx={{ px: 3, py: 2.5 }}>
        <Button onClick={onClose} color="inherit" disabled={busy}>Cancel</Button>
        <Button variant="contained" onClick={onSubmit} disabled={busy || !feedback.trim()}>{busy ? 'Submitting…' : 'Submit feedback'}</Button>
      </DialogActions>
    </Dialog>
  );
}
