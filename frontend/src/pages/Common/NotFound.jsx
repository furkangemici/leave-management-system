import React from 'react';
import { Box, Typography, Button, Container, Paper } from '@mui/material';
import { Home as HomeIcon, SearchOff as SearchOffIcon } from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';

const NotFound = () => {
    const navigate = useNavigate();

    return (
        <Box
            sx={{
                display: 'flex',
                justifyContent: 'center',
                alignItems: 'center',
                minHeight: '100vh',
                background: 'linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%)'
            }}
        >
            <Container maxWidth="md">
                <Paper
                    elevation={24}
                    sx={{
                        p: 5,
                        display: 'flex',
                        flexDirection: 'column',
                        alignItems: 'center',
                        textAlign: 'center',
                        borderRadius: 4,
                        bgcolor: 'rgba(255, 255, 255, 0.9)',
                        backdropFilter: 'blur(10px)'
                    }}
                >
                    <SearchOffIcon sx={{ fontSize: 100, color: 'text.secondary', mb: 2, opacity: 0.5 }} />
                    
                    <Typography variant="h1" sx={{ fontWeight: 900, color: 'primary.main', mb: 1 }}>
                        404
                    </Typography>
                    
                    <Typography variant="h4" sx={{ fontWeight: 'bold', color: 'text.primary', mb: 2 }}>
                        Aradığınız Sayfa Bulunamadı
                    </Typography>
                    
                    <Typography variant="body1" sx={{ color: 'text.secondary', mb: 4, maxWidth: 500 }}>
                        Gitmek istediğiniz sayfa silinmiş, taşınmış veya hiç var olmamış olabilir. 
                        Ama endişelenmeyin, sizi güvenli bir yere götürebiliriz.
                    </Typography>

                    <Button
                        variant="contained"
                        size="large"
                        startIcon={<HomeIcon />}
                        onClick={() => navigate('/dashboard')}
                        sx={{
                            borderRadius: 2,
                            px: 4,
                            py: 1.5,
                            fontSize: '1.1rem',
                            fontWeight: 'bold',
                            boxShadow: '0 8px 16px rgba(0,0,0,0.2)'
                        }}
                    >
                        Ana Sayfaya Dön
                    </Button>
                </Paper>
            </Container>
        </Box>
    );
};

export default NotFound;
