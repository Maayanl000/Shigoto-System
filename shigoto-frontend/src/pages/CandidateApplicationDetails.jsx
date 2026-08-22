import PageSkeleton from '../components/PageSkeleton';

export default function CandidateApplicationDetails() {
  return (
    <PageSkeleton
      title="Application Details"
      description="Candidate view of one application and its assigned hiring tasks."
      sections={[
        { title: 'Position Summary', content: 'Job title, department, location, and application date will appear here.' },
        { title: 'Application Status', content: 'The current hiring stage and status explanation will appear here.' },
        { title: 'Task Area', content: 'Assessment instructions, due dates, and future submission controls will appear here.', wide: true },
        { title: 'Interview Information', content: 'Upcoming interview details will appear here when available.' },
        { title: 'Activity Timeline', content: 'A chronological history of application events will appear here.' },
      ]}
    />
  );
}
