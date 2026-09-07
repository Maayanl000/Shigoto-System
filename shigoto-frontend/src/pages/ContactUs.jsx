import { Box, Card, CardContent, Container, Stack, Typography } from '@mui/material';
import EmailOutlinedIcon from '@mui/icons-material/EmailOutlined';
import PhoneOutlinedIcon from '@mui/icons-material/PhoneOutlined';
import LocationOnOutlinedIcon from '@mui/icons-material/LocationOnOutlined';

const contactItems = [
  { label: 'Email', value: 'contact@shigoto.com', icon: <EmailOutlinedIcon /> },
  { label: 'Phone', value: '+972-3-555-0100', icon: <PhoneOutlinedIcon /> },
  { label: 'Location', value: 'Israel - Technology recruitment', icon: <LocationOnOutlinedIcon /> },
];

/**
 * Renders the Contact Us page with Shigoto's static contact details.
 */
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
            Get in touch with Shigoto for general information about our technology recruitment platform.
          </Typography>
        </Box>

        <Card sx={{ maxWidth: 640, bgcolor: 'background.paper', borderColor: 'divider', boxShadow: '0 10px 28px rgba(16,35,61,0.07)' }}>
          <CardContent sx={{ p: { xs: 3, md: 4 }, '&:last-child': { pb: { xs: 3, md: 4 } } }}>
            <Typography variant="h5" component="h2" fontWeight={750}>Contact information</Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mt: 1, lineHeight: 1.7 }}>
              For general inquiries, you can reach Shigoto using the contact details below.
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
      </Container>
    </Box>
  );
}
