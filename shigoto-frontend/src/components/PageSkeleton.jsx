import { Box, Card, CardContent, Chip, Grid, Stack, Typography } from '@mui/material';

export default function PageSkeleton({ title, description, sections, children }) {
  return (
    <Box sx={{ mt: { xs: 3, md: 5 }, textAlign: 'left' }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 2, mb: { xs: 3, md: 4 } }}>
        <Box>
          <Typography variant="overline" color="secondary.dark" sx={{ fontWeight: 800, letterSpacing: '0.08em' }}>
            Shigoto workspace
          </Typography>
          <Typography variant="h4" component="h1" color="text.primary" gutterBottom>
            {title}
          </Typography>
          {description && (
            <Typography color="text.secondary" sx={{ maxWidth: 720 }}>
              {description}
            </Typography>
          )}
        </Box>
        <Chip label="UI preview" size="small" variant="outlined" sx={{ mt: 0.5, color: 'text.secondary', bgcolor: 'background.paper' }} />
      </Box>

      {children || (
        <Grid container spacing={2.5}>
          {sections.map((section) => (
            <Grid key={section.title} size={{ xs: 12, md: section.wide ? 12 : 6 }}>
              <Card sx={{ height: '100%', minHeight: 148 }}>
                <CardContent>
                  <Stack direction="row" justifyContent="space-between" alignItems="center" spacing={1} sx={{ mb: 1.5 }}>
                    <Typography variant="h6" component="h2">{section.title}</Typography>
                    {section.status && <Chip label={section.status} size="small" color={section.statusColor || 'default'} variant="outlined" />}
                  </Stack>
                  {typeof section.content === 'string' ? (
                    <Typography variant="body2" color="text.secondary">
                      {section.content}
                    </Typography>
                  ) : section.content}
                </CardContent>
              </Card>
            </Grid>
          ))}
        </Grid>
      )}
    </Box>
  );
}
