import { createTheme } from '@mui/material/styles';
// import { trTR } from '@mui/x-data-grid/locales';

const theme = createTheme({
  palette: {
    mode: 'light',
    primary: {
      main: '#0d47a1', // Enterprise Dark Blue
      light: '#5472d3',
      dark: '#002171',
      contrastText: '#ffffff',
    },
    secondary: {
      main: '#475569', // Slate
      light: '#94a3b8',
      dark: '#1e293b',
      contrastText: '#ffffff',
    },
    background: {
      default: '#f8fafc', // Very light gray/blue background
      paper: '#ffffff',
    },
    text: {
      primary: '#0f172a',
      secondary: '#64748b',
    },
    success: {
        main: '#10b981',
    },
    warning: {
        main: '#f59e0b',
    },
    error: {
        main: '#ef4444',
    }
  },
  typography: {
    fontFamily: '"Inter", "Roboto", "Helvetica", "Arial", sans-serif',
    h1: { fontWeight: 700 },
    h2: { fontWeight: 700 },
    h3: { fontWeight: 600 },
    h4: { fontWeight: 600, letterSpacing: '-0.5px' },
    h5: { fontWeight: 600 },
    h6: { fontWeight: 600 },
    button: { textTransform: 'none', fontWeight: 500 },
  },
  shape: {
    borderRadius: 12, // More rounded corners
  },
  components: {
    MuiButton: {
      styleOverrides: {
        root: {
          borderRadius: '8px',
          boxShadow: 'none',
          padding: '10px 20px',
          '&:hover': {
            boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1), 0 2px 4px -2px rgb(0 0 0 / 0.1)',
          },
        },
        containedPrimary: {
            background: 'linear-gradient(to right, #0d47a1, #1976d2)',
        }
      },
    },
    MuiCard: {
      styleOverrides: {
        root: {
          borderRadius: '16px',
          boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1), 0 2px 4px -2px rgb(0 0 0 / 0.1)',
          border: '1px solid #e2e8f0',
        },
      },
    },
    MuiPaper: {
        styleOverrides: {
            root: {
                backgroundImage: 'none',
            },
            elevation1: {
                boxShadow: '0 1px 3px 0 rgb(0 0 0 / 0.1), 0 1px 2px -1px rgb(0 0 0 / 0.1)',
            }
        }
    },
    MuiDataGrid: {
        styleOverrides: {
            root: {
                border: 'none',
                backgroundColor: '#ffffff',
                borderRadius: '16px',
                '& .MuiDataGrid-cell': {
                    borderBottom: '1px solid #f1f5f9',
                },
                '& .MuiDataGrid-columnHeaders': {
                    borderBottom: '2px solid #e2e8f0',
                    backgroundColor: '#f8fafc',
                    color: '#475569',
                    fontSize: '0.875rem',
                    textTransform: 'uppercase',
                    letterSpacing: '0.05em',
                },
                '& .MuiDataGrid-row': {
                    '&:hover': {
                        backgroundColor: '#f8fafc',
                    }
                }
            },
        }
    }
  },
}); 
// trTR); // Appying Turkish Locale for DataGrid - Disabled for stability

export default theme;
