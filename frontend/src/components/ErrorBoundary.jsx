import { Component } from 'react';
import { Box, Typography, Button, Container } from '@mui/material';

class ErrorBoundary extends Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }

  componentDidCatch(error, errorInfo) {
    console.error('ErrorBoundary caught an error:', error, errorInfo);
  }

  handleReset = () => {
    this.setState({ hasError: false, error: null });
    window.location.reload();
  };

  render() {
    if (this.state.hasError) {
      return (
        <Container maxWidth="sm" sx={{ mt: 8 }}>
          <Box
            sx={{
              textAlign: 'center',
              p: 4,
              borderRadius: 2,
              bgcolor: 'background.paper',
              boxShadow: 2,
            }}
          >
            <Typography variant="h4" gutterBottom color="error">
              Bir Hata Oluştu
            </Typography>
            <Typography variant="body1" color="text.secondary" sx={{ mb: 3 }}>
              Üzgünüz, beklenmeyen bir hata oluştu. Lütfen sayfayı yenileyin.
            </Typography>
            <Button
              variant="contained"
              color="primary"
              onClick={this.handleReset}
            >
              Sayfayı Yenile
            </Button>
          </Box>
        </Container>
      );
    }

    return this.props.children;
  }
}

export default ErrorBoundary;

