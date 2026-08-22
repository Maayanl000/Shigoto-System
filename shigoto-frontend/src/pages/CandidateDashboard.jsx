import PageSkeleton from '../components/PageSkeleton';

export default function CandidateDashboard() {
  return (
    <PageSkeleton
      title="Candidate Dashboard"
      description="An overview of the candidate's job search and application progress."
      sections={[
        { title: 'Profile Summary', content: 'Contact information, profile completion, and resume status will appear here.' },
        { title: 'Active Applications', content: 'Current applications and their latest hiring-stage status will appear here.' },
        { title: 'Upcoming Tasks', content: 'Assigned assessments, interviews, and due dates will appear here.' },
        { title: 'Recent Activity', content: 'Application updates and notifications will appear here.' },
      ]}
    />
  );
}
