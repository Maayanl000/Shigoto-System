import { useState } from 'react';
import { Box, Card, CardActionArea, CardContent, Chip, Divider, Paper, Stack, Typography } from '@mui/material';
import AccessTimeRoundedIcon from '@mui/icons-material/AccessTimeRounded';
import PageSkeleton from '../components/PageSkeleton';
import CandidateDrawer from '../components/CandidateDrawer';

// Local frontend-only examples used to make the planned Kanban layout visible.
const mockColumns = [
  { title: 'Applied', color: '#64748b', candidates: [{ name: 'Amit Mizrahi', initials: 'AM', role: 'Frontend Developer', location: 'Tel Aviv', age: '1 day' }, { name: 'Noa Cohen', initials: 'NC', role: 'Backend Engineer', location: 'Hybrid', age: '2 days' }] },
  { title: 'Screening', color: '#2563eb', candidates: [{ name: 'Daniel Levi', initials: 'DL', role: 'Product Designer', location: 'Remote', age: '3 days' }] },
  { title: 'Task', color: '#7c3aed', candidates: [{ name: 'Maya Shalev', initials: 'MS', role: 'Full Stack Developer', location: 'Jerusalem', age: 'Due Friday' }] },
  { title: 'Interview', color: '#087f8c', candidates: [{ name: 'Eitan Bar', initials: 'EB', role: 'Backend Engineer', location: 'Tel Aviv', age: 'Tomorrow' }] },
  { title: 'Decision', color: '#d97706', candidates: [] },
];

export default function HrDashboard() {
  const [selectedCandidate, setSelectedCandidate] = useState(null);

  return (
    <PageSkeleton title="HR Dashboard" description="Static hiring pipeline preview. Drag-and-drop is not enabled.">
      <Paper variant="outlined" sx={{ display: 'flex', alignItems: 'center', flexWrap: 'wrap', gap: 2, p: 2, mb: 2.5 }}>
        <Box>
          <Typography variant="caption" color="text.secondary">Pipeline view</Typography>
          <Typography variant="body2" fontWeight={700}>All open positions</Typography>
        </Box>
        <Divider orientation="vertical" flexItem />
        <Chip label="5 local mock candidates" size="small" color="secondary" variant="outlined" />
        <Typography variant="caption" color="text.secondary" sx={{ ml: { md: 'auto' } }}>
          Local frontend data · cards are not draggable
        </Typography>
      </Paper>
      <Box sx={{ overflowX: 'auto', pb: 1 }}>
        <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(5, minmax(210px, 1fr))', gap: 2, minWidth: 1120 }}>
          {mockColumns.map((column) => (
            <Box key={column.title}>
              <Paper variant="outlined" sx={{ p: 1.5, minHeight: 390, bgcolor: '#f8fafc', borderTop: 3, borderTopColor: column.color }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1.5 }}>
                  <Typography variant="subtitle1" fontWeight="bold">{column.title}</Typography>
                  <Chip label={column.candidates.length} size="small" sx={{ bgcolor: 'background.paper' }} />
                </Box>
                {column.candidates.map((candidate) => (
                  <Card key={candidate.name} sx={{ mb: 1.25, bgcolor: 'background.paper' }}>
                    <CardActionArea onClick={() => setSelectedCandidate({ ...candidate, stage: column.title })} aria-label={`Open details for ${candidate.name}`}>
                      <CardContent sx={{ p: 2, '&:last-child': { pb: 2 } }}>
                        <Typography variant="body2" fontWeight={700} sx={{ mb: 0.5 }}>{candidate.name}</Typography>
                        <Typography variant="caption" color="text.secondary">{candidate.role}</Typography>
                        <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mt: 1.5 }}>
                          <Chip label={candidate.location} size="small" variant="outlined" />
                          <Stack direction="row" alignItems="center" spacing={0.4} color="text.secondary">
                            <AccessTimeRoundedIcon sx={{ fontSize: 14 }} />
                            <Typography variant="caption">{candidate.age}</Typography>
                          </Stack>
                        </Stack>
                      </CardContent>
                    </CardActionArea>
                  </Card>
                ))}
                {column.candidates.length === 0 && (
                  <Box sx={{ py: 4, px: 2, textAlign: 'center', border: 1, borderStyle: 'dashed', borderColor: 'divider', borderRadius: 2, bgcolor: 'background.paper' }}>
                    <Typography variant="body2" color="text.secondary">No candidates in this stage</Typography>
                  </Box>
                )}
              </Paper>
            </Box>
          ))}
        </Box>
      </Box>
      <CandidateDrawer candidate={selectedCandidate} open={Boolean(selectedCandidate)} onClose={() => setSelectedCandidate(null)} />
    </PageSkeleton>
  );
}
