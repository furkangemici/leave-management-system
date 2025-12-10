import React, { useState } from 'react';
import { 
    Box, 
    Typography, 
    Button, 
    Paper, 
    Dialog, 
    DialogTitle, 
    DialogContent, 
    DialogActions, 
    TextField, 
    FormControl, 
    InputLabel, 
    Select, 
    MenuItem, 
    FormControlLabel, 
    Switch,
    IconButton,
    Tooltip,
    Chip,
    FormHelperText
} from '@mui/material';
import { DataGrid, GridActionsCellItem } from '@mui/x-data-grid';
import { trTR } from '@mui/x-data-grid/locales';
import { 
    Add as AddIcon, 
    Edit as EditIcon, 
    Delete as DeleteIcon 
} from '@mui/icons-material';
import { useFormik } from 'formik';
import * as Yup from 'yup';

// Mock Data
const initialLeaveTypes = [
    { id: 1, name: 'Yıllık İzin', unit: 'DAILY', deductsFromAnnual: true, isPaid: true, isActive: true },
    { id: 2, name: 'Mazeret İzni', unit: 'HOURLY', deductsFromAnnual: false, isPaid: true, isActive: true },
    { id: 3, name: 'Hastalık İzni', unit: 'DAILY', deductsFromAnnual: false, isPaid: true, isActive: true },
    { id: 4, name: 'Ücretsiz İzin', unit: 'DAILY', deductsFromAnnual: false, isPaid: false, isActive: true },
];

const LeaveTypes = () => {
    const [leaveTypes, setLeaveTypes] = useState(initialLeaveTypes);
    const [open, setOpen] = useState(false);
    const [editMode, setEditMode] = useState(false);
    const [currentId, setCurrentId] = useState(null);

    const validationSchema = Yup.object({
        name: Yup.string().required('İzin türü adı zorunludur'),
        unit: Yup.string().oneOf(['DAILY', 'HOURLY']).required('Birim seçimi zorunludur'),
    });

    const formik = useFormik({
        initialValues: {
            name: '',
            unit: 'DAILY',
            deductsFromAnnual: false,
            isPaid: true,
            isActive: true
        },
        validationSchema: validationSchema,
        onSubmit: (values) => {
            if (editMode && currentId) {
                // Edit Logic
                setLeaveTypes(prev => prev.map(item => 
                    item.id === currentId ? { ...values, id: currentId } : item
                ));
            } else {
                // Add Logic
                const newId = Math.max(...leaveTypes.map(t => t.id), 0) + 1;
                setLeaveTypes(prev => [...prev, { ...values, id: newId }]);
            }
            handleClose();
        },
    });

    const handleOpen = (type = null) => {
        if (type) {
            setEditMode(true);
            setCurrentId(type.id);
            formik.setValues({
                name: type.name,
                unit: type.unit,
                deductsFromAnnual: type.deductsFromAnnual,
                isPaid: type.isPaid,
                isActive: type.isActive
            });
        } else {
            setEditMode(false);
            setCurrentId(null);
            formik.resetForm();
        }
        setOpen(true);
    };

    const handleClose = () => {
        setOpen(false);
        formik.resetForm();
    };

    const handleDelete = (id) => {
        if (window.confirm('Bu izin türünü silmek istediğinize emin misiniz?')) {
            setLeaveTypes(prev => prev.filter(item => item.id !== id));
        }
    };

    const columns = [
        { field: 'name', headerName: 'İzin Türü Adı', flex: 1, minWidth: 150 },
        { 
            field: 'unit', 
            headerName: 'Talep Birimi', 
            width: 130,
            renderCell: (params) => (
                <Chip 
                    label={params.value === 'DAILY' ? 'Günlük' : 'Saatlik'} 
                    color="primary" 
                    variant="outlined" 
                    size="small" 
                />
            )
        },
        { 
            field: 'deductsFromAnnual', 
            headerName: 'Yıllık İzinden Düşer', 
            width: 150, 
            type: 'boolean' 
        },
        { 
            field: 'isPaid', 
            headerName: 'Ücretli İzin', 
            width: 120, 
            type: 'boolean' 
        },
        { 
            field: 'isActive', 
            headerName: 'Durum', 
            width: 100, 
            type: 'boolean',
            renderCell: (params) => (
                <Chip 
                    label={params.value ? 'Aktif' : 'Pasif'} 
                    color={params.value ? 'success' : 'default'} 
                    size="small" 
                />
            )
        },
        {
            field: 'actions',
            type: 'actions',
            headerName: 'İşlemler',
            width: 100,
            getActions: (params) => [
                <GridActionsCellItem
                    icon={<EditIcon />}
                    label="Düzenle"
                    onClick={() => handleOpen(params.row)}
                />,
                <GridActionsCellItem
                    icon={<DeleteIcon />}
                    label="Sil"
                    onClick={() => handleDelete(params.row.id)}
                    showInMenu
                />,
            ],
        },
    ];

    return (
        <Box sx={{ width: '100%' }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
                <Typography variant="h4" sx={{ fontWeight: 'bold' }}>
                    İzin Türü Yönetimi
                </Typography>
                <Button 
                    variant="contained" 
                    startIcon={<AddIcon />} 
                    onClick={() => handleOpen()}
                >
                    Yeni İzin Türü Ekle
                </Button>
            </Box>

            <Paper sx={{ height: 'calc(100vh - 180px)', width: '100%', p: 3 }}>
                <DataGrid
                    rows={leaveTypes}
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
            <Dialog open={open} onClose={handleClose} maxWidth="sm" fullWidth>
                <DialogTitle>{editMode ? 'İzin Türünü Düzenle' : 'Yeni İzin Türü Ekle'}</DialogTitle>
                <Box component="form" onSubmit={formik.handleSubmit}>
                    <DialogContent>
                        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 1 }}>
                            <TextField
                                fullWidth
                                id="name"
                                name="name"
                                label="İzin Türü Adı"
                                value={formik.values.name}
                                onChange={formik.handleChange}
                                error={formik.touched.name && Boolean(formik.errors.name)}
                                helperText={formik.touched.name && formik.errors.name}
                            />
                            
                            <FormControl fullWidth>
                                <InputLabel id="unit-label">Talep Birimi</InputLabel>
                                <Select
                                    labelId="unit-label"
                                    id="unit"
                                    name="unit"
                                    value={formik.values.unit}
                                    label="Talep Birimi"
                                    onChange={formik.handleChange}
                                >
                                    <MenuItem value="DAILY">Günlük (Start Date - End Date)</MenuItem>
                                    <MenuItem value="HOURLY">Saatlik (Date + Time Range)</MenuItem>
                                </Select>
                            </FormControl>

                            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
                                <FormControlLabel
                                    control={
                                        <Switch
                                            checked={formik.values.deductsFromAnnual}
                                            onChange={formik.handleChange}
                                            name="deductsFromAnnual"
                                        />
                                    }
                                    label="Yıllık izinden düşer mi?"
                                />
                                <FormHelperText sx={{ mt: -1, ml: 1.5 }}>İşaretlenirse, personelin yıllık izin bakiyesinden eksiltilir.</FormHelperText>
                                
                                <FormControlLabel
                                    control={
                                        <Switch
                                            checked={formik.values.isPaid}
                                            onChange={formik.handleChange}
                                            name="isPaid"
                                        />
                                    }
                                    label="Ücretli izin mi?"
                                />
                                
                                <FormControlLabel
                                    control={
                                        <Switch
                                            checked={formik.values.isActive}
                                            onChange={formik.handleChange}
                                            name="isActive"
                                        />
                                    }
                                    label="Aktif mi?"
                                />
                            </Box>
                        </Box>
                    </DialogContent>
                    <DialogActions>
                        <Button onClick={handleClose}>İptal</Button>
                        <Button type="submit" variant="contained">Kaydet</Button>
                    </DialogActions>
                </Box>
            </Dialog>
        </Box>
    );
};

export default LeaveTypes;
