import { Box, Card, CardContent, Chip, Container, Grid, LinearProgress, Stack, Typography } from '@mui/material';
import PersonOutlineRoundedIcon from '@mui/icons-material/PersonOutlineRounded';
import ViewKanbanOutlinedIcon from '@mui/icons-material/ViewKanbanOutlined';
import RateReviewOutlinedIcon from '@mui/icons-material/RateReviewOutlined';

const workspaces = [
  {
    title: 'Candidate workspace',
    description: 'Track applications, recruitment stages, tasks, and interview updates in one focused view.',
    icon: <PersonOutlineRoundedIcon />,
    preview: 'candidate',
  },
  {
    title: 'HR workspace',
    description: 'See candidates across a structured hiring pipeline while keeping jobs and actions close at hand.',
    icon: <ViewKanbanOutlinedIcon />,
    preview: 'hr',
  },
  {
    title: 'Interviewer workspace',
    description: 'Review upcoming interviews, candidate context, and outstanding feedback without extra noise.',
    icon: <RateReviewOutlinedIcon />,
    preview: 'interviewer',
  },
];

function WorkspaceMiniature({ type }) {
  if (type === 'candidate') {
    return (
      <Box>
        <Stack direction="row" justifyContent="space-between" alignItems="center">
          <Typography variant="caption" fontWeight={800}>Frontend Developer</Typography>
          <Chip label="Review" size="small" color="secondary" variant="outlined" />
        </Stack>
        <Typography variant="caption" color="text.secondary">Application progress</Typography>
        <LinearProgress variant="determinate" value={40} color="secondary" sx={{ height: 6, mt: 1.5, borderRadius: 8 }} />
        <Stack direction="row" justifyContent="space-between" sx={{ mt: 1.25 }}>
          {['Applied', 'Review', 'Task'].map((stage, index) => <Typography key={stage} variant="caption" color={index < 2 ? 'secondary.dark' : 'text.disabled'}>{stage}</Typography>)}
        </Stack>
      </Box>
    );
  }

  if (type === 'hr') {
    return (
      <Grid container spacing={0.75}>
        {['Applied', 'Task', 'Interview'].map((stage, index) => (
          <Grid key={stage} size={4}>
            <Box sx={{ p: 0.8, minHeight: 86, borderRadius: 1, bgcolor: index === 2 ? 'secondary.light' : '#EAF0F6' }}>
              <Typography variant="caption" fontWeight={800}>{stage}</Typography>
              <Box sx={{ mt: 1, height: 22, border: 1, borderColor: 'divider', borderRadius: 0.75, bgcolor: 'background.paper' }} />
              {index === 0 && <Box sx={{ mt: 0.6, height: 22, border: 1, borderColor: 'divider', borderRadius: 0.75, bgcolor: 'background.paper' }} />}
            </Box>
          </Grid>
        ))}
      </Grid>
    );
  }

  return (
    <Stack spacing={0.75}>
      {['10:00 - Technical interview', '14:30 - Candidate review'].map((item, index) => (
        <Stack key={item} direction="row" alignItems="center" spacing={1} sx={{ p: 1, border: 1, borderColor: 'divider', borderRadius: 1, bgcolor: 'background.paper' }}>
          <Box sx={{ width: 8, height: 8, borderRadius: '50%', bgcolor: index === 0 ? 'secondary.main' : 'primary.main' }} />
          <Typography variant="caption" fontWeight={700}>{item}</Typography>
        </Stack>
      ))}
    </Stack>
  );
}

export default function WorkspaceShowcase() {
  return (
    <Box component="section" sx={{ py: { xs: 7, md: 9 }, bgcolor: '#DDE7F0', borderBlock: 1, borderColor: 'divider' }}>
      <Container maxWidth="xl">
        <Box sx={{ maxWidth: 720, mb: 4.5 }}>
          <Typography variant="overline" color="secondary.dark" fontWeight={800}>Shigoto workspaces</Typography>
          <Typography variant="h3" component="h2" sx={{ mt: 0.75, fontSize: { xs: '2rem', md: '2.6rem' } }}>One platform, three focused experiences</Typography>
          <Typography color="text.secondary" sx={{ mt: 1.5, lineHeight: 1.75 }}>Each participant gets a workspace shaped around their role in the same structured recruitment process.</Typography>
        </Box>

        <Grid container spacing={2.5}>
          {workspaces.map((workspace) => (
            <Grid key={workspace.title} size={{ xs: 12, md: 4 }}>
              <Card sx={{ height: '100%', bgcolor: 'background.paper' }}>
                <CardContent sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
                    <Stack direction="row" justifyContent="space-between" alignItems="center">
                      <Box sx={{ display: 'grid', placeItems: 'center', width: 42, height: 42, borderRadius: 1.5, bgcolor: 'secondary.light', color: 'secondary.dark' }}>{workspace.icon}</Box>
                      <Chip label="Development preview" size="small" variant="outlined" />
                    </Stack>
                    <Typography variant="h6" component="h3" sx={{ mt: 2.5 }}>{workspace.title}</Typography>
                    <Typography variant="body2" color="text.secondary" sx={{ mt: 1, minHeight: 64, lineHeight: 1.65 }}>{workspace.description}</Typography>
                    <Box sx={{ mt: 2.5, p: 1.5, border: 1, borderColor: 'divider', borderRadius: 1.5, bgcolor: '#F1F5F9' }}><WorkspaceMiniature type={workspace.preview} /></Box>
                  <Typography variant="body2" fontWeight={800} color="text.secondary" sx={{ mt: 2.25 }}>Preview workspace</Typography>
                </CardContent>
              </Card>
            </Grid>
          ))}
        </Grid>
      </Container>
    </Box>
  );
}
