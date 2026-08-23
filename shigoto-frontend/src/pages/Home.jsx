import { useState, useEffect } from 'react';
import { Typography, Grid, Card, CardContent, CardActions, Button, Chip, Box, CircularProgress, Container, Divider, Drawer, IconButton, InputAdornment, Stack, TextField } from '@mui/material';
import WorkIcon from '@mui/icons-material/Work';
import LocationOnIcon from '@mui/icons-material/LocationOn';
import ArrowForwardRoundedIcon from '@mui/icons-material/ArrowForwardRounded';
import CloseRoundedIcon from '@mui/icons-material/CloseRounded';
import SearchRoundedIcon from '@mui/icons-material/SearchRounded';
import ApplicationDialog from '../components/ApplicationDialog';
import WorkspaceShowcase from '../components/WorkspaceShowcase';
import api from '../services/api';
import professionalNetworkImage from '../assets/ChatGPT Image Aug 22, 2026, 05_35_31 PM.png';

export default function Home() {
  const [jobs, setJobs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedJob, setSelectedJob] = useState(null);
  const [applicationJob, setApplicationJob] = useState(null);

  // שליפת המשרות מהשרת בעת טעינת העמוד
  useEffect(() => {
    api.get('/jobs')
      .then((response) => {
        setJobs(response.data);
        setLoading(false);
      })
      .catch((error) => {
        console.error('Error fetching jobs:', error);
        // נתוני גיבוי (Fallback) למקרה ששרת ה-Java כרגע כבוי
        setJobs([
          { id: 1, title: 'Full Stack Java Developer', department: 'R&D', location: 'Tel Aviv', type: 'Full-time', isFallback: true },
          { id: 2, title: 'Backend Engineer (Spring Boot)', department: 'Engineering', location: 'Hybrid', type: 'Full-time', isFallback: true },
          { id: 3, title: 'Frontend Developer (React)', department: 'UI/UX', location: 'Remote', type: 'Full-time', isFallback: true },
        ]);
        setLoading(false);
      });
  }, []);

  return (
    <Box sx={{ textAlign: 'left' }}>
      <Box sx={{ position: 'relative', overflow: 'hidden', borderBottom: 1, borderColor: 'divider', bgcolor: 'primary.dark', color: 'primary.contrastText' }}>
        <Box className="hero-visual" sx={{ display: { xs: 'none', md: 'block' }, position: 'absolute', inset: '0 0 0 auto', width: '62%', overflow: 'hidden' }}>
          <Box component="img" className="public-image" src={professionalNetworkImage} alt="Technology professionals collaborating around a laptop in a modern city workspace" sx={{ position: 'absolute', inset: 0, width: '100%', height: '100%', objectFit: 'cover', objectPosition: '68% center' }} />
          <Box sx={{ position: 'absolute', inset: 0, background: 'linear-gradient(90deg, #10233d 0%, rgba(16,35,61,0.96) 12%, rgba(16,35,61,0.64) 38%, rgba(16,35,61,0.10) 76%)' }} />
        </Box>

        <Container maxWidth="xl" sx={{ position: 'relative', zIndex: 1 }}>
          <Grid container alignItems="center" sx={{ minHeight: { md: 620 } }}>
            <Grid className="hero-copy" size={{ xs: 12, md: 6 }} sx={{ py: { xs: 7, md: 9 } }}>
              <Chip label="Technology recruitment, clearly structured" size="small" variant="outlined" sx={{ mb: 3, color: '#9ae0e4', borderColor: 'rgba(112,210,216,0.46)', bgcolor: 'rgba(8,127,140,0.16)' }} />
              <Typography variant="h1" component="h1" sx={{ maxWidth: 720, fontSize: { xs: '2.7rem', sm: '3.6rem', lg: '4.4rem' }, lineHeight: 1.03, fontWeight: 850, letterSpacing: '-0.045em', color: '#fff' }}>
                Build the team behind what&apos;s next.
              </Typography>
              <Typography sx={{ mt: 3, maxWidth: 650, fontSize: { xs: '1.05rem', md: '1.2rem' }, color: 'rgba(255,255,255,0.72)', lineHeight: 1.75 }}>
                Discover technology roles and move through a recruitment process designed to keep candidates, HR teams, and interviewers aligned.
              </Typography>
              <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.5} sx={{ mt: 4 }}>
                <Button component="a" href="#jobs" variant="contained" color="secondary" size="large" startIcon={<SearchRoundedIcon />}>Browse open roles</Button>
                <Button component="a" href="#how-it-works" variant="outlined" size="large" sx={{ color: '#fff', borderColor: 'rgba(255,255,255,0.42)', '&:hover': { borderColor: '#fff', bgcolor: 'rgba(255,255,255,0.06)' } }}>Explore the process</Button>
              </Stack>
              <Stack direction="row" spacing={3} sx={{ mt: 5 }}>
                <Box><Typography variant="h5" fontWeight={800}>{loading ? '—' : jobs.length}</Typography><Typography variant="caption" sx={{ color: 'rgba(255,255,255,0.62)' }}>Open roles</Typography></Box>
                <Divider orientation="vertical" flexItem sx={{ borderColor: 'rgba(255,255,255,0.2)' }} />
                <Box><Typography variant="h5" fontWeight={800}>3</Typography><Typography variant="caption" sx={{ color: 'rgba(255,255,255,0.62)' }}>Recruitment workspaces</Typography></Box>
                <Divider orientation="vertical" flexItem sx={{ borderColor: 'rgba(255,255,255,0.2)' }} />
                <Box><Typography variant="h5" fontWeight={800}>1</Typography><Typography variant="caption" sx={{ color: 'rgba(255,255,255,0.62)' }}>Shared workflow</Typography></Box>
              </Stack>
            </Grid>
          </Grid>
        </Container>
        <Box className="hero-visual" sx={{ display: { xs: 'block', md: 'none' }, position: 'relative', height: { xs: 300, sm: 380 }, mt: -1, overflow: 'hidden' }}>
          <Box component="img" src={professionalNetworkImage} alt="Technology professionals collaborating around a laptop in a modern city workspace" sx={{ position: 'absolute', inset: 0, width: '100%', height: '100%', objectFit: 'cover', objectPosition: '68% center' }} />
          <Box sx={{ position: 'absolute', inset: 0, background: 'linear-gradient(180deg, #10233d 0%, rgba(16,35,61,0.44) 24%, transparent 58%)' }} />
        </Box>
      </Box>

      <Container id="how-it-works" maxWidth="xl" sx={{ py: { xs: 6, md: 8 } }}>
        <Grid container spacing={2}>
          {[
            ['01', 'Discover', 'Explore current technology opportunities and understand what each role needs.'],
            ['02', 'Progress', 'Follow application stages, assigned tasks, and interview information in one place.'],
            ['03', 'Collaborate', 'Give HR teams and technical interviewers clear, focused review spaces.'],
          ].map(([number, title, text]) => (
            <Grid key={number} size={{ xs: 12, md: 4 }}>
              <Box sx={{ display: 'flex', gap: 2, py: 2 }}>
                <Typography color="secondary.dark" fontWeight={900}>{number}</Typography>
                <Box><Typography variant="h6">{title}</Typography><Typography variant="body2" color="text.secondary" sx={{ mt: 1, lineHeight: 1.7 }}>{text}</Typography></Box>
              </Box>
            </Grid>
          ))}
        </Grid>
      </Container>

      <WorkspaceShowcase />

      <Box id="jobs" sx={{ py: { xs: 7, md: 10 }, bgcolor: 'background.default', borderTop: 1, borderColor: 'divider' }}>
        <Container maxWidth="xl">
          <Box sx={{ display: { md: 'flex' }, alignItems: 'flex-end', justifyContent: 'space-between', gap: 3, mb: 4 }}>
            <Box>
              <Typography variant="overline" color="secondary.dark" fontWeight={800}>Open opportunities</Typography>
              <Typography variant="h3" component="h2" sx={{ mt: 0.75, fontSize: { xs: '2rem', md: '2.6rem' }, fontWeight: 800 }}>Find your place at Shigoto</Typography>
              <Typography color="text.secondary" sx={{ mt: 1.5, maxWidth: 620 }}>Browse the roles currently returned by the existing Shigoto jobs service.</Typography>
              <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 1 }}>The existing local fallback listings may appear when the service is unavailable.</Typography>
            </Box>
            {!loading && <Chip label={`${jobs.length} roles available`} color="secondary" variant="outlined" sx={{ mt: { xs: 2, md: 0 }, bgcolor: 'secondary.light' }} />}
          </Box>

          <Box sx={{ display: 'flex', flexDirection: { xs: 'column', md: 'row' }, alignItems: { md: 'center' }, gap: 1.5, p: 2, mb: 3, border: 1, borderColor: 'divider', borderRadius: 2, bgcolor: 'background.paper', boxShadow: '0 4px 16px rgba(16,35,61,0.05)' }}>
            <TextField
              label="Search roles"
              placeholder="Job title or department"
              disabled
              fullWidth
              slotProps={{ input: { startAdornment: <InputAdornment position="start"><SearchRoundedIcon fontSize="small" /></InputAdornment> } }}
            />
            <TextField label="Location" placeholder="All locations" disabled sx={{ minWidth: { md: 220 } }} />
            <Button variant="outlined" disabled sx={{ minWidth: { md: 120 } }}>Filters</Button>
            <Typography variant="caption" color="text.secondary" sx={{ minWidth: { md: 150 }, textAlign: { md: 'right' } }}>Search controls are a UI preview.</Typography>
          </Box>

          {loading ? (
            <Box sx={{ display: 'grid', placeItems: 'center', minHeight: 260, border: 1, borderStyle: 'dashed', borderColor: 'divider', borderRadius: 2, bgcolor: 'background.paper' }}>
              <Stack alignItems="center" spacing={2}><CircularProgress size={30} /><Typography variant="body2" color="text.secondary">Loading open positions…</Typography></Stack>
            </Box>
          ) : jobs.length === 0 ? (
            <Box sx={{ py: 8, textAlign: 'center', border: 1, borderStyle: 'dashed', borderColor: 'divider', borderRadius: 2, bgcolor: 'background.paper' }}>
              <WorkIcon color="disabled" sx={{ fontSize: 40, mb: 1 }} />
              <Typography variant="h6">No open roles right now</Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>Please check again as new technology opportunities are added.</Typography>
            </Box>
          ) : (
            <Grid container spacing={2.5}>
              {jobs.map((job) => (
                <Grid size={{ xs: 12, md: 6, lg: 4 }} key={job.id}>
                  <Card sx={{ height: '100%', display: 'flex', flexDirection: 'column', justifyContent: 'space-between', transition: 'transform 160ms ease, border-color 160ms ease, box-shadow 160ms ease', '&:hover': { transform: 'translateY(-3px)', borderColor: 'secondary.main', boxShadow: '0 12px 30px rgba(16,35,61,0.08)' } }}>
                    <CardContent>
                      <Stack direction="row" justifyContent="space-between" alignItems="flex-start" spacing={2} sx={{ mb: 2.5 }}>
                        <Chip label={job.department || 'Technology'} color="secondary" size="small" sx={{ bgcolor: 'secondary.light', color: 'secondary.dark' }} />
                        <Chip label={job.type || 'Full-time'} size="small" variant="outlined" />
                      </Stack>
                      <Typography variant="h6" component="h3" sx={{ mb: 1 }}>{job.title}</Typography>
                      <Stack direction="row" alignItems="center" spacing={0.75} sx={{ mb: 2, color: 'text.secondary' }}>
                        <LocationOnIcon sx={{ fontSize: 18 }} /><Typography variant="body2">{job.location || 'Israel'}</Typography>
                      </Stack>
                      <Typography variant="body2" color="text.secondary" sx={{ lineHeight: 1.7 }}>Review this technology opportunity and the planned candidate application experience.</Typography>
                    </CardContent>
                    <CardActions sx={{ px: 3, pb: 3, pt: 0 }}>
                      <Button onClick={() => setSelectedJob(job)} variant="outlined" fullWidth endIcon={<ArrowForwardRoundedIcon />}>View role</Button>
                    </CardActions>
                  </Card>
                </Grid>
              ))}
            </Grid>
          )}
        </Container>
      </Box>

      <Drawer anchor="right" open={Boolean(selectedJob)} onClose={() => setSelectedJob(null)}>
        <Box sx={{ width: { xs: '100vw', sm: 520 }, maxWidth: '100vw', p: { xs: 2.5, sm: 4 } }} role="dialog" aria-label="Job details">
          <Stack direction="row" justifyContent="space-between" alignItems="flex-start" spacing={2}>
            <Box>
              <Typography variant="overline" color="secondary.dark" fontWeight={800}>Job details</Typography>
              <Typography variant="h4" component="h2" sx={{ mt: 0.5 }}>{selectedJob?.title}</Typography>
            </Box>
            <IconButton aria-label="Close job details" onClick={() => setSelectedJob(null)}><CloseRoundedIcon /></IconButton>
          </Stack>
          <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap sx={{ my: 3 }}>
            <Chip label={selectedJob?.department || 'Technology'} color="secondary" />
            <Chip icon={<LocationOnIcon />} label={selectedJob?.location || 'Israel'} variant="outlined" />
            <Chip label={selectedJob?.type || 'Full-time'} variant="outlined" />
          </Stack>
          <Divider />
          <Typography variant="h6" sx={{ mt: 3, mb: 1 }}>About the role</Typography>
          <Typography color="text.secondary" sx={{ lineHeight: 1.8 }}>A detailed role description, responsibilities, and requirements will appear here when the existing job data provides them.</Typography>
          <Typography variant="h6" sx={{ mt: 3, mb: 1 }}>Recruitment process</Typography>
          <Typography color="text.secondary" sx={{ lineHeight: 1.8 }}>Candidates may move through application review, a home task, and technical interviews as appropriate for the role.</Typography>
          <Box sx={{ mt: 4, p: 2, bgcolor: 'secondary.light', borderRadius: 1.5 }}><Typography variant="caption" color="secondary.dark" fontWeight={700}>Preview content — detailed backend job data is not connected here.</Typography></Box>
          <Button variant="contained" fullWidth sx={{ mt: 3 }} onClick={() => { setApplicationJob(selectedJob); setSelectedJob(null); }}>Apply to this role</Button>
        </Box>
      </Drawer>
      <ApplicationDialog open={Boolean(applicationJob)} job={applicationJob} onClose={() => setApplicationJob(null)} />
    </Box>
  );
}
