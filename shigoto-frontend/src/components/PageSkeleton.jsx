import { Box, Card, CardContent, Grid, Typography } from '@mui/material';

export default function PageSkeleton({ title, description, sections, children }) {
  return (
    <Box sx={{ mt: 4, textAlign: 'left' }}>
      <Typography variant="h4" component="h1" fontWeight="bold" color="primary" gutterBottom>
        {title}
      </Typography>
      {description && (
        <Typography color="text.secondary" sx={{ mb: 3 }}>
          {description}
        </Typography>
      )}

      {children || (
        <Grid container spacing={2}>
          {sections.map((section) => (
            <Grid key={section.title} size={{ xs: 12, md: section.wide ? 12 : 6 }}>
              <Card variant="outlined" sx={{ height: '100%' }}>
                <CardContent>
                  <Typography variant="h6" component="h2" gutterBottom>
                    {section.title}
                  </Typography>
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
