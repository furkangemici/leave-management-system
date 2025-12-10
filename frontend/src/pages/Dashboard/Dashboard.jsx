import React, { useState } from 'react';
import { 
    Grid, 
    Paper, 
    Typography, 
    Box, 
    Card, 
    CardContent, 
    List, 
    ListItem, 
    ListItemAvatar, 
    Avatar, 
    ListItemText,
    Chip,
    TableContainer,
    Table,
    TableHead,
    TableRow,
    TableCell,
    TableBody,
    Divider,
    Fade,
    Zoom,
    Badge,
    Button,
    Alert
} from '@mui/material';
import { 
    EventAvailable as EventAvailableIcon, 
    PendingActions as PendingActionsIcon, 
    BeachAccess as BeachAccessIcon,
    Circle as CircleIcon,
    AddBox as AddBoxIcon
} from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import useAuth from '../../hooks/useAuth';
import { DateCalendar } from '@mui/x-date-pickers/DateCalendar';
import { PickersDay } from '@mui/x-date-pickers/PickersDay';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { AdapterDateFns } from '@mui/x-date-pickers/AdapterDateFns';
import { tr } from 'date-fns/locale';
import { 
    format, 
    isSameDay, 
    isWithinInterval, 
    parseISO, 
    addDays, 
    startOfDay 
} from 'date-fns';
import { formatLeaveDuration, formatDate } from '../../utils/timeHelpers';

// Mock Data Utilities
const TODAY = startOfDay(new Date());
const getMockDate = (daysAdd) => addDays(TODAY, daysAdd);

// Re-generate Mock Data for Demo
const kpiData = {
    leaveBalanceHours: 116,
    pendingRequests: 1,
    nextHoliday: { name: 'Yılbaşı', date: '2026-01-01' }
};

const teamOnLeave = [
    { 
        id: 1, 
        name: 'Ahmet Yılmaz', 
        department: 'IT', 
        startDate: getMockDate(0), // Today
        endDate: getMockDate(2), 
        avatar: '',
        type: 'Yıllık İzin',
        status: 'APPROVED'
    },
    { 
        id: 2, 
        name: 'Ayşe Demir', 
        department: 'HR', 
        startDate: getMockDate(-1), 
        endDate: getMockDate(0), // Today (Last day)
        avatar: '',
        type: 'Mazeret İzni',
        status: 'APPROVED'
    },
    { 
        id: 3, 
        name: 'Mehmet Öz', 
        department: 'Satış', 
        startDate: getMockDate(3), // Future
        endDate: getMockDate(5), 
        avatar: '',
        type: 'Yıllık İzin',
        status: 'APPROVED'
    },
];

const recentRequests = [
    { id: 101, type: 'Yıllık İzin', startDate: '2025-11-20', endDate: '2025-11-25', status: 'APPROVED', days: 5 },
    { id: 102, type: 'Mazeret İzni', startDate: '2025-12-01', endDate: '2025-12-01', status: 'PENDING', days: 1 },
    { id: 103, type: 'Hastalık İzni', startDate: '2025-10-15', endDate: '2025-10-16', status: 'REJECTED', days: 2 },
];

const getStatusChip = (status) => {
    switch (status) {
        case 'APPROVED': return <Chip label="Onaylandı" color="success" size="small" />;
        case 'PENDING': return <Chip label="Bekliyor" color="warning" size="small" />;
        case 'REJECTED': return <Chip label="Reddedildi" color="error" size="small" />;
        default: return <Chip label={status} size="small" />;
    }
};

const KPICard = ({ title, value, icon, color, gradient }) => (
    <Zoom in={true} timeout={600}>
        <Card 
            sx={{ 
                height: '100%', 
                display: 'flex', 
                flexDirection: 'column', 
                justifyContent: 'center',
                background: gradient || 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                color: 'white',
                borderRadius: 3,
                boxShadow: '0 8px 24px rgba(102, 126, 234, 0.3)',
                transition: 'all 0.3s ease',
                position: 'relative',
                overflow: 'hidden',
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

// Custom Day Renderer for Calendar
const ServerDay = (props) => {
    const { highlightedDays = [], day, outsideCurrentMonth, ...other } = props;

    // Check if this day matches any leave interval
    const status = highlightedDays.find(d => isSameDay(d.date, day));

    return (
        <Badge
            key={props.day.toString()}
            overlap="circular"
            badgeContent={status ? <Box sx={{ width: 8, height: 8, borderRadius: '50%', bgcolor: status.color, boxShadow: '0 0 0 2px white' }} /> : undefined}
            anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
        >
            <PickersDay {...other} outsideCurrentMonth={outsideCurrentMonth} day={day} />
        </Badge>
    );
};

const Dashboard = () => {
    const { auth } = useAuth();
    const navigate = useNavigate();
    const [selectedDate, setSelectedDate] = useState(TODAY);

    // Filter logic for calendar dots
    const highlightedDays = [];
    const daysInMonth = 31; // Simplified look-ahead
    
    // Generate map of days to status colors
    for (let i = -10; i < 40; i++) {
        const d = addDays(TODAY, i);
        const activeLeaves = teamOnLeave.filter(l => 
            isWithinInterval(d, { start: startOfDay(l.startDate), end: startOfDay(l.endDate) })
        );

        if (activeLeaves.length > 0) {
            // Priority: Active (Green) > Future (Orange)
            // But 'activeLeaves' implies they cover this date.
            // If date < TODAY (past), maybe grey? For now:
            // If date is TODAY or past but within interval: Green (Currently on leave)
            // IF date > TODAY: Orange (Future planned leave)
            
            // Simplification: 
            // If the leave START date is in future, it's future leave (Orange)
            // If we are IN the interval and today is >= start, it's Active (Green)
            
            const isFuture = d > TODAY && activeLeaves.some(l => startOfDay(l.startDate) > TODAY);
            
            highlightedDays.push({
                date: d,
                color: isFuture ? '#ff9800' : '#4caf50'
            });
        }
    }

    // Filter list for Selected Date
    const selectedDateLeaves = teamOnLeave.filter(person => 
        isWithinInterval(selectedDate, { 
            start: startOfDay(person.startDate), 
            end: startOfDay(person.endDate) 
        })
    );

    return (
        <Box>
            <Box sx={{ mb: 4, display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end' }}>
                <Box>
                    <Typography variant="h4" fontWeight="bold" sx={{ 
                        background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                        WebkitBackgroundClip: 'text',
                        WebkitTextFillColor: 'transparent',
                        mb: 1
                    }}>
                        Hoş Geldiniz! 👋
                    </Typography>
                    <Typography variant="body1" color="text.secondary">
                        İzin durumunuzu ve takviminizi buradan takip edebilirsiniz.
                    </Typography>
                </Box>
                <Button 
                    variant="contained" 
                    size="large"
                    startIcon={<AddBoxIcon />}
                    onClick={() => navigate('/new-request')}
                    sx={{
                        borderRadius: 2,
                        px: 3,
                        background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                        boxShadow: '0 8px 16px rgba(102, 126, 234, 0.3)',
                        textTransform: 'none',
                        fontWeight: 600
                    }}
                >
                    Yeni İzin Talep Et
                </Button>
            </Box>

            {/* Accountant Shortcut */}
            {auth?.user?.roles?.includes('ACCOUNTANT') && (
                <Fade in={true}>
                    <Alert 
                        severity="info" 
                        variant="filled"
                        icon={<PendingActionsIcon />}
                        sx={{ 
                            mb: 3, 
                            borderRadius: 2, 
                            boxShadow: '0 4px 12px rgba(2, 136, 209, 0.2)',
                            alignItems: 'center',
                            background: 'linear-gradient(45deg, #0288d1 30%, #26c6da 90%)'
                        }}
                        action={
                            <Button 
                                color="inherit" 
                                variant="outlined"
                                size="small" 
                                onClick={() => navigate('/reports')}
                                sx={{ 
                                    bgcolor: 'rgba(255,255,255,0.2)', 
                                    border: '1px solid rgba(255,255,255,0.5)',
                                    '&:hover': { bgcolor: 'rgba(255,255,255,0.3)' }
                                }}
                            >
                                Bordro Raporlarına Git
                            </Button>
                        }
                    >
                        <Typography variant="subtitle2" fontWeight="bold">
                            Muhasebe Modülü Aktif
                        </Typography>
                        <Typography variant="caption" sx={{ display: 'block', opacity: 0.9 }}>
                            Bordro hesaplamaları için onaylanmış izin raporlarına buradan ulaşabilirsiniz.
                        </Typography>
                    </Alert>
                </Fade>
            )}

            {/* Quick Status Alert */}
            {kpiData.pendingRequests > 0 && (
                <Fade in={true}>
                    <Alert 
                        severity="warning" 
                        variant="filled"
                        sx={{ 
                            mb: 4, 
                            borderRadius: 2, 
                            boxShadow: '0 4px 12px rgba(237, 108, 2, 0.2)',
                            alignItems: 'center'
                        }}
                        action={
                            <Button color="inherit" size="small" onClick={() => navigate('/my-leaves')}>
                                Detayları Gör
                            </Button>
                        }
                    >
                        <Typography variant="subtitle2" fontWeight="bold">
                            Dikkat: Onay bekleyen {kpiData.pendingRequests} adet izin talebiniz bulunmaktadır.
                        </Typography>
                    </Alert>
                </Fade>
            )}
            
            <Grid container spacing={3}>
                {/* KPI Cards Section */}
                <Grid item xs={12} md={4}>
                    <KPICard 
                        title="Kalan İzin Hakkım" 
                        value={formatLeaveDuration(kpiData.leaveBalanceHours)} 
                        icon={<EventAvailableIcon />} 
                        gradient="linear-gradient(135deg, #667eea 0%, #764ba2 100%)"
                    />
                </Grid>
                <Grid item xs={12} md={4}>
                    <KPICard 
                        title="Onay Bekleyenler" 
                        value={`${kpiData.pendingRequests} Talep`} 
                        icon={<PendingActionsIcon />} 
                        gradient="linear-gradient(135deg, #f093fb 0%, #f5576c 100%)"
                    />
                </Grid>
                <Grid item xs={12} md={4}>
                    <KPICard 
                        title="Sonraki Tatil" 
                        value={`${formatDate(kpiData.nextHoliday.date)} - ${kpiData.nextHoliday.name}`} 
                        icon={<BeachAccessIcon />} 
                        gradient="linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)"
                    />
                </Grid>

            {/* Main Content Area */}
            
            {/* Left: Recent Activity */}
            <Grid item xs={12} md={8}>
                <Fade in={true} timeout={800}>
                    <Paper 
                        sx={{ 
                            p: 3, 
                            borderRadius: 3,
                            boxShadow: '0 4px 20px rgba(0,0,0,0.08)',
                            border: '1px solid',
                            borderColor: 'divider',
                            borderOpacity: 0.5
                        }}
                    >
                        <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
                            <Box
                                sx={{
                                    width: 4,
                                    height: 24,
                                    borderRadius: 1,
                                    background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                                    mr: 2
                                }}
                            />
                            <Typography variant="h6" fontWeight="bold">
                                Son Hareketlerim
                            </Typography>
                        </Box>
                        <Divider sx={{ mb: 3, opacity: 0.3 }} />
                        <TableContainer>
                            <Table size="small">
                                <TableHead>
                                    <TableRow>
                                        <TableCell sx={{ fontWeight: 600, color: 'text.secondary' }}>İzin Türü</TableCell>
                                        <TableCell sx={{ fontWeight: 600, color: 'text.secondary' }}>Tarih Aralığı</TableCell>
                                        <TableCell sx={{ fontWeight: 600, color: 'text.secondary' }}>Süre</TableCell>
                                        <TableCell sx={{ fontWeight: 600, color: 'text.secondary' }}>Durum</TableCell>
                                    </TableRow>
                                </TableHead>
                                <TableBody>
                                    {recentRequests.map((req, index) => (
                                        <TableRow 
                                            key={req.id}
                                            sx={{
                                                '&:hover': {
                                                    bgcolor: 'action.hover',
                                                    transition: 'background-color 0.2s'
                                                }
                                            }}
                                        >
                                            <TableCell sx={{ fontWeight: 500 }}>{req.type}</TableCell>
                                            <TableCell>{formatDate(req.startDate)} - {formatDate(req.endDate)}</TableCell>
                                            <TableCell sx={{ fontWeight: 500 }}>{req.days} Gün</TableCell>
                                            <TableCell>{getStatusChip(req.status)}</TableCell>
                                        </TableRow>
                                    ))}
                                </TableBody>
                            </Table>
                        </TableContainer>
                    </Paper>
                </Fade>
            </Grid>

            {/* Right: Team Calendar Widget */}
            <Grid item xs={12} md={4}>
                <Fade in={true} timeout={1000}>
                    <Paper 
                        sx={{ 
                            p: 0, 
                            height: '100%',
                            minHeight: 450,
                            borderRadius: 3,
                            boxShadow: '0 4px 20px rgba(0,0,0,0.08)',
                            border: '1px solid',
                            borderColor: 'divider',
                            borderOpacity: 0.5,
                            overflow: 'hidden'
                        }}
                    >
                        {/* Widget Header */}
                        <Box sx={{ p: 3, pb: 0 }}>
                            <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
                                <Box
                                    sx={{
                                        width: 4,
                                        height: 24,
                                        borderRadius: 1,
                                        background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                                        mr: 2
                                    }}
                                />
                                <Typography variant="h6" fontWeight="bold">
                                    Ekibimde Kimler İzinli?
                                </Typography>
                            </Box>
                            {/* Legend */}
                            <Box sx={{ display: 'flex', gap: 2, mb: 1 }}>
                                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                                    <Box sx={{ width: 8, height: 8, borderRadius: '50%', bgcolor: '#4caf50' }} />
                                    <Typography variant="caption" color="text.secondary">İzinde</Typography>
                                </Box>
                                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                                    <Box sx={{ width: 8, height: 8, borderRadius: '50%', bgcolor: '#ff9800' }} />
                                    <Typography variant="caption" color="text.secondary">Planlı</Typography>
                                </Box>
                            </Box>
                        </Box>

                        <Divider sx={{ opacity: 0.3 }} />

                        {/* Calendar */}
                        <LocalizationProvider dateAdapter={AdapterDateFns} adapterLocale={tr}>
                            <DateCalendar 
                                value={selectedDate}
                                onChange={(newValue) => setSelectedDate(newValue)}
                                slots={{
                                    day: ServerDay
                                }}
                                slotProps={{
                                    day: {
                                        highlightedDays
                                    }
                                }}
                                sx={{
                                    width: '100%',
                                    '& .MuiPickersDay-root': {
                                        fontSize: '0.9rem',
                                    },
                                    '& .MuiDayCalendar-header': {
                                        fontWeight: 'bold'
                                    }
                                }}
                            />
                        </LocalizationProvider>
                        
                        <Divider />

                        {/* Selected Date List */}
                        <Box sx={{ p: 2, bgcolor: '#f8fafc', height: '100%' }}>
                            <Typography variant="subtitle2" fontWeight="bold" sx={{ mb: 1.5, color: 'text.primary' }}>
                                {format(selectedDate, 'd MMMM yyyy', { locale: tr })}
                            </Typography>
                            
                            <List sx={{ width: '100%', p: 0 }}>
                                {selectedDateLeaves.length > 0 ? (
                                    selectedDateLeaves.map((person) => (
                                        <ListItem 
                                            key={person.id} 
                                            sx={{
                                                mb: 1,
                                                p: 1.5,
                                                borderRadius: 2,
                                                bgcolor: 'white',
                                                boxShadow: '0 1px 3px rgba(0,0,0,0.05)',
                                                border: '1px solid',
                                                borderColor: 'divider'
                                            }}
                                        >
                                            <ListItemAvatar sx={{ minWidth: 48 }}>
                                                <Avatar 
                                                    sx={{ 
                                                        width: 32, 
                                                        height: 32, 
                                                        fontSize: '0.9rem',
                                                        bgcolor: person.startDate > TODAY ? '#ff9800' : '#4caf50' 
                                                    }}
                                                >
                                                    {person.name.charAt(0)}
                                                </Avatar>
                                            </ListItemAvatar>
                                            <ListItemText
                                                primary={
                                                    <Typography variant="body2" fontWeight={600}>
                                                        {person.name}
                                                    </Typography>
                                                }
                                                secondary={
                                                    <Typography variant="caption" color="text.secondary">
                                                        {person.type}
                                                    </Typography>
                                                }
                                            />
                                            <Chip label="İzinli" size="small" color={person.startDate > TODAY ? "warning" : "success"} sx={{ height: 20, fontSize: '0.65rem' }} />
                                        </ListItem>
                                    ))
                                ) : (
                                    <Box sx={{ textAlign: 'center', py: 2, opacity: 0.6 }}>
                                        <CircleIcon sx={{ fontSize: 32, color: 'text.disabled', mb: 1 }} />
                                        <Typography variant="body2" color="text.secondary">
                                            Bu tarihte izinli kimse yok.
                                        </Typography>
                                    </Box>
                                )}
                            </List>
                        </Box>
                    </Paper>
                </Fade>
            </Grid>
        </Grid>
        </Box>
    );
};

export default Dashboard;
