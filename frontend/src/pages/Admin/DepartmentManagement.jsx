import React, { useState } from 'react';
import { 
    Box, 
    Typography, 
    Paper, 
    Button, 
    Dialog,
    DialogTitle,
    DialogContent,
    DialogActions,
    TextField,
    FormControl,
    InputLabel,
    Select,
    MenuItem,
    FormHelperText,
    IconButton,
    Chip,
    Avatar
} from '@mui/material';
import { DataGrid } from '@mui/x-data-grid';
import { trTR } from '@mui/x-data-grid/locales';
import { 
    Add as AddIcon, 
    Edit as EditIcon, 
    Delete as DeleteIcon,
    SupervisorAccount as ManagerIcon
} from '@mui/icons-material';
import { useFormik } from 'formik';
import * as Yup from 'yup';

// Mock Data
const mockManagers = [
    { id: 1, name: 'Mehmet Öz', email: 'mehmet@cozumtr.com' },
    { id: 2, name: 'Ali Veli', email: 'ali@cozumtr.com' },
    { id: 3, name: 'Zeynep Kaya', email: 'zeynep@cozumtr.com' },
    { id: 4, name: 'Ahmet Yılmaz', email: 'ahmet@cozumtr.com' }
];

const initialDepartments = [
    { 
        id: 1, 
        name: 'Yazılım', 
        managerId: 1, 
        managerName: 'Mehmet Öz',
        employeeCount: 25,
        description: 'Yazılım geliştirme ve teknoloji departmanı'
    },
    { 
        id: 2, 
        name: 'Satış', 
        managerId: 2, 
        managerName: 'Ali Veli',
        employeeCount: 18,
        description: 'Satış ve pazarlama departmanı'
    },
    { 
        id: 3, 
        name: 'İnsan Kaynakları', 
        managerId: 3, 
        managerName: 'Zeynep Kaya',
        employeeCount: 8,
        description: 'İnsan kaynakları ve personel yönetimi'
    },
    { 
        id: 4, 
        name: 'Muhasebe', 
        managerId: null, 
        managerName: null,
        employeeCount: 12,
        description: 'Mali işler ve muhasebe departmanı'
    },
];

const DepartmentManagement = () => {
    const [departments, setDepartments] = useState(initialDepartments);
    const [editDialogOpen, setEditDialogOpen] = useState(false);
    const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
    const [selectedDepartment, setSelectedDepartment] = useState(null);
    const [editMode, setEditMode] = useState(false);

    const validationSchema = Yup.object({
        name: Yup.string().required('Departman adı zorunludur'),
        description: Yup.string(),
        managerId: Yup.string()
    });

    const formik = useFormik({
        initialValues: {
            name: '',
            description: '',
            managerId: ''
        },
        validationSchema: validationSchema,
        onSubmit: (values) => {
            if (editMode && selectedDepartment) {
                const manager = mockManagers.find(m => m.id === parseInt(values.managerId));
                setDepartments(departments.map(d => 
                    d.id === selectedDepartment.id 
                        ? { 
                            ...d, 
                            ...values, 
                            managerId: values.managerId ? parseInt(values.managerId) : null,
                            managerName: manager?.name || null
                        } 
                        : d
                ));
            } else {
                const newId = Math.max(...departments.map(d => d.id), 0) + 1;
                const manager = mockManagers.find(m => m.id === parseInt(values.managerId));
                setDepartments([...departments, {
                    id: newId,
                    ...values,
                    managerId: values.managerId ? parseInt(values.managerId) : null,
                    managerName: manager?.name || null,
                    employeeCount: 0
                }]);
            }
            handleCloseEditDialog();
        },
    });

    const handleOpenEditDialog = (dept = null) => {
        if (dept) {
            setEditMode(true);
            setSelectedDepartment(dept);
            formik.setValues({
                name: dept.name,
                description: dept.description || '',
                managerId: dept.managerId?.toString() || ''
            });
        } else {
            setEditMode(false);
            setSelectedDepartment(null);
            formik.resetForm();
        }
        setEditDialogOpen(true);
    };

    const handleCloseEditDialog = () => {
        setEditDialogOpen(false);
        setSelectedDepartment(null);
        formik.resetForm();
    };

    const handleDeleteClick = (dept) => {
        setSelectedDepartment(dept);
        setDeleteDialogOpen(true);
    };

    const handleConfirmDelete = () => {
        // Check if department has employees
        if (selectedDepartment.employeeCount > 0) {
            alert('Bu departmanda çalışan bulunduğu için silinemez. Önce çalışanları başka departmana taşıyın.');
            setDeleteDialogOpen(false);
            return;
        }
        setDepartments(departments.filter(d => d.id !== selectedDepartment.id));
        setDeleteDialogOpen(false);
        setSelectedDepartment(null);
    };

    const columns = [
        { 
            field: 'name', 
            headerName: 'Departman Adı', 
            flex: 1.5,
            minWidth: 200
        },
        { 
            field: 'description', 
            headerName: 'Açıklama', 
            flex: 2,
            minWidth: 250
        },
        { 
            field: 'managerName', 
            headerName: 'Yönetici', 
            flex: 1.5,
            minWidth: 180,
            renderCell: (params) => (
                params.value ? (
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                        <Avatar sx={{ width: 32, height: 32, bgcolor: 'primary.main' }}>
                            {params.value.charAt(0)}
                        </Avatar>
                        <Typography variant="body2">{params.value}</Typography>
                    </Box>
                ) : (
                    <Chip 
                        label="Yönetici Atanmamış" 
                        size="small" 
                        color="warning" 
                        variant="outlined"
                    />
                )
            )
        },
        { 
            field: 'employeeCount', 
            headerName: 'Çalışan Sayısı', 
            flex: 1,
            minWidth: 120,
            renderCell: (params) => (
                <Chip 
                    label={params.value} 
                    size="small" 
                    color="primary" 
                    variant="outlined"
                />
            )
        },
        {
            field: 'actions',
            headerName: 'İşlemler',
            flex: 1,
            minWidth: 120,
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
        <Box sx={{ width: '100%' }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
                <Box>
                    <Typography variant="h4" fontWeight="bold">
                        Departman Yönetimi
                    </Typography>
                    <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
                        Şirket hiyerarşisini yönetin ve departman yöneticilerini atayın
                    </Typography>
                </Box>
                <Button 
                    variant="contained" 
                    startIcon={<AddIcon />}
                    onClick={() => handleOpenEditDialog()}
                >
                    Yeni Departman
                </Button>
            </Box>

            <Paper sx={{ height: 'calc(100vh - 200px)', width: '100%', p: 3, borderRadius: 3, boxShadow: 3 }}>
                <DataGrid
                    rows={departments}
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
                    {editMode ? 'Departman Düzenle' : 'Yeni Departman Ekle'}
                </DialogTitle>
                <Box component="form" onSubmit={formik.handleSubmit}>
                    <DialogContent>
                        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 1 }}>
                            <TextField
                                fullWidth
                                id="name"
                                name="name"
                                label="Departman Adı"
                                value={formik.values.name}
                                onChange={formik.handleChange}
                                error={formik.touched.name && Boolean(formik.errors.name)}
                                helperText={formik.touched.name && formik.errors.name}
                            />
                            
                            <TextField
                                fullWidth
                                id="description"
                                name="description"
                                label="Açıklama"
                                multiline
                                rows={3}
                                value={formik.values.description}
                                onChange={formik.handleChange}
                            />

                            <FormControl fullWidth>
                                <InputLabel>Yönetici</InputLabel>
                                <Select
                                    id="managerId"
                                    name="managerId"
                                    value={formik.values.managerId}
                                    label="Yönetici"
                                    onChange={formik.handleChange}
                                >
                                    <MenuItem value="">Yönetici Atanmamış</MenuItem>
                                    {mockManagers.map((manager) => (
                                        <MenuItem key={manager.id} value={manager.id.toString()}>
                                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                                                <ManagerIcon fontSize="small" />
                                                {manager.name} ({manager.email})
                                            </Box>
                                        </MenuItem>
                                    ))}
                                </Select>
                                <FormHelperText>
                                    Bu departmanın yöneticisini seçiniz. İzin talepleri bu kişiye yönlendirilecektir.
                                </FormHelperText>
                            </FormControl>
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
                <DialogTitle>Departmanı Sil</DialogTitle>
                <DialogContent>
                    <Typography variant="body2" color="text.secondary">
                        {selectedDepartment && (
                            <>
                                <strong>{selectedDepartment.name}</strong> departmanını silmek istediğinize emin misiniz?
                                <br/><br/>
                                {selectedDepartment.employeeCount > 0 && (
                                    <Typography variant="body2" color="error" sx={{ mt: 1 }}>
                                        ⚠️ Bu departmanda {selectedDepartment.employeeCount} çalışan bulunmaktadır. 
                                        Önce çalışanları başka departmana taşımanız gerekmektedir.
                                    </Typography>
                                )}
                            </>
                        )}
                    </Typography>
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setDeleteDialogOpen(false)}>İptal</Button>
                    <Button 
                        onClick={handleConfirmDelete} 
                        color="error" 
                        variant="contained"
                        disabled={selectedDepartment?.employeeCount > 0}
                    >
                        Sil
                    </Button>
                </DialogActions>
            </Dialog>
        </Box>
    );
};

export default DepartmentManagement;

