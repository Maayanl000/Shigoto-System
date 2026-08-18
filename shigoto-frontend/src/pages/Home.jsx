import { useState, useEffect } from 'react';
import { Typography, Grid, Card, CardContent, CardActions, Button, Chip, Box, CircularProgress } from '@mui/material';
import WorkIcon from '@mui/icons-material/Work';
import LocationOnIcon from '@mui/icons-material/LocationOn';
import api from '../services/api';

export default function Home() {
  const [jobs, setJobs] = useState([]);
  const [loading, setLoading] = useState(true);

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
          { id: 1, title: 'Full Stack Java Developer', department: 'R&D', location: 'Tel Aviv', type: 'Full-time' },
          { id: 2, title: 'Backend Engineer (Spring Boot)', department: 'Engineering', location: 'Hybrid', type: 'Full-time' },
          { id: 3, title: 'Frontend Developer (React)', department: 'UI/UX', location: 'Remote', type: 'Full-time' },
        ]);
        setLoading(false);
      });
  }, []);

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', mt: 8 }}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box sx={{ mt: 4 }}>
      <Typography variant="h4" fontWeight="bold" gutterBottom color="primary">
        Open Positions at Shigoto
      </Typography>
      <Typography variant="subtitle1" color="text.secondary" paragraph>
        Explore top tech opportunities and apply directly.
      </Typography>

      <Grid container spacing={3} sx={{ mt: 1 }}>
        {jobs.map((job) => (
          <Grid item xs={12} md={6} key={job.id}>
            <Card sx={{ height: '100%', display: 'flex', flexDirection: 'column', justifyContent: 'space-between', boxShadow: 3 }}>
              <CardContent>
                <Typography variant="h6" fontWeight="bold" gutterBottom>
                  {job.title}
                </Typography>
                
                <Box sx={{ display: 'flex', gap: 1, mb: 2, alignItems: 'center' }}>
                  <Chip label={job.department || 'Tech'} color="secondary" size="small" />
                  <Chip icon={<LocationOnIcon />} label={job.location || 'Israel'} variant="outlined" size="small" />
                </Box>

                <Typography variant="body2" color="text.secondary">
                  We are looking for a talented professional to join our fast-growing team.
                </Typography>
              </CardContent>

              <CardActions sx={{ p: 2, pt: 0 }}>
                <Button variant="contained" color="primary" fullWidth startIcon={<WorkIcon />}>
                  Apply Now
                </Button>
              </CardActions>
            </Card>
          </Grid>
        ))}
      </Grid>
    </Box>
  );
}