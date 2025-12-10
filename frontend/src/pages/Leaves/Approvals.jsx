import React, { useState } from 'react';
import { 
    Box, 
    Typography, 
    Paper, 
    Chip, 
    Button, 
    Dialog,
    DialogTitle,
    DialogContent,
    DialogActions,
    TextField,
    Tooltip,
    IconButton,
    Alert,
    Avatar
} from '@mui/material';
import { DataGrid } from '@mui/x-data-grid';
import { trTR } from '@mui/x-data-grid/locales';
import { 
    CheckCircle as CheckCircleIcon, 
    Cancel as CancelIcon, 
    Warning as WarningIcon, 
    History as HistoryIcon, 
    Close as CloseIcon 
} from '@mui/icons-material';
import { formatDate } from '../../utils/timeHelpers';
import AuditLogs from '../../components/Common/AuditLogs';

// Mock Data
const mockRequests = [
    { id: 1, employeeName: 'Ahmet Yılmaz', avatar: '', department: 'IT', type: 'Yıllık İzin', startDate: '2025-06-10', endDate: '2025-06-20', duration: 10, status: 'PENDING' },
    { id: 2, employeeName: 'Ayşe Demir', avatar: '', department: 'HR', type: 'Mazeret İzni', startDate: '2025-05-05', endDate: '2025-05-05', duration: 1, status: 'APPROVED' },
    { id: 3, employeeName: 'Mehmet Kaya', avatar: '', department: 'Sales', type: 'Hastalık İzni', startDate: '2025-07-01', endDate: '2025-07-03', duration: 3, status: 'REJECTED', rejectionReason: 'Yoğun dönem.' },
    { id: 4, employeeName: 'Zeynep Çelik', avatar: '', department: 'IT', type: 'Yıllık İzin', startDate: '2025-08-15', endDate: '2025-08-25', duration: 10, status: 'PENDING' },
];

const Approvals = () => {
    const [rows, setRows] = useState(mockRequests);
    const [selectedTab, setSelectedTab] = useState(0); // 0: Pending, 1: Approved, 2: Rejected
    
    // States for Rejection Dialog
    const [openRejectDialog, setOpenRejectDialog] = useState(false);
    const [selectedRequestId, setSelectedRequestId] = useState(null);
    const [rejectionReason, setRejectionReason] = useState('');
    
    // History Dialog State
    const [openHistoryDialog, setOpenHistoryDialog] = useState(false);
    const [selectedHistoryItem, setSelectedHistoryItem] = useState(null);

    const handleApprove = (id) => {
        // Mock API call
        setRows(prev => prev.map(row => row.id === id ? { ...row, status: 'APPROVED' } : row));
    };

    const handleOpenRejectDialog = (id) => {
        setSelectedRequestId(id);
        setOpenRejectDialog(true);
    };

    const handleCloseRejectDialog = () => {
        setOpenRejectDialog(false);
        setSelectedRequestId(null);
        setRejectionReason('');
    };

    const handleRejectConfirm = () => {
        if (selectedRequestId) {
             setRows(prev => prev.map(row => row.id === selectedRequestId ? { ...row, status: 'REJECTED', rejectionReason } : row));
             handleCloseRejectDialog();
        }
    };

    // History Handlers
    const handleOpenHistory = (row) => {
        setSelectedHistoryItem(row);
        setOpenHistoryDialog(true);
    };

    const handleCloseHistory = () => {
        setOpenHistoryDialog(false);
        setSelectedHistoryItem(null);
    };

    const columns = [
        { 
            field: 'employeeName', 
            headerName: 'Çalışan', 
            width: 200,
            renderCell: (params) => (
                <Box sx={{ display: 'flex', alignItems: 'center' }}>
                    <Avatar sx={{ width: 24, height: 24, mr: 1, bgcolor: 'primary.light', fontSize: '0.8rem' }}>
                        {params.value.charAt(0)}
                    </Avatar>
                    {params.value}
                </Box>
            )
        },
        { field: 'department', headerName: 'Departman', width: 120 },
        { field: 'type', headerName: 'İzin Türü', width: 150 },
        { field: 'startDate', headerName: 'Başlangıç', width: 120, valueFormatter: (params) => formatDate(params.value) },
        { field: 'endDate', headerName: 'Bitiş', width: 120, valueFormatter: (params) => formatDate(params.value) },
        { field: 'duration', headerName: 'Süre', width: 100, valueGetter: (value) => `${value} Gün` },
        { 
            field: 'status', 
            headerName: 'Durum', 
            width: 140,
            renderCell: (params) => (
                <Chip 
                    label={params.value === 'PENDING' ? 'Bekliyor' : params.value === 'APPROVED' ? 'Onaylandı' : 'Reddedildi'} 
                    color={params.value === 'PENDING' ? 'warning' : params.value === 'APPROVED' ? 'success' : 'error'}
                    size="small"
                    variant="outlined"
                />
            )
        },
        {
            field: 'actions',
            headerName: 'İşlemler',
            width: 180,
            renderCell: (params) => (
                <Box>
                     {params.row.status === 'PENDING' && (
                        <>
                            <Tooltip title="Onayla">
                                <IconButton color="success" size="small" onClick={() => handleApprove(params.id)}>
                                    <CheckCircleIcon />
                                </IconButton>
                            </Tooltip>
                            <Tooltip title="Reddet">
                                <IconButton color="error" size="small" onClick={() => handleOpenRejectDialog(params.id)}>
                                    <CancelIcon />
                                </IconButton>
                            </Tooltip>
                        </>
                    )}
                    <Tooltip title="Geçmişi Gör">
                        <IconButton color="info" size="small" onClick={() => handleOpenHistory(params.row)}>
                            <HistoryIcon />
                        </IconButton>
                    </Tooltip>
                </Box>
            )
        }
    ];

    const filteredRows = rows.filter(row => {
        if (selectedTab === 0) return row.status === 'PENDING';
        if (selectedTab === 1) return row.status === 'APPROVED';
        if (selectedTab === 2) return row.status === 'REJECTED';
        return true;
    });

    return (
        <Box sx={{ width: '100%' }}>
            <Typography variant="h4" sx={{ mb: 3, fontWeight: 'bold' }}>Onay Merkezi</Typography>
            
            <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 2 }}>
                 <Button onClick={() => setSelectedTab(0)} sx={{ borderBottom: selectedTab === 0 ? '2px solid' : 'none', borderRadius: 0, pb: 1, mr: 2, color: selectedTab === 0 ? 'primary.main' : 'text.secondary' }}>Bekleyenler</Button>
                 <Button onClick={() => setSelectedTab(1)} sx={{ borderBottom: selectedTab === 1 ? '2px solid' : 'none', borderRadius: 0, pb: 1, mr: 2, color: selectedTab === 1 ? 'success.main' : 'text.secondary' }}>Onaylananlar</Button>
                 <Button onClick={() => setSelectedTab(2)} sx={{ borderBottom: selectedTab === 2 ? '2px solid' : 'none', borderRadius: 0, pb: 1, color: selectedTab === 2 ? 'error.main' : 'text.secondary' }}>Reddedilenler</Button>
            </Box>

            {/* Conflict Warning - Mock implementation for demo */}
             {mockRequests.some(r => r.status === 'PENDING' && r.employeeName === 'Ahmet Yılmaz') && (
                 <Alert severity="warning" icon={<WarningIcon />} sx={{ mb: 2 }}>
                     Dikkat: "Ahmet Yılmaz" isimli personelin talep ettiği tarihlerde ekip arkadaşı "Ali Veli" de izinlidir. (Çakışma Kontrolü)
                 </Alert>
             )}

            <Paper sx={{ height: 500, width: '100%', p: 2, boxShadow: 3, borderRadius: 2 }}>
                <DataGrid
                    rows={filteredRows}
                    columns={columns}
                    pageSize={5}
                    rowsPerPageOptions={[5]}
                    disableSelectionOnClick
                    localeText={trTR.components.MuiDataGrid.defaultProps.localeText}
                    sx={{ border: 'none' }}
                />
            </Paper>

             {/* Rejection Dialog */}
             <Dialog open={openRejectDialog} onClose={handleCloseRejectDialog}>
                <DialogTitle>Talebi Reddet</DialogTitle>
                <DialogContent sx={{ pt: 1, minWidth: 400 }}>
                    <TextField
                        autoFocus
                        margin="dense"
                        label="Reddetme Nedeni"
                        fullWidth
                        multiline
                        rows={3}
                        value={rejectionReason}
                        onChange={(e) => setRejectionReason(e.target.value)}
                    />
                </DialogContent>
                <DialogActions>
                    <Button onClick={handleCloseRejectDialog}>İptal</Button>
                    <Button onClick={handleRejectConfirm} color="error" variant="contained">Reddet</Button>
                </DialogActions>
            </Dialog>

            {/* History/Audit Log Dialog */}
            <Dialog open={openHistoryDialog} onClose={handleCloseHistory} maxWidth="sm" fullWidth>
                <DialogTitle sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                     İşlem Geçmişi - {selectedHistoryItem?.employeeName}
                     <IconButton onClick={handleCloseHistory} size="small"><CloseIcon /></IconButton>
                </DialogTitle>
                <DialogContent dividers>
                    <AuditLogs />
                </DialogContent>
            </Dialog>
        </Box>
    );
};

export default Approvals;
