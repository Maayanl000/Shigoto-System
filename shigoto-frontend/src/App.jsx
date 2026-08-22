import { BrowserRouter, Routes, Route, Link } from 'react-router-dom';
import { AppBar, Toolbar, Button, Container, Typography, Box, Paper } from '@mui/material';
import Home from './pages/Home';
import Login from './pages/Login';
import Register from './pages/Register';
import CandidateDashboard from './pages/CandidateDashboard';
import ApplicationForm from './pages/ApplicationForm';
import CandidateApplicationDetails from './pages/CandidateApplicationDetails';
import HrDashboard from './pages/HrDashboard';
import JobManagement from './pages/JobManagement';
import CandidateDetails from './pages/CandidateDetails';
import InterviewerDashboard from './pages/InterviewerDashboard';
import InterviewDetails from './pages/InterviewDetails';
import InterviewFeedback from './pages/InterviewFeedback';

const developmentLinks = [
  { label: 'Jobs', to: '/' },
  { label: 'Login', to: '/login' },
  { label: 'Register', to: '/register' },
  { label: 'Candidate Dashboard', to: '/candidate' },
  { label: 'Apply', to: '/candidate/applications/new' },
  { label: 'Application Details', to: '/candidate/applications/demo' },
  { label: 'HR Kanban', to: '/hr' },
  { label: 'Job Management', to: '/hr/jobs' },
  { label: 'Candidate Details', to: '/hr/candidates/demo' },
  { label: 'My Interviews', to: '/interviewer' },
  { label: 'Interview Details', to: '/interviewer/interviews/demo' },
  { label: 'Feedback', to: '/interviewer/interviews/demo/feedback' },
];

function App() {
  return (
    <BrowserRouter>
      <AppBar position="static">
        <Toolbar>
          <Typography variant="h6" sx={{ flexGrow: 1, fontWeight: 'bold', textAlign: 'left' }}>
            Shigoto ATS
          </Typography>
          <Button color="inherit" component={Link} to="/">Open Jobs</Button>
          <Button color="inherit" component={Link} to="/login">Login</Button>
        </Toolbar>
      </AppBar>

      <Paper component="nav" square variant="outlined" aria-label="Development navigation">
        <Box sx={{ display: 'flex', flexWrap: 'wrap', justifyContent: 'center', gap: 0.5, p: 1 }}>
          {developmentLinks.map((link) => (
            <Button key={link.to} size="small" component={Link} to={link.to}>
              {link.label}
            </Button>
          ))}
        </Box>
      </Paper>

      <Container component="main" maxWidth="lg" sx={{ pb: 6 }}>
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/candidate" element={<CandidateDashboard />} />
          <Route path="/candidate/applications/new" element={<ApplicationForm />} />
          <Route path="/candidate/applications/:applicationId" element={<CandidateApplicationDetails />} />
          <Route path="/hr" element={<HrDashboard />} />
          <Route path="/hr/jobs" element={<JobManagement />} />
          <Route path="/hr/candidates/:candidateId" element={<CandidateDetails />} />
          <Route path="/interviewer" element={<InterviewerDashboard />} />
          <Route path="/interviewer/interviews/:interviewId" element={<InterviewDetails />} />
          <Route path="/interviewer/interviews/:interviewId/feedback" element={<InterviewFeedback />} />
        </Routes>
      </Container>
    </BrowserRouter>
  );
}

export default App;
