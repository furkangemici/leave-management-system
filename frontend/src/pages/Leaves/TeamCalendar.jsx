import React from 'react';
import FullCalendar from '@fullcalendar/react';
import dayGridPlugin from '@fullcalendar/daygrid';
import timeGridPlugin from '@fullcalendar/timegrid';
import interactionPlugin from '@fullcalendar/interaction';
import { Box, Paper, Typography } from '@mui/material';

const TeamCalendar = () => {
    
    // Helper to get current date + days
    const getDate = (daysToAdd) => {
        const date = new Date();
        date.setDate(date.getDate() + daysToAdd);
        return date.toISOString().split('T')[0];
    };

    const mockEvents = [
        {
            id: 1,
            title: 'Ahmet Yılmaz - Yıllık İzin',
            start: getDate(2),
            end: getDate(5),
            color: '#4caf50', // Green
            textColor: 'white'
        },
        {
            id: 2,
            title: 'Ayşe Demir - Mazeret (Bekliyor)',
            start: getDate(4),
            end: getDate(4),
            color: '#ff9800', // Orange
            textColor: 'white'
        },
        {
            id: 3,
            title: 'Mehmet Kaya - Hasta',
            start: getDate(-1),
            end: getDate(1),
            color: '#4caf50',
            textColor: 'white'
        },
        {
            id: 4,
            title: 'Resmi Tatil - Yılbaşı',
            start: '2026-01-01',
            display: 'background',
            color: '#f44336' // Red
        },
        {
            id: 5,
            title: 'Demo Tatil',
            start: getDate(10),
            color: '#f44336',
            display: 'background'
        }
    ];

    return (
        <Box sx={{ width: '100%', height: 'calc(100vh - 100px)' }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
                <Typography variant="h4" sx={{ fontWeight: 'bold' }}>
                    Ekip Takvimi
                </Typography>
                <Box sx={{ display: 'flex', gap: 2 }}>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                        <Box sx={{ width: 12, height: 12, borderRadius: '50%', bgcolor: '#4caf50' }} />
                        <Typography variant="body2">Onaylı</Typography>
                    </Box>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                        <Box sx={{ width: 12, height: 12, borderRadius: '50%', bgcolor: '#ff9800' }} />
                        <Typography variant="body2">Bekliyor</Typography>
                    </Box>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                        <Box sx={{ width: 12, height: 12, borderRadius: '50%', bgcolor: '#f44336' }} />
                        <Typography variant="body2">Tatil</Typography>
                    </Box>
                </Box>
            </Box>
            <Paper elevation={3} sx={{ p: 2, height: '100%' }}>
                <FullCalendar
                    plugins={[dayGridPlugin, timeGridPlugin, interactionPlugin]}
                    initialView="dayGridMonth"
                    headerToolbar={{
                        left: 'prev,next today',
                        center: 'title',
                        right: 'dayGridMonth,timeGridWeek,timeGridDay'
                    }}
                    events={mockEvents}
                    height="85vh" // Adapts to screen
                    locale="tr"
                    buttonText={{
                        today: 'Bugün',
                        month: 'Ay',
                        week: 'Hafta',
                        day: 'Gün'
                    }}
                    eventContent={renderEventContent}
                />
            </Paper>
        </Box>
    );
};

// Custom event render (optional)
function renderEventContent(eventInfo) {
    return (
        <div style={{ padding: '2px', overflow: 'hidden', textOverflow: 'ellipsis' }}>
            <i>{eventInfo.event.title}</i>
        </div>
    );
}

export default TeamCalendar;
