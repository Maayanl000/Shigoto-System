import { Chip, Stack } from '@mui/material';
import PageSkeleton from '../components/PageSkeleton';

export default function InterviewerDashboard() {
  return (
    <PageSkeleton
      title="My Interviews"
      description="Interviewer dashboard for assigned candidate reviews."
      sections={[
        { title: 'Upcoming Interviews', content: <Stack direction="row" spacing={1}><Chip label="No live schedule" size="small" /><span>Assigned interviews will appear here.</span></Stack>, wide: true },
        { title: 'Awaiting Feedback', content: 'Completed interviews that still require feedback will appear here.' },
        { title: 'Completed Reviews', content: 'Previously submitted interview reviews will appear here.' },
      ]}
    />
  );
}
