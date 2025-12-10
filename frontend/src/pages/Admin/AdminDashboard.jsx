import React from 'react';
import { 
    Box, 
    Grid, 
    Card, 
    CardContent, 
    Typography, 
    Button, 
    Paper,
    List,
    ListItem,
    ListItemAvatar,
    Avatar,
    ListItemText,
    Chip,
    Divider,
    Fade,
    Zoom
} from '@mui/material';
import { 
    People as PeopleIcon,
    EventBusy as EventBusyIcon,
    PendingActions as PendingActionsIcon,
    Add as AddIcon,
    Assessment as AssessmentIcon,
    TrendingUp as TrendingUpIcon
} from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import { formatDate } from '../../utils/timeHelpers';

// Mock Data
const kpiData = {
    totalEmployees: 156,
    onLeaveToday: 12,
    pendingRequests: 8,
    departments: [
        { name: 'Yazılım', usage: 45, total: 60 },
        { name: 'İnsan Kaynakları', usage: 30, total: 40 },
        { name: 'Satış', usage: 25, total: 35 },
        { name: 'Muhasebe', usage: 15, total: 21 }
    ]
};

const recentActivities = [
    { id: 1, user: 'Ahmet Yılmaz', action: 'Yeni kullanıcı eklendi', time: '2 saat önce', type: 'user' },
    { id: 2, user: 'Ayşe Demir', action: 'İzin türü güncellendi', time: '5 saat önce', type: 'config' },
    { id: 3, user: 'Mehmet Öz', action: 'Departman yöneticisi atandı', time: '1 gün önce', type: 'workflow' },
];

const KPICard = ({ title, value, icon, gradient, onClick }) => (
    <Zoom in={true} timeout={600}>
        <Card 
            onClick={onClick}
            sx={{ 
                height: '100%', 
                background: gradient || 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                color: 'white',
                borderRadius: 3,
                boxShadow: '0 8px 24px rgba(102, 126, 234, 0.3)',
                transition: 'all 0.3s ease',
                position: 'relative',
                overflow: 'hidden',
                cursor: onClick ? 'pointer' : 'default',
                '&::before': {
                    content: '""',
                    position: 'absolute',
                    top: 0,
                    right: 0,
                    width: '100px',
                    height: '100px',
                    background: 'radial-gradient(circle, rgba(255,255,255,0.2) 0%, transparent 70%)',
                    borderRadius: '50%',
                    transform: 'translate(30%, -30%)'
                },
                '&:hover': {
                    transform: 'translateY(-4px)',
                    boxShadow: '0 12px 32px rgba(102, 126, 234, 0.4)'
                }
            }}
        >
            <CardContent sx={{ display: 'flex', alignItems: 'center', p: 3, position: 'relative', zIndex: 1 }}>
                <Box
                    sx={{
                        width: 64,
                        height: 64,
                        borderRadius: 2,
                        background: 'rgba(255, 255, 255, 0.2)',
                        backdropFilter: 'blur(10px)',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        mr: 2.5,
                        boxShadow: '0 4px 12px rgba(0,0,0,0.1)'
                    }}
                >
                    {React.cloneElement(icon, { sx: { fontSize: 32, color: 'white' } })}
                </Box>
                <Box sx={{ flex: 1 }}>
                    <Typography variant="body2" sx={{ opacity: 0.9, mb: 0.5, fontWeight: 500 }}>
                        {title}
                    </Typography>
                    <Typography variant="h4" fontWeight="bold" sx={{ lineHeight: 1.2 }}>
                        {value}
                    </Typography>
                </Box>
            </CardContent>
        </Card>
    </Zoom>
);

const AdminDashboard = () => {
    const navigate = useNavigate();

    return (
        <Box>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 4 }}>
                <Typography variant="h4" fontWeight="bold" sx={{ 
                    background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                    WebkitBackgroundClip: 'text',
                    WebkitTextFillColor: 'transparent'
                }}>
                    Admin Paneli - Genel Bakış
                </Typography>
                <Box sx={{ display: 'flex', gap: 2 }}>
                    <Button 
                        variant="contained" 
                        startIcon={<AddIcon />}
                        onClick={() => navigate('/admin/users')}
                        sx={{
                            background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                            '&:hover': {
                                background: 'linear-gradient(135deg, #5568d3 0%, #6a3f8f 100%)'
                            }
                        }}
                    >
                        Yeni Personel Ekle
                    </Button>
                    <Button 
                        variant="outlined" 
                        startIcon={<AssessmentIcon />}
                        onClick={() => navigate('/reports')}
                    >
                        Rapor Al
                    </Button>
                </Box>
            </Box>

            <Grid container spacing={3}>
                {/* KPI Cards */}
                <Grid item xs={12} md={4}>
                    <KPICard 
                        title="Toplam Personel Sayısı" 
                        value={kpiData.totalEmployees} 
                        icon={<PeopleIcon />} 
                        gradient="linear-gradient(135deg, #667eea 0%, #764ba2 100%)"
                        onClick={() => navigate('/admin/users')}
                    />
                </Grid>
                <Grid item xs={12} md={4}>
                    <KPICard 
                        title="Bugün İzinli Personel" 
                        value={kpiData.onLeaveToday} 
                        icon={<EventBusyIcon />} 
                        gradient="linear-gradient(135deg, #f093fb 0%, #f5576c 100%)"
                    />
                </Grid>
                <Grid item xs={12} md={4}>
                    <KPICard 
                        title="Onay Bekleyen Talepler" 
                        value={kpiData.pendingRequests} 
                        icon={<PendingActionsIcon />} 
                        gradient="linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)"
                        onClick={() => navigate('/approvals')}
                    />
                </Grid>

                {/* Department Usage Chart */}
                <Grid item xs={12} md={8}>
                    <Fade in={true} timeout={800}>
                        <Paper 
                            sx={{ 
                                p: 3, 
                                borderRadius: 3,
                                boxShadow: '0 4px 20px rgba(0,0,0,0.08)',
                                border: '1px solid',
                                borderColor: 'divider',
                                borderOpacity: 0.5,
                                height: '100%'
                            }}
                        >
                            <Box sx={{ display: 'flex', alignItems: 'center', mb: 3 }}>
                                <TrendingUpIcon sx={{ color: 'primary.main', mr: 1, fontSize: 28 }} />
                                <Typography variant="h6" fontWeight="bold">
                                    Departmanlara Göre İzin Kullanım Dağılımı
                                </Typography>
                            </Box>
                            <Divider sx={{ mb: 3, opacity: 0.3 }} />
                            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                                {kpiData.departments.map((dept, index) => {
                                    const percentage = Math.round((dept.usage / dept.total) * 100);
                                    return (
                                        <Box key={index}>
                                            <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                                                <Typography variant="body2" fontWeight={600}>
                                                    {dept.name}
                                                </Typography>
                                                <Typography variant="body2" color="text.secondary">
                                                    {dept.usage} / {dept.total} (%{percentage})
                                                </Typography>
                                            </Box>
                                            <Box
                                                sx={{
                                                    width: '100%',
                                                    height: 8,
                                                    bgcolor: 'action.hover',
                                                    borderRadius: 4,
                                                    overflow: 'hidden'
                                                }}
                                            >
                                                <Box
                                                    sx={{
                                                        width: `${percentage}%`,
                                                        height: '100%',
                                                        background: 'linear-gradient(90deg, #667eea 0%, #764ba2 100%)',
                                                        borderRadius: 4,
                                                        transition: 'width 0.5s ease'
                                                    }}
                                                />
                                            </Box>
                                        </Box>
                                    );
                                })}
                            </Box>
                        </Paper>
                    </Fade>
                </Grid>

                {/* Recent Activities */}
                <Grid item xs={12} md={4}>
                    <Fade in={true} timeout={1000}>
                        <Paper 
                            sx={{ 
                                p: 3, 
                                height: '100%',
                                borderRadius: 3,
                                boxShadow: '0 4px 20px rgba(0,0,0,0.08)',
                                border: '1px solid',
                                borderColor: 'divider',
                                borderOpacity: 0.5
                            }}
                        >
                            <Typography variant="h6" fontWeight="bold" sx={{ mb: 2 }}>
                                Son Aktiviteler
                            </Typography>
                            <Divider sx={{ mb: 2, opacity: 0.3 }} />
                            <List sx={{ p: 0 }}>
                                {recentActivities.map((activity) => (
                                    <ListItem 
                                        key={activity.id}
                                        sx={{
                                            mb: 2,
                                            p: 2,
                                            borderRadius: 2,
                                            bgcolor: 'action.hover',
                                            transition: 'all 0.2s',
                                            '&:hover': {
                                                bgcolor: 'action.selected',
                                                transform: 'translateX(4px)'
                                            }
                                        }}
                                    >
                                        <ListItemAvatar>
                                            <Avatar 
                                                sx={{
                                                    bgcolor: activity.type === 'user' ? 'primary.main' : 
                                                             activity.type === 'config' ? 'warning.main' : 'success.main',
                                                    width: 40,
                                                    height: 40
                                                }}
                                            >
                                                {activity.user.charAt(0)}
                                            </Avatar>
                                        </ListItemAvatar>
                                        <ListItemText
                                            primary={
                                                <Typography variant="body2" fontWeight={600}>
                                                    {activity.action}
                                                </Typography>
                                            }
                                            secondary={
                                                <Box>
                                                    <Typography variant="caption" color="text.secondary">
                                                        {activity.user} • {activity.time}
                                                    </Typography>
                                                </Box>
                                            }
                                        />
                                    </ListItem>
                                ))}
                            </List>
                        </Paper>
                    </Fade>
                </Grid>
            </Grid>
        </Box>
    );
};

export default AdminDashboard;

