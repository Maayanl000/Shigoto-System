import PageSkeleton from '../components/PageSkeleton';

export default function CandidateDetails() {
  return (
    <PageSkeleton
      title="Candidate Details"
      description="HR review workspace for a candidate in the hiring pipeline."
      sections={[
        { title: 'Candidate Profile', content: 'Contact details, professional summary, and resume link will appear here.' },
        { title: 'Application Summary', content: 'Applied position, source, date, and current stage will appear here.' },
        { title: 'Tasks and Assessments', content: 'Assigned task status and future reviewer notes will appear here.' },
        { title: 'Interview History', content: 'Interview rounds and feedback summaries will appear here.' },
        { title: 'Internal Notes', content: 'HR-only notes and pipeline activity will appear here.', wide: true },
      ]}
    />
  );
}
