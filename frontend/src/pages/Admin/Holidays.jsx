import React, { useState } from 'react';
import { 
    Box, 
    Typography, 
    Paper, 
    Button, 
    IconButton, 
    Dialog,
    DialogTitle,
    DialogContent,
    DialogActions,
    TextField,
    Chip
} from '@mui/material';
import { DataGrid } from '@mui/x-data-grid';
import { trTR } from '@mui/x-data-grid/locales';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { AdapterDateFns } from '@mui/x-date-pickers/AdapterDateFns';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import { 
    Add as AddIcon, 
    Delete as DeleteIcon, 
    Edit as EditIcon,
    CalendarToday as CalendarIcon
} from '@mui/icons-material';
import { useFormik } from 'formik';
import * as Yup from 'yup';
import { formatDate } from '../../utils/timeHelpers';

const mockHolidays = [
    { id: 1, name: 'Yılbaşı', date: '2025-01-01', days: 1 },
    { id: 2, name: 'Ulusal Egemenlik ve Çocuk Bayramı', date: '2025-04-23', days: 1 },
    { id: 3, name: 'Emek ve Dayanışma Günü', date: '2025-05-01', days: 1 },
    { id: 4, name: 'Atatürk\'ü Anma, Gençlik ve Spor Bayramı', date: '2025-05-19', days: 1 },
    { id: 5, name: 'Kurban Bayramı', date: '2025-06-06', days: 4.5 },
    { id: 6, name: 'Demokrasi ve Milli Birlik Günü', date: '2025-07-15', days: 1 },
    { id: 7, name: 'Zafer Bayramı', date: '2025-08-30', days: 1 },
    { id: 8, name: 'Cumhuriyet Bayramı', date: '2025-10-29', days: 1.5 },
];

const Holidays = () => {
    const [holidays, setHolidays] = useState(mockHolidays);
    const [editDialogOpen, setEditDialogOpen] = useState(false);
    const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
    const [selectedHoliday, setSelectedHoliday] = useState(null);
    const [editMode, setEditMode] = useState(false);

    const validationSchema = Yup.object({
        name: Yup.string().required('Tatil adı zorunludur'),
        date: Yup.date().required('Tarih zorunludur').nullable(),
        days: Yup.number().min(0.5, 'En az 0.5 gün olmalıdır').required('Gün sayısı zorunludur')
    });

    const formik = useFormik({
        initialValues: {
            name: '',
            date: null,
            days: 1
        },
        validationSchema: validationSchema,
        onSubmit: (values) => {
            if (editMode && selectedHoliday) {
                setHolidays(holidays.map(h => 
                    h.id === selectedHoliday.id 
                        ? { ...h, ...values, date: values.date.toISOString().split('T')[0] } 
                        : h
                ));
            } else {
                const newId = Math.max(...holidays.map(h => h.id), 0) + 1;
                setHolidays([...holidays, {
                    id: newId,
                    ...values,
                    date: values.date.toISOString().split('T')[0]
                }]);
            }
            handleCloseEditDialog();
        },
    });

    const handleOpenEditDialog = (holiday = null) => {
        if (holiday) {
            setEditMode(true);
            setSelectedHoliday(holiday);
            formik.setValues({
                name: holiday.name,
                date: new Date(holiday.date),
                days: holiday.days
            });
        } else {
            setEditMode(false);
            setSelectedHoliday(null);
            formik.resetForm();
        }
        setEditDialogOpen(true);
    };

    const handleCloseEditDialog = () => {
        setEditDialogOpen(false);
        setSelectedHoliday(null);
        formik.resetForm();
    };

    const handleDeleteClick = (holiday) => {
        setSelectedHoliday(holiday);
        setDeleteDialogOpen(true);
    };

    const handleConfirmDelete = () => {
        setHolidays(holidays.filter(h => h.id !== selectedHoliday.id));
        setDeleteDialogOpen(false);
        setSelectedHoliday(null);
    };

    const columns = [
        { 
            field: 'name', 
            headerName: 'Tatil Adı', 
            flex: 2, 
            minWidth: 250 
        },
        { 
            field: 'date', 
            headerName: 'Tarih', 
            width: 150,
            valueFormatter: (params) => formatDate(params.value)
        },
        { 
            field: 'days', 
            headerName: 'Süre (Gün)', 
            width: 130,
            renderCell: (params) => (
                <Chip 
                    label={`${params.value} Gün`} 
                    size="small" 
                    color="primary" 
                    variant="outlined"
                />
            )
        },
        {
            field: 'actions',
            headerName: 'İşlemler',
            width: 120,
            renderCell: (params) => (
                <Box>
                    <IconButton 
                        size="small" 
                        color="primary"
                        onClick={() => handleOpenEditDialog(params.row)}
                    >
                        <EditIcon />
                    </IconButton>
                    <IconButton 
                        size="small" 
                        color="error"
                        onClick={() => handleDeleteClick(params.row)}
                    >
                        <DeleteIcon />
                    </IconButton>
                </Box>
            )
        }
    ];

    return (
        <LocalizationProvider dateAdapter={AdapterDateFns}>
            <Box sx={{ width: '100%' }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
                    <Box>
                        <Typography variant="h4" sx={{ fontWeight: 'bold' }}>
                            Resmi Tatiller
                        </Typography>
                        <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
                            Yılın resmi tatillerini yönetin. Sistem izin hesaplamalarında bu günleri dikkate alır.
                        </Typography>
                    </Box>
                    <Button 
                        variant="contained" 
                        startIcon={<AddIcon />}
                        onClick={() => handleOpenEditDialog()}
                    >
                        Tatil Ekle
                    </Button>
                </Box>
                <Paper sx={{ height: 'calc(100vh - 180px)', width: '100%', p: 3, borderRadius: 3, boxShadow: 3 }}>
                    <DataGrid
                        rows={holidays}
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

                {/* Add/Edit Dialog */}
                <Dialog open={editDialogOpen} onClose={handleCloseEditDialog} maxWidth="sm" fullWidth>
                    <DialogTitle>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                            <CalendarIcon color="primary" />
                            {editMode ? 'Tatil Düzenle' : 'Yeni Tatil Ekle'}
                        </Box>
                    </DialogTitle>
                    <Box component="form" onSubmit={formik.handleSubmit}>
                        <DialogContent>
                            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 1 }}>
                                <TextField
                                    fullWidth
                                    id="name"
                                    name="name"
                                    label="Tatil Adı"
                                    value={formik.values.name}
                                    onChange={formik.handleChange}
                                    error={formik.touched.name && Boolean(formik.errors.name)}
                                    helperText={formik.touched.name && formik.errors.name}
                                />
                                
                                <DatePicker
                                    label="Tatil Tarihi"
                                    value={formik.values.date}
                                    onChange={(value) => formik.setFieldValue('date', value)}
                                    slotProps={{
                                        textField: {
                                            fullWidth: true,
                                            error: formik.touched.date && Boolean(formik.errors.date),
                                            helperText: formik.touched.date && formik.errors.date
                                        }
                                    }}
                                />

                                <TextField
                                    fullWidth
                                    id="days"
                                    name="days"
                                    label="Süre (Gün)"
                                    type="number"
                                    inputProps={{ step: 0.5, min: 0.5 }}
                                    value={formik.values.days}
                                    onChange={formik.handleChange}
                                    error={formik.touched.days && Boolean(formik.errors.days)}
                                    helperText={formik.touched.days ? formik.errors.days : 'Örn: 1, 1.5, 2 gibi'}
                                />
                            </Box>
                        </DialogContent>
                        <DialogActions>
                            <Button onClick={handleCloseEditDialog}>İptal</Button>
                            <Button type="submit" variant="contained">Kaydet</Button>
                        </DialogActions>
                    </Box>
                </Dialog>

                {/* Delete Dialog */}
                <Dialog open={deleteDialogOpen} onClose={() => setDeleteDialogOpen(false)}>
                    <DialogTitle>Tatili Sil</DialogTitle>
                    <DialogContent>
                        <Typography variant="body2" color="text.secondary">
                            {selectedHoliday && (
                                <>
                                    <strong>{selectedHoliday.name}</strong> ({formatDate(selectedHoliday.date)}) 
                                    tatilini silmek istediğinize emin misiniz?
                                </>
                            )}
                        </Typography>
                    </DialogContent>
                    <DialogActions>
                        <Button onClick={() => setDeleteDialogOpen(false)}>İptal</Button>
                        <Button onClick={handleConfirmDelete} color="error" variant="contained">
                            Sil
                        </Button>
                    </DialogActions>
                </Dialog>
            </Box>
        </LocalizationProvider>
    );
};

export default Holidays;
