import React, { useState } from 'react';
import { 
    Box, 
    Typography, 
    Paper, 
    Button, 
    IconButton, 
    Avatar, 
    Chip,
    Dialog,
    DialogTitle,
    DialogContent,
    DialogContentText,
    DialogActions,
    TextField,
    FormControl,
    InputLabel,
    Select,
    MenuItem,
    FormHelperText,
    Divider
} from '@mui/material';
import { DataGrid } from '@mui/x-data-grid';
import { trTR } from '@mui/x-data-grid/locales';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { AdapterDateFns } from '@mui/x-date-pickers/AdapterDateFns';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import { 
    Edit as EditIcon, 
    Delete as DeleteIcon, 
    Add as AddIcon,
    Block as BlockIcon,
    AccountBalanceWallet as WalletIcon
} from '@mui/icons-material';
import { useFormik } from 'formik';
import * as Yup from 'yup';
import { formatDate } from '../../utils/timeHelpers';

// Mock Data
const mockDepartments = ['Yazılım', 'İnsan Kaynakları', 'Satış', 'Muhasebe', 'IT'];
const mockManagers = [
    { id: 3, name: 'Mehmet Öz', department: 'Yazılım' },
    { id: 5, name: 'Ali Veli', department: 'Satış' },
    { id: 6, name: 'Zeynep Kaya', department: 'İnsan Kaynakları' }
];

const initialUsers = [
    { 
        id: 1, 
        name: 'Ahmet Yılmaz', 
        email: 'ahmet@cozumtr.com', 
        role: 'EMPLOYEE', 
        department: 'Yazılım', 
        managerId: 3,
        managerName: 'Mehmet Öz',
        startDate: '2023-01-15',
        status: 'ACTIVE', 
        leaveBalance: 14, 
        avatar: '' 
    },
    { 
        id: 2, 
        name: 'Ayşe Demir', 
        email: 'ayse@cozumtr.com', 
        role: 'HR', 
        department: 'İnsan Kaynakları', 
        managerId: 6,
        managerName: 'Zeynep Kaya',
        startDate: '2022-06-01',
        status: 'ACTIVE', 
        leaveBalance: 20, 
        avatar: '' 
    },
    { 
        id: 3, 
        name: 'Mehmet Öz', 
        email: 'mehmet@cozumtr.com', 
        role: 'MANAGER', 
        department: 'Yazılım', 
        managerId: null,
        managerName: null,
        startDate: '2020-03-10',
        status: 'ACTIVE', 
        leaveBalance: 18, 
        avatar: '' 
    },
    { 
        id: 4, 
        name: 'İK Yöneticisi', 
        email: 'hr@cozumtr.com', 
        role: 'HR', 
        department: 'İnsan Kaynakları', 
        managerId: null,
        managerName: null,
        startDate: '2019-01-01',
        status: 'ACTIVE', 
        leaveBalance: 999, 
        avatar: '' 
    },
    { 
        id: 5, 
        name: 'Muhasebe Uzmanı', 
        email: 'muhasebe@cozumtr.com', 
        role: 'ACCOUNTANT', 
        department: 'Muhasebe', 
        managerId: null,
        managerName: null,
        startDate: '2021-05-01',
        status: 'ACTIVE', 
        leaveBalance: 0, 
        avatar: '' 
    },
];

const UserManagement = () => {
    const [users, setUsers] = useState(initialUsers);
    const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
    const [editDialogOpen, setEditDialogOpen] = useState(false);
    const [balanceDialogOpen, setBalanceDialogOpen] = useState(false);
    const [selectedUser, setSelectedUser] = useState(null);
    const [editMode, setEditMode] = useState(false);

    const validationSchema = Yup.object({
        name: Yup.string().required('Ad Soyad zorunludur'),
        email: Yup.string().email('Geçerli bir e-posta adresi giriniz').required('E-posta zorunludur'),
        role: Yup.string().required('Rol seçimi zorunludur'),
        department: Yup.string().required('Departman seçimi zorunludur'),
        startDate: Yup.date().required('İşe başlama tarihi zorunludur').nullable(),
    });

    const formik = useFormik({
        initialValues: {
            name: '',
            email: '',
            role: 'EMPLOYEE',
            department: '',
            managerId: '',
            startDate: null,
            leaveBalance: 0,
            status: 'ACTIVE'
        },
        validationSchema: validationSchema,
        onSubmit: (values) => {
            if (editMode && selectedUser) {
                const manager = mockManagers.find(m => m.id === parseInt(values.managerId));
                setUsers(users.map(u => 
                    u.id === selectedUser.id 
                        ? { 
                            ...u, 
                            ...values, 
                            managerId: values.managerId ? parseInt(values.managerId) : null,
                            managerName: manager?.name || null
                        } 
                        : u
                ));
            } else {
                const newId = Math.max(...users.map(u => u.id), 0) + 1;
                const manager = mockManagers.find(m => m.id === parseInt(values.managerId));
                setUsers([...users, {
                    id: newId,
                    ...values,
                    managerId: values.managerId ? parseInt(values.managerId) : null,
                    managerName: manager?.name || null,
                    avatar: ''
                }]);
            }
            handleCloseEditDialog();
        },
    });

    const handleOpenEditDialog = (user = null) => {
        if (user) {
            setEditMode(true);
            setSelectedUser(user);
            formik.setValues({
                name: user.name,
                email: user.email,
                role: user.role,
                department: user.department,
                managerId: user.managerId?.toString() || '',
                startDate: user.startDate ? new Date(user.startDate) : null,
                leaveBalance: user.leaveBalance,
                status: user.status
            });
        } else {
            setEditMode(false);
            setSelectedUser(null);
            formik.resetForm();
        }
        setEditDialogOpen(true);
    };

    const handleCloseEditDialog = () => {
        setEditDialogOpen(false);
        setSelectedUser(null);
        formik.resetForm();
    };

    const handleDeleteClick = (user) => {
        setSelectedUser(user);
        setDeleteDialogOpen(true);
    };

    const handleConfirmDelete = () => {
        setUsers(users.map(u => u.id === selectedUser.id ? { ...u, status: 'INACTIVE' } : u));
        setDeleteDialogOpen(false);
        setSelectedUser(null);
    };

    const handleBalanceClick = (user) => {
        setSelectedUser(user);
        setBalanceDialogOpen(true);
    };

    const handleConfirmBalanceChange = (newBalance) => {
        setUsers(users.map(u => u.id === selectedUser.id ? { ...u, leaveBalance: newBalance } : u));
        setBalanceDialogOpen(false);
        setSelectedUser(null);
    };

    const columns = [
        { 
            field: 'user', 
            headerName: 'Kullanıcı', 
            flex: 1.5,
            minWidth: 200,
            renderCell: (params) => (
                <Box sx={{ display: 'flex', alignItems: 'center' }}>
                    <Avatar src={params.row.avatar} sx={{ mr: 2, bgcolor: 'primary.light' }}>
                        {params.row.name.charAt(0)}
                    </Avatar>
                    <Box>
                        <Typography variant="body2" fontWeight="bold">{params.row.name}</Typography>
                        <Typography variant="caption" color="text.secondary">{params.row.email}</Typography>
                    </Box>
                </Box>
            )
        },
        { field: 'department', headerName: 'Departman', flex: 1, minWidth: 120 },
        { 
            field: 'managerName', 
            headerName: 'Yönetici', 
            flex: 1, 
            minWidth: 150,
            renderCell: (params) => (
                params.value ? (
                    <Chip label={params.value} size="small" variant="outlined" color="info" />
                ) : (
                    <Typography variant="caption" color="text.secondary">Yönetici Yok</Typography>
                )
            )
        },
        { 
            field: 'role', 
            headerName: 'Rol', 
            flex: 1, 
            minWidth: 120,
            renderCell: (params) => {
                const roleLabels = {
                    'HR': 'İK / Admin',
                    'MANAGER': 'Departman Yöneticisi',
                    'EMPLOYEE': 'Çalışan',
                    'ACCOUNTANT': 'Muhasebe'
                };
                return (
                    <Chip 
                        label={roleLabels[params.value] || params.value} 
                        size="small" 
                        variant="outlined" 
                        color={params.value === 'HR' ? 'error' : params.value === 'ACCOUNTANT' ? 'info' : 'primary'}
                    />
                );
            }
        },
        { 
            field: 'startDate', 
            headerName: 'İşe Başlama', 
            flex: 1, 
            minWidth: 130,
            valueFormatter: (params) => params.value ? formatDate(params.value) : '-'
        },
        { 
            field: 'leaveBalance', 
            headerName: 'İzin Bakiyesi', 
            flex: 1, 
            minWidth: 130,
            renderCell: (params) => (
                params.row.role === 'HR' || params.row.role === 'ACCOUNTANT' ? (
                    <Typography variant="caption" color="text.secondary">
                        {params.row.role === 'HR' ? 'Sınırsız' : 'N/A'}
                    </Typography>
                ) : (
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                        <Typography variant="body2" fontWeight="bold" color="primary.main">
                            {params.value} Gün
                        </Typography>
                        <IconButton 
                            size="small" 
                            onClick={() => handleBalanceClick(params.row)}
                            sx={{ p: 0.5 }}
                        >
                            <WalletIcon fontSize="small" />
                        </IconButton>
                    </Box>
                )
            )
        },
        { 
            field: 'status', 
            headerName: 'Durum', 
            flex: 0.8, 
            minWidth: 100,
            renderCell: (params) => (
                <Chip 
                    label={params.value === 'ACTIVE' ? 'Aktif' : 'Pasif'} 
                    color={params.value === 'ACTIVE' ? 'success' : 'default'} 
                    size="small" 
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
                        {params.row.status === 'ACTIVE' ? <DeleteIcon /> : <BlockIcon />}
                    </IconButton>
                </Box>
            )
        }
    ];

    return (
        <LocalizationProvider dateAdapter={AdapterDateFns}>
            <Box sx={{ width: '100%' }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
                    <Typography variant="h4" fontWeight="bold">
                        Kullanıcı Yönetimi
                    </Typography>
                    <Button 
                        variant="contained" 
                        startIcon={<AddIcon />}
                        onClick={() => handleOpenEditDialog()}
                    >
                        Yeni Kullanıcı
                    </Button>
                </Box>

                <Paper sx={{ height: 'calc(100vh - 200px)', width: '100%', p: 3, borderRadius: 3, boxShadow: 3 }}>
                    <DataGrid
                        rows={users}
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
                <Dialog open={editDialogOpen} onClose={handleCloseEditDialog} maxWidth="md" fullWidth>
                    <DialogTitle>
                        {editMode ? 'Kullanıcı Düzenle' : 'Yeni Kullanıcı Ekle'}
                    </DialogTitle>
                    <Box component="form" onSubmit={formik.handleSubmit}>
                        <DialogContent>
                            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 1 }}>
                                <TextField
                                    fullWidth
                                    id="name"
                                    name="name"
                                    label="Ad Soyad"
                                    value={formik.values.name}
                                    onChange={formik.handleChange}
                                    error={formik.touched.name && Boolean(formik.errors.name)}
                                    helperText={formik.touched.name && formik.errors.name}
                                />
                                
                                <TextField
                                    fullWidth
                                    id="email"
                                    name="email"
                                    label="E-posta"
                                    type="email"
                                    value={formik.values.email}
                                    onChange={formik.handleChange}
                                    error={formik.touched.email && Boolean(formik.errors.email)}
                                    helperText={formik.touched.email && formik.errors.email}
                                />

                                <FormControl fullWidth error={formik.touched.role && Boolean(formik.errors.role)}>
                                    <InputLabel>Rol</InputLabel>
                                    <Select
                                        id="role"
                                        name="role"
                                        value={formik.values.role}
                                        label="Rol"
                                        onChange={formik.handleChange}
                                    >
                                        <MenuItem value="EMPLOYEE">Çalışan</MenuItem>
                                        <MenuItem value="MANAGER">Departman Yöneticisi</MenuItem>
                                        <MenuItem value="HR">İnsan Kaynakları (Admin Yetkisi)</MenuItem>
                                        <MenuItem value="ACCOUNTANT">Muhasebe</MenuItem>
                                    </Select>
                                    <FormHelperText>{formik.touched.role && formik.errors.role}</FormHelperText>
                                </FormControl>

                                <FormControl fullWidth error={formik.touched.department && Boolean(formik.errors.department)}>
                                    <InputLabel>Departman</InputLabel>
                                    <Select
                                        id="department"
                                        name="department"
                                        value={formik.values.department}
                                        label="Departman"
                                        onChange={formik.handleChange}
                                    >
                                        {mockDepartments.map((dept) => (
                                            <MenuItem key={dept} value={dept}>{dept}</MenuItem>
                                        ))}
                                    </Select>
                                    <FormHelperText>{formik.touched.department && formik.errors.department}</FormHelperText>
                                </FormControl>

                                {(formik.values.role === 'EMPLOYEE' || formik.values.role === 'ACCOUNTANT') && (
                                    <FormControl fullWidth>
                                        <InputLabel>Yönetici</InputLabel>
                                        <Select
                                            id="managerId"
                                            name="managerId"
                                            value={formik.values.managerId}
                                            label="Yönetici"
                                            onChange={formik.handleChange}
                                        >
                                            <MenuItem value="">Yönetici Yok</MenuItem>
                                            {mockManagers
                                                .filter(m => m.department === formik.values.department)
                                                .map((manager) => (
                                                    <MenuItem key={manager.id} value={manager.id.toString()}>
                                                        {manager.name}
                                                    </MenuItem>
                                                ))}
                                        </Select>
                                        <FormHelperText>Bu kişinin bağlı olduğu departman yöneticisini seçiniz</FormHelperText>
                                    </FormControl>
                                )}

                                <DatePicker
                                    label="İşe Başlama Tarihi"
                                    value={formik.values.startDate}
                                    onChange={(value) => formik.setFieldValue('startDate', value)}
                                    slotProps={{
                                        textField: {
                                            fullWidth: true,
                                            error: formik.touched.startDate && Boolean(formik.errors.startDate),
                                            helperText: formik.touched.startDate && formik.errors.startDate
                                        }
                                    }}
                                />

                                {formik.values.role !== 'HR' && formik.values.role !== 'ACCOUNTANT' && (
                                    <TextField
                                        fullWidth
                                        id="leaveBalance"
                                        name="leaveBalance"
                                        label="İzin Bakiyesi (Gün)"
                                        type="number"
                                        value={formik.values.leaveBalance}
                                        onChange={formik.handleChange}
                                        helperText="Manuel bakiye ayarlaması yapabilirsiniz"
                                    />
                                )}
                            </Box>
                        </DialogContent>
                        <DialogActions>
                            <Button onClick={handleCloseEditDialog}>İptal</Button>
                            <Button type="submit" variant="contained">Kaydet</Button>
                        </DialogActions>
                    </Box>
                </Dialog>

                {/* Balance Adjustment Dialog */}
                <Dialog open={balanceDialogOpen} onClose={() => setBalanceDialogOpen(false)}>
                    <DialogTitle>İzin Bakiyesi Düzenle</DialogTitle>
                    <DialogContent>
                        <Box sx={{ pt: 2 }}>
                            <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                                {selectedUser?.name} için izin bakiyesini düzenleyiniz:
                            </Typography>
                            <TextField
                                autoFocus
                                fullWidth
                                type="number"
                                label="Yeni Bakiye (Gün)"
                                defaultValue={selectedUser?.leaveBalance}
                                inputProps={{ min: 0 }}
                                onBlur={(e) => {
                                    const newBalance = parseInt(e.target.value);
                                    if (!isNaN(newBalance) && newBalance >= 0) {
                                        handleConfirmBalanceChange(newBalance);
                                    }
                                }}
                            />
                        </Box>
                    </DialogContent>
                    <DialogActions>
                        <Button onClick={() => setBalanceDialogOpen(false)}>İptal</Button>
                    </DialogActions>
                </Dialog>

                {/* Soft Delete Dialog */}
                <Dialog open={deleteDialogOpen} onClose={() => setDeleteDialogOpen(false)}>
                    <DialogTitle>Kullanıcıyı Pasife Al</DialogTitle>
                    <DialogContent>
                        <DialogContentText>
                            {selectedUser && (
                                <>
                                    <strong>{selectedUser.name}</strong> isimli kullanıcıyı pasife almak istediğinize emin misiniz?
                                    <br/>
                                    Bu işlem veriyi silmez, sadece sisteme girişini engeller.
                                </>
                            )}
                        </DialogContentText>
                    </DialogContent>
                    <DialogActions>
                        <Button onClick={() => setDeleteDialogOpen(false)}>İptal</Button>
                        <Button onClick={handleConfirmDelete} color="error" variant="contained">
                            Evet, Pasife Al
                        </Button>
                    </DialogActions>
                </Dialog>
            </Box>
        </LocalizationProvider>
    );
};

export default UserManagement;
