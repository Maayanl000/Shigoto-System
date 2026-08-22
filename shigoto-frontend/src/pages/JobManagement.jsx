import { Button, Stack } from '@mui/material';
import PageSkeleton from '../components/PageSkeleton';

export default function JobManagement() {
  return (
    <PageSkeleton
      title="Job Management"
      description="HR workspace for creating and maintaining job openings."
      sections={[
        { title: 'Job List', content: 'Open, draft, and closed positions will be listed here.', wide: true },
        { title: 'Job Details', content: 'Role description, requirements, department, and location will appear here.' },
        { title: 'Hiring Team', content: 'Assigned HR owners and interviewers will appear here.' },
        { title: 'Actions', content: <Stack direction="row" spacing={1}><Button variant="contained" disabled>Create job</Button><Button disabled>Edit selected job</Button></Stack>, wide: true },
      ]}
    />
  );
}
