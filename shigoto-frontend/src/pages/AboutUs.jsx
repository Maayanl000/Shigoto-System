import { Box, Card, CardContent, Container, Grid, Stack, Typography } from '@mui/material';
import PersonSearchOutlinedIcon from '@mui/icons-material/PersonSearchOutlined';
import ViewKanbanOutlinedIcon from '@mui/icons-material/ViewKanbanOutlined';
import RateReviewOutlinedIcon from '@mui/icons-material/RateReviewOutlined';
import AccountTreeOutlinedIcon from '@mui/icons-material/AccountTreeOutlined';
import ArrowDownwardRoundedIcon from '@mui/icons-material/ArrowDownwardRounded';
import professionalConnectionsImage from '../assets/shigoto_about_bright.png';

const audiences = [
  { title: 'Candidates', text: 'A clear place to view opportunities, applications, recruitment status, tasks, and interviews.', icon: <PersonSearchOutlinedIcon /> },
  { title: 'HR teams', text: 'A structured view of open roles and candidates moving through the recruitment pipeline.', icon: <ViewKanbanOutlinedIcon /> },
  { title: 'Technical interviewers', text: 'Focused candidate review and interview feedback areas for technical hiring decisions.', icon: <RateReviewOutlinedIcon /> },
];

const workflow = ['Discover roles', 'Apply', 'Review and task', 'Technical interview', 'Hiring decision'];

const principles = [
  'Structured recruitment',
  'Clear collaboration',
  'Focused experiences',
];

export default function AboutUs() {
  return (
    <Box sx={{ bgcolor: 'background.default' }}>
      <Box
        component="section"
        sx={{
          position: 'relative',
          minHeight: { xs: 620, sm: 560, md: 580 },
          display: 'flex',
          alignItems: 'center',
          overflow: 'hidden',
          bgcolor: 'primary.dark',
          backgroundImage: `url("${professionalConnectionsImage}")`,
          backgroundRepeat: 'no-repeat',
          backgroundSize: 'cover',
          backgroundPosition: { xs: '60% center', sm: '58% center', md: 'center' },
        }}
      >
        <Box
          aria-hidden="true"
          sx={{
            position: 'absolute',
            inset: 0,
            background: {
              xs: 'linear-gradient(90deg, rgba(8, 25, 45, 0.92) 0%, rgba(8, 25, 45, 0.78) 58%, rgba(8, 25, 45, 0.38) 100%)',
              md: 'linear-gradient(90deg, rgba(8, 25, 45, 0.90) 0%, rgba(8, 25, 45, 0.74) 38%, rgba(8, 25, 45, 0.28) 64%, rgba(8, 25, 45, 0.04) 100%)',
            },
          }}
        />
        <Container maxWidth="lg" sx={{ position: 'relative', zIndex: 1, py: { xs: 6, md: 7 } }}>
          <Box className="public-visual-enter" sx={{ maxWidth: { xs: 620, md: 680 } }}>
            <Typography variant="overline" sx={{ color: '#70d2d8', fontWeight: 800 }}>About Shigoto</Typography>
            <Typography variant="h2" component="h1" sx={{ mt: 1, maxWidth: 700, color: 'common.white', fontSize: { xs: '2.4rem', md: '3.55rem' }, fontWeight: 800, lineHeight: 1.06 }}>
              We connect people, technology and opportunity.
            </Typography>
            <Typography sx={{ mt: 3, maxWidth: 640, color: 'rgba(255,255,255,0.82)', fontSize: '1.08rem', lineHeight: 1.75 }}>
              Shigoto is an applicant tracking system concept that brings candidates, HR teams, and technical interviewers into one clear recruitment workflow.
            </Typography>
            <Typography sx={{ mt: 1.5, maxWidth: 640, color: 'rgba(255,255,255,0.72)', lineHeight: 1.75 }}>
              Each participant sees the information and next steps relevant to their part of the recruitment process.
            </Typography>
            <Stack spacing={1.25} sx={{ mt: 3 }}>
              {principles.map((principle) => (
                <Stack key={principle} direction="row" spacing={1.5} alignItems="center">
                  <Box sx={{ width: 8, height: 8, borderRadius: '50%', bgcolor: 'secondary.main', flexShrink: 0 }} />
                  <Typography variant="body2" fontWeight={700} sx={{ color: '#F8FAFC', textShadow: '0 1px 2px rgba(8, 25, 45, 0.55)' }}>{principle}</Typography>
                </Stack>
              ))}
            </Stack>
          </Box>
        </Container>
      </Box>

      <Box sx={{ bgcolor: 'background.paper', borderBlock: 1, borderColor: 'divider' }}>
        <Container maxWidth="lg" sx={{ py: { xs: 6, md: 8 } }}>
          <Typography variant="overline" color="secondary.dark" fontWeight={800}>Purpose-built perspectives</Typography>
          <Typography variant="h4" component="h2" sx={{ mt: 0.75 }}>One process, focused experiences</Typography>
          <Typography color="text.secondary" sx={{ mt: 1.5, mb: 4, maxWidth: 680, lineHeight: 1.7 }}>
            Shigoto keeps the shared recruitment journey connected while giving each participant a workspace focused on their responsibilities.
          </Typography>
          <Grid container spacing={2.5}>
            {audiences.map((audience) => (
              <Grid key={audience.title} size={{ xs: 12, md: 4 }}>
                <Card sx={{ height: '100%', bgcolor: 'background.paper' }}>
                  <CardContent>
                    <Box sx={{ display: 'grid', placeItems: 'center', width: 44, height: 44, mb: 2.5, borderRadius: 2, bgcolor: 'secondary.light', color: 'secondary.dark' }}>{audience.icon}</Box>
                    <Typography variant="h6" component="h3" sx={{ mb: 1 }}>{audience.title}</Typography>
                    <Typography variant="body2" color="text.secondary" sx={{ lineHeight: 1.75 }}>{audience.text}</Typography>
                  </CardContent>
                </Card>
              </Grid>
            ))}
          </Grid>
        </Container>
      </Box>

      <Container maxWidth="lg" sx={{ pb: { xs: 7, md: 9 } }}>
        <Box sx={{ py: { xs: 6, md: 8 }, textAlign: 'center' }}>
          <Box sx={{ display: 'grid', placeItems: 'center', width: 42, height: 42, mx: 'auto', mb: 2, borderRadius: '50%', bgcolor: 'secondary.light', color: 'secondary.dark' }}>
            <ArrowDownwardRoundedIcon />
          </Box>
          <Typography variant="overline" color="secondary.dark" fontWeight={800}>A structured recruitment workflow</Typography>
          <Typography variant="h4" component="h2" sx={{ mt: 0.75 }}>How Shigoto works</Typography>
        </Box>

        <Box sx={{ p: { xs: 3, md: 4 }, bgcolor: '#edf6f7', border: 1, borderColor: '#cee8ea', borderRadius: 2 }}>
          <Stack direction="row" spacing={1.5} alignItems="center" sx={{ mb: 3 }}>
            <AccountTreeOutlinedIcon color="secondary" />
            <Typography variant="h6" component="h2">A structured recruitment workflow</Typography>
          </Stack>
          <Grid container spacing={1.5}>
            {workflow.map((step, index) => (
              <Grid key={step} size={{ xs: 12, sm: 6, md: 2.4 }}>
                <Box sx={{ p: 2, height: '100%', bgcolor: 'background.paper', border: 1, borderColor: 'divider', borderRadius: 1.5 }}>
                  <Typography variant="caption" color="secondary.dark" fontWeight={800}>STEP {index + 1}</Typography>
                  <Typography variant="body2" fontWeight={700} sx={{ mt: 0.75 }}>{step}</Typography>
                </Box>
              </Grid>
            ))}
          </Grid>
        </Box>
      </Container>
    </Box>
  );
}
