import { Box, Card, CardContent, Chip, Paper, Typography } from '@mui/material';
import PageSkeleton from '../components/PageSkeleton';

// Local frontend-only examples used to make the planned Kanban layout visible.
const mockColumns = [
  { title: 'Applied', candidates: ['Sample Candidate A', 'Sample Candidate B'] },
  { title: 'Screening', candidates: ['Sample Candidate C'] },
  { title: 'Task', candidates: ['Sample Candidate D'] },
  { title: 'Interview', candidates: ['Sample Candidate E'] },
  { title: 'Decision', candidates: [] },
];

export default function HrDashboard() {
  return (
    <PageSkeleton title="HR Dashboard" description="Static hiring pipeline preview. Drag-and-drop is not enabled.">
      <Box sx={{ overflowX: 'auto', pb: 1 }}>
        <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(5, minmax(180px, 1fr))', gap: 2, minWidth: 960 }}>
          {mockColumns.map((column) => (
            <Box key={column.title}>
              <Paper variant="outlined" sx={{ p: 1.5, minHeight: 300, bgcolor: 'action.hover' }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1.5 }}>
                  <Typography variant="subtitle1" fontWeight="bold">{column.title}</Typography>
                  <Chip label={column.candidates.length} size="small" />
                </Box>
                {column.candidates.map((candidate) => (
                  <Card key={candidate} variant="outlined" sx={{ mb: 1 }}>
                    <CardContent sx={{ p: 1.5, '&:last-child': { pb: 1.5 } }}>
                      <Typography variant="body2" fontWeight="medium">{candidate}</Typography>
                      <Typography variant="caption" color="text.secondary">Local mock candidate</Typography>
                    </CardContent>
                  </Card>
                ))}
                {column.candidates.length === 0 && (
                  <Typography variant="body2" color="text.secondary">No candidates in this stage.</Typography>
                )}
              </Paper>
            </Box>
          ))}
        </Box>
      </Box>
    </PageSkeleton>
  );
}
