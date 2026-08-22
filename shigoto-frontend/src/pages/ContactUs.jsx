import { Box, Button, Card, CardContent, Container, Grid, Stack, TextField, Typography } from '@mui/material';
import EmailOutlinedIcon from '@mui/icons-material/EmailOutlined';
import LocationOnOutlinedIcon from '@mui/icons-material/LocationOnOutlined';
import AccessTimeOutlinedIcon from '@mui/icons-material/AccessTimeOutlined';

const contactItems = [
  { label: 'Email', value: 'Contact channel to be confirmed', icon: <EmailOutlinedIcon /> },
  { label: 'Location', value: 'Israel - Technology recruitment', icon: <LocationOnOutlinedIcon /> },
  { label: 'Response', value: 'Messaging service not connected yet', icon: <AccessTimeOutlinedIcon /> },
];

export default function ContactUs() {
  return (
    <Box sx={{ minHeight: { xs: 680, md: 760 }, bgcolor: 'background.default', py: { xs: 6, md: 9 } }}>
      <Container maxWidth="lg">
        <Box sx={{ mb: { xs: 4, md: 5 }, maxWidth: 720 }}>
          <Typography variant="overline" color="secondary.dark" fontWeight={800}>Contact us</Typography>
          <Typography variant="h2" component="h1" sx={{ mt: 1, fontSize: { xs: '2.3rem', md: '3.3rem' }, fontWeight: 800, lineHeight: 1.1 }}>
            Let&apos;s talk about technology recruitment.
          </Typography>
          <Typography color="text.secondary" sx={{ mt: 2.5, maxWidth: 500, lineHeight: 1.75 }}>
            This page presents the planned contact experience. Contact delivery is intentionally not connected to a backend service yet.
          </Typography>
        </Box>

        <Grid container spacing={{ xs: 3, md: 4 }} alignItems="stretch">
          <Grid size={{ xs: 12, md: 5 }}>
            <Card sx={{ height: '100%', bgcolor: 'background.paper', borderColor: 'divider', boxShadow: '0 10px 28px rgba(16,35,61,0.07)' }}>
              <CardContent sx={{ p: { xs: 3, md: 4 }, '&:last-child': { pb: { xs: 3, md: 4 } } }}>
                <Typography variant="h5" component="h2" fontWeight={750}>Contact information</Typography>
                <Typography variant="body2" color="text.secondary" sx={{ mt: 1, lineHeight: 1.7 }}>
                  These channels are placeholders for the future support experience.
                </Typography>
                <Stack spacing={2.75} sx={{ mt: 4 }}>
                  {contactItems.map((item) => (
                    <Stack key={item.label} direction="row" spacing={2} alignItems="flex-start">
                      <Box sx={{ display: 'grid', placeItems: 'center', width: 42, height: 42, borderRadius: 1.5, bgcolor: 'secondary.light', color: 'secondary.dark', flexShrink: 0 }}>{item.icon}</Box>
                      <Box>
                        <Typography variant="body2" fontWeight={700}>{item.label}</Typography>
                        <Typography variant="body2" color="text.secondary" sx={{ mt: 0.25 }}>{item.value}</Typography>
                      </Box>
                    </Stack>
                  ))}
                </Stack>
              </CardContent>
            </Card>
          </Grid>
          <Grid size={{ xs: 12, md: 7 }}>
            <Card sx={{ height: '100%', bgcolor: 'background.paper', borderColor: 'divider', boxShadow: '0 10px 28px rgba(16,35,61,0.07)' }}>
              <CardContent sx={{ p: { xs: 3, md: 4 }, '&:last-child': { pb: { xs: 3, md: 4 } } }}>
                <Typography variant="h5" component="h2" fontWeight={750}>Send a message</Typography>
                <Typography variant="body2" color="text.secondary" sx={{ mt: 1, mb: 3 }}>Fields are disabled until contact functionality is implemented.</Typography>
                <Grid container spacing={2}>
                  <Grid size={{ xs: 12, sm: 6 }}><TextField label="Full name" disabled fullWidth /></Grid>
                  <Grid size={{ xs: 12, sm: 6 }}><TextField label="Email address" type="email" disabled fullWidth /></Grid>
                  <Grid size={12}><TextField label="Subject" disabled fullWidth /></Grid>
                  <Grid size={12}><TextField label="How can we help?" multiline minRows={6} disabled fullWidth /></Grid>
                </Grid>
                <Button variant="contained" disabled sx={{ mt: 3 }}>Send message</Button>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      </Container>
    </Box>
  );
}
