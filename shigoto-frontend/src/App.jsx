import { useEffect } from 'react';
import { BrowserRouter, Navigate, Route, Routes, useLocation } from 'react-router-dom';
import { Box, CircularProgress } from '@mui/material';
import AuthProvider from './auth/AuthProvider';
import { useAuth } from './auth/authContext';
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
import CandidateProfile from './pages/CandidateProfile';
import HrDashboard from './pages/HrDashboard';
import JobManagement from './pages/JobManagement';
import CandidateDetails from './pages/CandidateDetails';
import InterviewerDashboard from './pages/InterviewerDashboard';
import InterviewerCandidateReview from './pages/InterviewerCandidateReview';

const publicPaths = ['/', '/jobs', '/about', '/contact', '/login', '/register'];

const roleHomes = {
  CANDIDATE: '/candidate',
  HR: '/hr',
  INTERVIEWER: '/interviewer',
};

function RequireRole({ role, children }) {
  const { user, loading, loggingOut } = useAuth();

  if (loading || loggingOut) {
    return <Box sx={{ minHeight: '60vh', display: 'grid', placeItems: 'center' }}><CircularProgress size={32} /></Box>;
  }
  if (!user) return <Navigate to="/login" replace />;
  if (user.role !== role) return <Navigate to={roleHomes[user.role] || '/'} replace />;
  return children;
}

/**
 * Defines the application's public and role-protected frontend routes.
 *
 * @returns {JSX.Element} The configured route hierarchy.
 */
function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<Home />} />
      <Route path="/jobs" element={<Navigate to="/#jobs" replace />} />
      <Route path="/about" element={<AboutUs />} />
      <Route path="/contact" element={<ContactUs />} />
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route path="/candidate" element={<RequireRole role="CANDIDATE"><CandidateDashboard /></RequireRole>} />
      <Route path="/candidate/profile" element={<RequireRole role="CANDIDATE"><CandidateProfile /></RequireRole>} />
      <Route path="/candidate/applications/new" element={<RequireRole role="CANDIDATE"><ApplicationForm /></RequireRole>} />
      <Route path="/candidate/applications/:applicationId" element={<RequireRole role="CANDIDATE"><CandidateApplicationDetails /></RequireRole>} />
      <Route path="/hr" element={<RequireRole role="HR"><HrDashboard /></RequireRole>} />
      <Route path="/hr/jobs" element={<RequireRole role="HR"><JobManagement /></RequireRole>} />
      <Route path="/hr/applications/:applicationId" element={<RequireRole role="HR"><CandidateDetails /></RequireRole>} />
      <Route path="/interviewer" element={<RequireRole role="INTERVIEWER"><InterviewerDashboard /></RequireRole>} />
      <Route path="/interviewer/applications/:applicationId" element={<RequireRole role="INTERVIEWER"><InterviewerCandidateReview /></RequireRole>} />
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
      <AuthProvider>
        <NavigationScrollManager />
        <AppLayout />
      </AuthProvider>
    </BrowserRouter>
  );
}

export default App;
