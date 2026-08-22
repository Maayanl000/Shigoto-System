import PageSkeleton from '../components/PageSkeleton';

export default function InterviewDetails() {
  return (
    <PageSkeleton
      title="Interview Details"
      description="Candidate review area for an assigned interview."
      sections={[
        { title: 'Interview Overview', content: 'Round, interview type, date, and participants will appear here.' },
        { title: 'Candidate Snapshot', content: 'Candidate background and application summary will appear here.' },
        { title: 'Role Requirements', content: 'Key skills and evaluation criteria for the position will appear here.' },
        { title: 'Interview Guide', content: 'Prepared topics and suggested questions will appear here.' },
        { title: 'Candidate Materials', content: 'Resume and completed assessment references will appear here.', wide: true },
      ]}
    />
  );
}
