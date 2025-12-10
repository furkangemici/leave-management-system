import React from 'react';
import { Box, Grid, Card, CardContent, Typography, Paper, List, ListItem, ListItemAvatar, Avatar, ListItemText, Divider, Chip } from '@mui/material';
import { 
    EventAvailable, 
    PendingActions, 
    BeachAccess, 
    ArrowForward 
} from '@mui/icons-material';
import useAuth from '../../hooks/useAuth';
import { formatDate } from '../../utils/timeHelpers';

const mockTeamOnLeave = [
    { id: 1, name: 'Ahmet Yılmaz', type: 'Yıllık İzin', endDate: '2025-08-25', avatar: '' },
    { id: 2, name: 'Ayşe Demir', type: 'Raporlu', endDate: '2025-08-20', avatar: '' },
];

const KPICard = ({ title, value, icon, color }) => (
    <Card sx={{ height: '100%', position: 'relative', overflow: 'hidden' }}>
        <Box sx={{ 
            position: 'absolute', 
            top: -10, 
            right: -10, 
            opacity: 0.1, 
            transform: 'rotate(15deg) scale(1.5)',
            color: color 
        }}>
            {icon}
        </Box>
        <CardContent>
            <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
                <Box sx={{ p: 1, borderRadius: 2, bgcolor: `${color}20`, color: color, mr: 2 }}>
                    {icon}
                </Box>
                <Typography variant="h6" color="text.secondary" sx={{ fontSize: '0.9rem', fontWeight: 600 }}>
                    {title}
                </Typography>
            </Box>
            <Typography variant="h4" fontWeight="bold">
                {value}
            </Typography>
        </CardContent>
    </Card>
);

const ManagerDashboard = () => {
    const { auth } = useAuth();

    return (
        <Box sx={{ width: '100%' }}>
            <Box sx={{ mb: 4 }}>
                <Typography variant="h4" fontWeight="bold" gutterBottom>
                    Hoş Geldiniz, {auth?.user?.name || 'Yönetici'} 👋
                </Typography>
                <Typography variant="body1" color="text.secondary">
                    Bugün ekip durumunu kontrol edebilir ve bekleyen talepleri yönetebilirsiniz.
                </Typography>
            </Box>

            <Grid container spacing={3} sx={{ mb: 4 }}>
                <Grid item xs={12} md={4}>
                    <KPICard 
                        title="Kalan Yıllık İzin" 
                        value="14 Gün" 
                        icon={<BeachAccess fontSize="large" />} 
                        color="#0d47a1" 
                    />
                </Grid>
                <Grid item xs={12} md={4}>
                    <KPICard 
                        title="Bekleyen Onaylar" 
                        value="3 Talep" 
                        icon={<PendingActions fontSize="large" />} 
                        color="#f59e0b" 
                    />
                </Grid>
                <Grid item xs={12} md={4}>
                    <KPICard 
                        title="Gelecek Resmi Tatil" 
                        value="29 Ekim" 
                        icon={<EventAvailable fontSize="large" />} 
                        color="#10b981" 
                    />
                </Grid>
            </Grid>

            <Grid container spacing={3}>
                <Grid item xs={12}>
                    <Paper sx={{ p: 0, height: '100%', overflow: 'hidden' }}>
                        <Box sx={{ p: 2, bgcolor: '#f1f5f9', borderBottom: '1px solid #e2e8f0' }}>
                            <Typography variant="h6" fontWeight="bold">
                                Kimler İzinli?
                            </Typography>
                        </Box>
                        <List sx={{ p: 0 }}>
                            {mockTeamOnLeave.map((person, index) => (
                                <React.Fragment key={person.id}>
                                    <ListItem alignItems="flex-start" sx={{ py: 2 }}>
                                        <ListItemAvatar>
                                            <Avatar sx={{ bgcolor: 'primary.light' }}>
                                                {person.name.charAt(0)}
                                            </Avatar>
                                        </ListItemAvatar>
                                        <ListItemText
                                            primary={
                                                <Typography variant="subtitle2" fontWeight="bold">
                                                    {person.name}
                                                </Typography>
                                            }
                                            secondary={
                                                <Box component="span" sx={{ display: 'flex', flexDirection: 'column', mt: 0.5 }}>
                                                    <Typography variant="caption" color="text.secondary">
                                                        {person.type} • {formatDate(person.endDate)}'e kadar
                                                    </Typography>
                                                    <Chip 
                                                        label="İzinli" 
                                                        size="small" 
                                                        color={person.type === 'Raporlu' ? 'error' : 'warning'} 
                                                        variant="outlined" 
                                                        sx={{ mt: 0.5, width: 'fit-content', height: 20, fontSize: '0.65rem' }} 
                                                    />
                                                </Box>
                                            }
                                        />
                                    </ListItem>
                                    {index < mockTeamOnLeave.length - 1 && <Divider component="li" />}
                                </React.Fragment>
                            ))}
                            {mockTeamOnLeave.length === 0 && (
                                <Box sx={{ p: 3, textAlign: 'center' }}>
                                    <Typography variant="body2" color="text.secondary">
                                        Şu an izinli kimse yok.
                                    </Typography>
                                </Box>
                            )}
                        </List>
                         <Box sx={{ p: 2, borderTop: '1px solid #e2e8f0', textAlign: 'center' }}>
                            <Typography variant="button" color="primary" sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '0.8rem', cursor: 'pointer' }}>
                                Tüm Takvimi Gör <ArrowForward sx={{ fontSize: 16, ml: 0.5 }} />
                            </Typography>
                        </Box>
                    </Paper>
                </Grid>
            </Grid>
        </Box>
    );
};

export default ManagerDashboard;
