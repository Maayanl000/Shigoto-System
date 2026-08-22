import { useEffect } from 'react';
import { BrowserRouter, Navigate, Route, Routes, useLocation } from 'react-router-dom';
import { Box } from '@mui/material';
import PublicHeader from './components/PublicHeader';
import PublicFooter from './components/PublicFooter';
import DashboardShell from './components/DashboardShell';
import Home from './pages/Home';
import AboutUs from './pages/AboutUs';
import ContactUs from './pages/ContactUs';
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

const publicPaths = ['/', '/jobs', '/about', '/contact', '/login', '/register'];

function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<Home />} />
      <Route path="/jobs" element={<Navigate to="/#jobs" replace />} />
      <Route path="/about" element={<AboutUs />} />
      <Route path="/contact" element={<ContactUs />} />
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
  );
}

function NavigationScrollManager() {
  const { pathname, hash } = useLocation();

  useEffect(() => {
    const frame = window.requestAnimationFrame(() => {
      if (hash === '#jobs') {
        document.getElementById('jobs')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
        return;
      }

      window.scrollTo({ top: 0, left: 0, behavior: 'auto' });
    });

    return () => window.cancelAnimationFrame(frame);
  }, [pathname, hash]);

  return null;
}

function AppLayout() {
  const { pathname } = useLocation();
  const isPublicPage = publicPaths.includes(pathname);

  if (!isPublicPage) {
    return (
      <DashboardShell>
        <AppRoutes />
      </DashboardShell>
    );
  }

  return (
    <Box sx={{ minHeight: '100vh', display: 'flex', flexDirection: 'column', bgcolor: 'background.default' }}>
      <PublicHeader />
      <Box component="main" sx={{ flex: 1 }}>
        <AppRoutes />
      </Box>
      <PublicFooter />
    </Box>
  );
}

function App() {
  return (
    <BrowserRouter>
      <NavigationScrollManager />
      <AppLayout />
    </BrowserRouter>
  );
}

export default App;
