import { createTheme } from '@mui/material/styles';

const theme = createTheme({
  palette: {
    mode: 'light',
    primary: {
      main: '#183153',
      dark: '#10233d',
      light: '#e8eef5',
      contrastText: '#ffffff',
    },
    secondary: {
      main: '#087f8c',
      dark: '#05616b',
      light: '#e2f3f4',
      contrastText: '#ffffff',
    },
    background: {
      default: '#EAF0F6',
      paper: '#F8FAFC',
    },
    text: {
      primary: '#172033',
      secondary: '#667085',
    },
    divider: '#CBD5E1',
  },
  shape: {
    borderRadius: 10,
  },
  typography: {
    fontFamily: 'Inter, "Segoe UI", Roboto, Helvetica, Arial, sans-serif',
    h1: {
      fontWeight: 800,
      letterSpacing: '-0.04em',
    },
    h2: {
      fontWeight: 800,
      letterSpacing: '-0.035em',
    },
    h3: {
      fontWeight: 800,
      letterSpacing: '-0.03em',
    },
    h4: {
      fontSize: '1.875rem',
      lineHeight: 1.25,
      fontWeight: 700,
      letterSpacing: '-0.025em',
    },
    h6: {
      fontSize: '1rem',
      lineHeight: 1.4,
      fontWeight: 700,
    },
    button: {
      fontWeight: 700,
      textTransform: 'none',
    },
  },
  components: {
    MuiAppBar: {
      styleOverrides: {
        root: {
          boxShadow: 'none',
        },
      },
    },
    MuiButton: {
      defaultProps: {
        disableElevation: true,
      },
      styleOverrides: {
        root: {
          borderRadius: 8,
          minHeight: 38,
          paddingInline: 16,
          transition: 'transform 140ms ease, background-color 150ms ease, border-color 150ms ease, color 150ms ease',
          '&:not(.Mui-disabled):hover': {
            transform: 'translateY(-1px)',
          },
          '&:not(.Mui-disabled):active': {
            transform: 'translateY(0) scale(0.99)',
          },
          '&:focus-visible': {
            outline: '3px solid rgba(8, 127, 140, 0.24)',
            outlineOffset: 2,
          },
        },
      },
    },
    MuiCard: {
      defaultProps: {
        variant: 'outlined',
      },
      styleOverrides: {
        root: {
          borderColor: '#CBD5E1',
          boxShadow: '0 1px 2px rgba(16, 35, 61, 0.04)',
          transition: 'border-color 160ms ease, box-shadow 160ms ease',
        },
      },
    },
    MuiCardActionArea: {
      styleOverrides: {
        root: {
          '&:focus-visible': {
            outline: '3px solid rgba(8, 127, 140, 0.24)',
            outlineOffset: -3,
          },
        },
      },
    },
    MuiCardContent: {
      styleOverrides: {
        root: {
          padding: 24,
          '&:last-child': {
            paddingBottom: 24,
          },
        },
      },
    },
    MuiChip: {
      styleOverrides: {
        root: {
          borderRadius: 6,
          fontWeight: 600,
          transition: 'background-color 150ms ease, border-color 150ms ease, color 150ms ease',
        },
      },
    },
    MuiTextField: {
      defaultProps: {
        size: 'small',
      },
    },
    MuiDialog: {
      styleOverrides: {
        paper: {
          border: '1px solid #CBD5E1',
          boxShadow: '0 24px 70px rgba(16, 35, 61, 0.22)',
        },
      },
    },
  },
});

export default theme;
