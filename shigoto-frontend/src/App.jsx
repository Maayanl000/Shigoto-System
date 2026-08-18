import { BrowserRouter, Routes, Route, Link } from 'react-router-dom';
import { AppBar, Toolbar, Button, Container, Typography } from '@mui/material';
import Home from './pages/Home';
import HrDashboard from './pages/HrDashboard';
import CandidateDashboard from './pages/CandidateDashboard';
import Login from './pages/Login';

function App() {
  return (
    <BrowserRouter>
      {/* תפריט הניווט העליון שלנו (Navbar) */}
      <AppBar position="static">
        <Toolbar>
          <Typography variant="h6" sx={{ flexGrow: 1, fontWeight: 'bold' }}>
            Shigoto ATS
          </Typography>
          <Button color="inherit" component={Link} to="/">חיפוש משרות</Button>
          <Button color="inherit" component={Link} to="/candidate">אזור מועמד</Button>
          <Button color="inherit" component={Link} to="/hr">אזור HR</Button>
        </Toolbar>
      </AppBar>

      {/* האזור שבו התוכן של כל עמוד יוצג בהתאם לכתובת */}
      <Container maxWidth="lg">
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/candidate" element={<CandidateDashboard />} />
          <Route path="/hr" element={<HrDashboard />} />
          <Route path="/login" element={<Login />} />
        </Routes>
      </Container>
    </BrowserRouter>
  );
}

export default App;