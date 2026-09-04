import { Avatar, Box, Chip, Paper, Stack, Typography } from '@mui/material';
import CheckCircleRoundedIcon from '@mui/icons-material/CheckCircleRounded';
import CodeRoundedIcon from '@mui/icons-material/CodeRounded';
import GroupsOutlinedIcon from '@mui/icons-material/GroupsOutlined';

const stages = [
  { label: 'Applied', value: '18', color: '#64748b' },
  { label: 'Technical review', value: '7', color: '#2563eb' },
  { label: 'Interview', value: '4', color: '#087f8c' },
];

export default function RecruitmentVisual() {
  return (
    <Box aria-label="Recruitment workflow illustration" sx={{ position: 'relative', minHeight: { xs: 390, md: 440 }, display: 'grid', placeItems: 'center' }}>
      <Box sx={{ position: 'absolute', inset: '8% 5% 2% 8%', bgcolor: 'secondary.light', borderRadius: '48% 52% 44% 56% / 52% 40% 60% 48%' }} />
      <Paper elevation={0} sx={{ position: 'relative', width: '88%', maxWidth: 500, p: { xs: 2, md: 2.5 }, border: 1, borderColor: 'divider', boxShadow: '0 28px 70px rgba(16,35,61,0.16)' }}>
        <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 2 }}>
          <Box>
            <Typography variant="caption" color="text.secondary">Recruitment pipeline</Typography>
            <Typography variant="h6">Engineering team</Typography>
          </Box>
          <Chip label="Hiring workflow" size="small" color="secondary" variant="outlined" />
        </Stack>
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.25}>
          {stages.map((stage) => (
            <Box key={stage.label} sx={{ flex: 1, p: 1.5, border: 1, borderColor: 'divider', borderTop: 3, borderTopColor: stage.color, borderRadius: 1.5, bgcolor: '#f8fafc' }}>
              <Typography variant="caption" color="text.secondary">{stage.label}</Typography>
              <Typography variant="h5" fontWeight={800} sx={{ mt: 0.5 }}>{stage.value}</Typography>
            </Box>
          ))}
        </Stack>
        <Paper variant="outlined" sx={{ mt: 2, p: 2, bgcolor: 'background.paper' }}>
          <Stack direction="row" alignItems="center" spacing={1.5}>
            <Avatar sx={{ bgcolor: 'primary.main' }}>AM</Avatar>
            <Box sx={{ flex: 1 }}>
              <Typography variant="body2" fontWeight={700}>Frontend Developer</Typography>
              <Typography variant="caption" color="text.secondary">Technical interview · Candidate review</Typography>
            </Box>
            <CheckCircleRoundedIcon color="secondary" />
          </Stack>
        </Paper>
      </Paper>
      <Paper sx={{ position: 'absolute', top: '4%', right: '2%', display: 'flex', alignItems: 'center', gap: 1, p: 1.25 }}>
        <CodeRoundedIcon color="secondary" fontSize="small" />
        <Typography variant="caption" fontWeight={700}>Technical roles</Typography>
      </Paper>
      <Paper sx={{ position: 'absolute', bottom: '3%', left: '1%', display: 'flex', alignItems: 'center', gap: 1, p: 1.25 }}>
        <GroupsOutlinedIcon color="primary" fontSize="small" />
        <Typography variant="caption" fontWeight={700}>Connected teams</Typography>
      </Paper>
    </Box>
  );
}
