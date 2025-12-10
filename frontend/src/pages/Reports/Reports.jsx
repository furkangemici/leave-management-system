import React, { useState, useEffect } from 'react';
import { 
    Box, 
    Typography, 
    Paper, 
    Grid, 
    TextField, 
    MenuItem, 
    Button, 
    Card, 
    CardContent,
    FormControl,
    InputLabel,
    Select,
    Chip
} from '@mui/material';
import { DataGrid } from '@mui/x-data-grid';
import { trTR } from '@mui/x-data-grid/locales';
import { 
    Search as SearchIcon, 
    FileDownload as FileDownloadIcon 
} from '@mui/icons-material';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { AdapterDateFns } from '@mui/x-date-pickers/AdapterDateFns';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import { formatDate } from '../../utils/timeHelpers';
import useAuth from '../../hooks/useAuth';
import { subDays, startOfMonth, endOfMonth } from 'date-fns';

// Mock Results Data
const mockReportData = [
    { id: 1, name: 'Ahmet Yılmaz', department: 'IT', type: 'Yıllık İzin', startDate: '2025-06-10', endDate: '2025-06-20', days: 10, status: 'APPROVED' },
    { id: 2, name: 'Ayşe Demir', department: 'HR', type: 'Mazeret İzni', startDate: '2025-05-05', endDate: '2025-05-05', days: 1, status: 'APPROVED' },
    { id: 3, name: 'Mehmet Kaya', department: 'Sales', type: 'Hastalık İzni', startDate: '2025-07-01', endDate: '2025-07-03', days: 3, status: 'APPROVED' },
    { id: 4, name: 'Zeynep Çelik', department: 'IT', type: 'Yıllık İzin', startDate: '2025-08-15', endDate: '2025-08-25', days: 10, status: 'PENDING' },
    { id: 5, name: 'Ali Veli', department: 'Sales', type: 'Ücretsiz İzin', startDate: '2025-05-15', endDate: '2025-05-20', days: 5, status: 'APPROVED' }, // New Mock Data
];

const Reports = () => {
    const { auth } = useAuth();
    const [startDate, setStartDate] = useState(null);
    const [endDate, setEndDate] = useState(null);
    const [department, setDepartment] = useState('ALL');
    const [leaveType, setLeaveType] = useState('ALL');
    const [results, setResults] = useState(mockReportData);

    // Auto-filter for Accountant Role
    useEffect(() => {
        if (auth?.user?.roles?.includes('ACCOUNTANT')) {
            // Default to Previous Month
            const now = new Date();
            const lastMonth = subDays(now, 30); // Approximate
            const startOfLast = startOfMonth(lastMonth);
            const endOfLast = endOfMonth(lastMonth);

            setStartDate(startOfLast);
            setEndDate(endOfLast);
            
            // Auto-select 'Ücretsiz İzin' or show all Approved
            // Requirement says: "Varsayılan olarak Maaştan Düşenler seçili gelmelidir"
            setLeaveType('Ücretsiz İzin'); 
            
            // Filter initially
            const filtered = mockReportData.filter(r => 
                r.status === 'APPROVED' && 
                r.type === 'Ücretsiz İzin'
            );
            setResults(filtered);
        }
    }, [auth]);

    const handleSearch = () => {
        // Mock filtering logic for demo
        console.log("Searching with:", { startDate, endDate, department, leaveType });
        
        let filtered = mockReportData;

        if (leaveType !== 'ALL') {
            filtered = filtered.filter(item => item.type === leaveType);
        }
        
        if (department !== 'ALL') {
             filtered = filtered.filter(item => item.department === department);
        }

        // Accountant should mostly see Approved, but let's strictly enforce if needed
        // For now, simple filter
        setResults(filtered);
    };

    const handleDownloadExcel = async () => {
        try {
            // Simulated API Delay
            await new Promise(resolve => setTimeout(resolve, 1000));
            // In a real scenario, this would trigger a backend download
            // For now, simple alert to show it works
             alert('Excel raporu hazırlanıyor... (Demo: Backend bağlantısı gerekli)');

        } catch (error) {
            console.error('Excel indirme hatası:', error);
            alert('Rapor indirilirken bir hata oluştu.');
        }
    };

    const columns = [
        { field: 'name', headerName: 'Ad Soyad', width: 200 },
        { field: 'department', headerName: 'Departman', width: 150 },
        { field: 'type', headerName: 'İzin Türü', width: 150 },
        { 
            field: 'dateRange', 
            headerName: 'Tarih Aralığı', 
            width: 200, 
            valueGetter: (value, row) => {
                 if (!row) return '';
                 return `${formatDate(row.startDate)} - ${formatDate(row.endDate)}`;
            }
        },
        { field: 'days', headerName: 'Gün Sayısı', width: 100 },
        { 
            field: 'status', 
            headerName: 'Durum', 
            width: 150,
            renderCell: (params) => {
                const statusLabels = {
                    'APPROVED': 'Onaylandı',
                    'PENDING': 'Bekliyor',
                    'REJECTED': 'Reddedildi'
                };
                return (
                    <Chip 
                        label={statusLabels[params.value] || params.value} 
                        color={params.value === 'APPROVED' ? 'success' : params.value === 'REJECTED' ? 'error' : 'warning'} 
                        size="small" 
                        variant="outlined" 
                    />
                );
            }
        }
    ];

    return (
        <LocalizationProvider dateAdapter={AdapterDateFns}>
            <Box sx={{ width: '100%' }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
                    <Typography variant="h4" sx={{ fontWeight: 'bold' }}>
                        Bordro Rapor Merkezi
                    </Typography>
                    <Button 
                        variant="contained" 
                        color="success" 
                        startIcon={<FileDownloadIcon />}
                        onClick={handleDownloadExcel}
                    >
                        Excel'e Aktar
                    </Button>
                </Box>

                {/* Filters */}
                <Card sx={{ mb: 3 }}>
                    <CardContent>
                        <Grid container spacing={2} alignItems="center">
                            <Grid item xs={12} md={3}>
                                <DatePicker
                                    label="Başlangıç Tarihi"
                                    value={startDate}
                                    onChange={(newValue) => setStartDate(newValue)}
                                    slotProps={{ textField: { fullWidth: true, size: 'small' } }}
                                />
                            </Grid>
                            <Grid item xs={12} md={3}>
                                <DatePicker
                                    label="Bitiş Tarihi"
                                    value={endDate}
                                    onChange={(newValue) => setEndDate(newValue)}
                                    slotProps={{ textField: { fullWidth: true, size: 'small' } }}
                                />
                            </Grid>
                            <Grid item xs={12} md={2}>
                                <FormControl fullWidth size="small">
                                    <InputLabel>Departman</InputLabel>
                                    <Select
                                        value={department}
                                        label="Departman"
                                        onChange={(e) => setDepartment(e.target.value)}
                                    >
                                        <MenuItem value="ALL">Tüm Departmanlar</MenuItem>
                                        <MenuItem value="IT">IT</MenuItem>
                                        <MenuItem value="HR">İK</MenuItem>
                                        <MenuItem value="Sales">Satış</MenuItem>
                                    </Select>
                                </FormControl>
                            </Grid>
                            <Grid item xs={12} md={2}>
                                <FormControl fullWidth size="small">
                                    <InputLabel>İzin Türü</InputLabel>
                                    <Select
                                        value={leaveType}
                                        label="İzin Türü"
                                        onChange={(e) => setLeaveType(e.target.value)}
                                    >
                                        <MenuItem value="ALL">Tümü</MenuItem>
                                        <MenuItem value="Yıllık İzin">Yıllık İzin</MenuItem>
                                        <MenuItem value="Mazeret İzni">Mazeret İzni</MenuItem>
                                        <MenuItem value="Hastalık İzni">Hastalık İzni</MenuItem>
                                        <MenuItem value="Ücretsiz İzin">Ücretsiz İzin (Maaş Kesintisi)</MenuItem>
                                    </Select>
                                </FormControl>
                            </Grid>
                            <Grid item xs={12} md={2}>
                                <Button 
                                    fullWidth 
                                    variant="contained" 
                                    startIcon={<SearchIcon />}
                                    onClick={handleSearch}
                                >
                                    Raporu Getir
                                </Button>
                            </Grid>
                        </Grid>
                    </CardContent>
                </Card>

                {/* Summary Strip */}
                <Card sx={{ mb: 3, bgcolor: '#f0f9ff', border: '1px solid #bae6fd' }}>
                    <CardContent sx={{ py: 2, '&:last-child': { pb: 2 } }}>
                        <Grid container spacing={4} justifyContent="center" textAlign="center">
                             <Grid item xs={4}>
                                <Typography variant="caption" color="text.secondary" fontWeight="bold">TOPLAM RAPOR KAYDI</Typography>
                                <Typography variant="h6" color="primary.main" fontWeight="bold">{results.length}</Typography>
                            </Grid>
                            <Grid item xs={4}>
                                <Typography variant="caption" color="text.secondary" fontWeight="bold">TOPLAM İZİN GÜNÜ</Typography>
                                <Typography variant="h6" color="primary.main" fontWeight="bold">{results.reduce((acc, curr) => acc + curr.days, 0)} Gün</Typography>
                            </Grid>
                            <Grid item xs={4}>
                                <Typography variant="caption" color="text.secondary" fontWeight="bold">ONAYLANAN</Typography>
                                <Typography variant="h6" color="success.main" fontWeight="bold">{results.filter(r => r.status === 'APPROVED').length}</Typography>
                            </Grid>
                        </Grid>
                    </CardContent>
                </Card>

                {/* Results Table */}
                <Paper sx={{ height: 'calc(100vh - 350px)', width: '100%', p: 3, boxShadow: 3, borderRadius: 2 }}>
                    <DataGrid
                        rows={results}
                        columns={columns}
                        pageSize={10}
                        rowsPerPageOptions={[10, 20, 50]}
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
            </Box>
        </LocalizationProvider>
    );
};

export default Reports;
