import React, { useState } from 'react';
import { 
    Box, 
    Typography, 
    Paper, 
    Chip, 
    Button, 
    Tooltip, 
    IconButton,
    Dialog,
    DialogTitle,
    DialogContent,
    DialogActions
} from '@mui/material';
import { Close as CloseIcon, Visibility as VisibilityIcon } from '@mui/icons-material';
import { DataGrid } from '@mui/x-data-grid';
import { trTR } from '@mui/x-data-grid/locales';
import { formatDate } from '../../utils/timeHelpers';
import RequestTimeline from '../../components/Leaves/RequestTimeline';

// Mock Data for the logged-in user
const mockMyLeaves = [
    { id: 1, type: 'Yıllık İzin', startDate: '2025-08-15', endDate: '2025-08-25', days: 10, status: 'PENDING', createdAt: '2024-12-01' },
    { id: 2, type: 'Mazeret İzni', startDate: '2025-05-05', endDate: '2025-05-05', days: 1, status: 'APPROVED', createdAt: '2024-11-20' },
    { id: 3, type: 'Hastalık İzni', startDate: '2025-01-10', endDate: '2025-01-12', days: 2, status: 'APPROVED', createdAt: '2024-11-15' },
    { id: 4, type: 'Yıllık İzin', startDate: '2024-12-20', endDate: '2024-12-30', days: 10, status: 'REJECTED', createdAt: '2024-10-05', rejectionReason: 'Yıl sonu yoğunluğu nedeniyle uygun değil.' },
];

const MyLeaves = () => {
    const [openTimelineDialog, setOpenTimelineDialog] = useState(false);
    const [selectedRequest, setSelectedRequest] = useState(null);

    const handleOpenTimeline = (request) => {
        setSelectedRequest(request);
        setOpenTimelineDialog(true);
    };

    const handleCloseTimeline = () => {
        setOpenTimelineDialog(false);
        setSelectedRequest(null);
    };

    const columns = [
        { field: 'type', headerName: 'İzin Türü', flex: 1, minWidth: 150 },
        { 
            field: 'startDate', 
            headerName: 'Başlangıç', 
            width: 140,
            valueFormatter: (params) => formatDate(params.value)
        },
        { 
            field: 'endDate', 
            headerName: 'Bitiş', 
            width: 140,
            valueFormatter: (params) => formatDate(params.value)
        },
        { field: 'days', headerName: 'Süre (Gün)', width: 100 },
        { 
            field: 'createdAt', 
            headerName: 'Oluşturulma', 
            width: 140,
            valueFormatter: (params) => formatDate(params.value)
        },
        { 
            field: 'status', 
            headerName: 'Durum', 
            width: 180,
            renderCell: (params) => {
                let color = 'default';
                let label = params.value;
                if (params.value === 'APPROVED') { color = 'success'; label = 'Onaylandı'; }
                if (params.value === 'PENDING') { color = 'warning'; label = 'Bekliyor'; }
                if (params.value === 'REJECTED') { color = 'error'; label = 'Reddedildi'; }
                
                const chip = <Chip label={label} color={color} size="small" variant="outlined" />;

                if (params.value === 'REJECTED' && params.row.rejectionReason) {
                    return (
                        <Tooltip title={`Ret Sebebi: ${params.row.rejectionReason}`} arrow>
                            {chip}
                        </Tooltip>
                    );
                }
                return chip;
            }
        },
        {
            field: 'actions',
            headerName: 'İşlemler',
            width: 150,
            sortable: false,
            renderCell: (params) => (
                <Box sx={{ display: 'flex', gap: 1 }}>
                     <Tooltip title="Talep Detayları ve Süreç">
                        <IconButton 
                            size="small" 
                            color="primary" 
                            onClick={() => handleOpenTimeline(params.row)}
                        >
                            <VisibilityIcon />
                        </IconButton>
                    </Tooltip>
                    
                    {params.row.status === 'PENDING' && (
                        <Tooltip title="İptal Et">
                            <IconButton 
                                size="small"
                                color="error"
                                onClick={() => {
                                    if(window.confirm('Bu izin talebini iptal etmek istediğinize emin misiniz?')) {
                                        alert('Talep iptal edildi.');
                                    }
                                }}
                            >
                                <CloseIcon />
                            </IconButton>
                        </Tooltip>
                    )}
                </Box>
            )
        }
    ];

    return (
        <Box sx={{ width: '100%' }}>
            <Typography variant="h4" sx={{ mb: 3, fontWeight: 'bold', color: 'text.primary' }}>
                Geçmiş İzinlerim
            </Typography>
            <Paper sx={{ height: 'calc(100vh - 180px)', width: '100%', p: 3, boxShadow: 3, borderRadius: 2 }}>
                <DataGrid
                    rows={mockMyLeaves}
                    columns={columns}
                    pageSize={10}
                    rowsPerPageOptions={[10, 25, 50]}
                    disableSelectionOnClick
                    localeText={trTR.components.MuiDataGrid.defaultProps.localeText}
                    sx={{ 
                        border: 'none',
                        '& .MuiDataGrid-row': {
                            '&:hover': {
                                bgcolor: 'action.hover'
                            }
                        }
                    }}
                />
            </Paper>

            {/* Timeline Dialog */}
            <Dialog open={openTimelineDialog} onClose={handleCloseTimeline} maxWidth="sm" fullWidth>
                <DialogTitle>İzin Talep Süreci</DialogTitle>
                <DialogContent dividers>
                    {selectedRequest && <RequestTimeline request={selectedRequest} />}
                </DialogContent>
                <DialogActions>
                    <Button onClick={handleCloseTimeline}>Kapat</Button>
                </DialogActions>
            </Dialog>
        </Box>
    );
};

export default MyLeaves;
