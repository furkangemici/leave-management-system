import React, { useState } from 'react';
import { 
    Box, 
    Typography, 
    Paper, 
    Grid, 
    TextField, 
    Button, 
    Avatar, 
    Chip, 
    Divider,
    Alert,
    Snackbar
} from '@mui/material';
import { Save as SaveIcon, Lock as LockIcon, Person as PersonIcon } from '@mui/icons-material';
import useAuth from '../../hooks/useAuth';
import { useFormik } from 'formik';
import * as Yup from 'yup';

const validationSchemaPassword = Yup.object({
    currentPassword: Yup.string().required('Mevcut şifre zorunludur'),
    newPassword: Yup.string().min(6, 'Yeni şifre en az 6 karakter olmalıdır').required('Yeni şifre zorunludur'),
    confirmPassword: Yup.string()
        .oneOf([Yup.ref('newPassword'), null], 'Şifreler eşleşmiyor')
        .required('Şifre tekrarı zorunludur'),
});

const validationSchemaInfo = Yup.object({
    phone: Yup.string().required('Telefon numarası zorunludur'),
    address: Yup.string().required('Adres zorunludur'),
});

const Profile = () => {
    const { auth } = useAuth();
    const [openSnackbar, setOpenSnackbar] = useState(false);
    const [snackbarMsg, setSnackbarMsg] = useState('');

    // Personal Info Form
    const formikInfo = useFormik({
        initialValues: {
            phone: auth?.user?.phone || '0555 123 45 67',
            address: auth?.user?.address || 'Örnek Mah. Teknoloji Cad. No:1 İstanbul',
        },
        validationSchema: validationSchemaInfo,
        onSubmit: (values) => {
            console.log("Info Update:", values);
            setSnackbarMsg('Kişisel bilgileriniz başarıyla güncellendi!');
            setOpenSnackbar(true);
        },
    });

    // Password Change Form
    const formikPassword = useFormik({
        initialValues: {
            currentPassword: '',
            newPassword: '',
            confirmPassword: '',
        },
        validationSchema: validationSchemaPassword,
        onSubmit: (values, { resetForm }) => {
            console.log("Password Change:", values);
            setSnackbarMsg('Şifreniz başarıyla güncellendi!');
            setOpenSnackbar(true);
            resetForm();
        },
    });

    return (
        <Box maxWidth="lg" sx={{ mx: 'auto', mt: 4 }}>
            <Typography variant="h4" sx={{ mb: 4, fontWeight: 'bold' }}>Profil Ayarları</Typography>

            <Grid container spacing={3}>
                {/* User Info Card (Read Only + Avatar) */}
                <Grid item xs={12} md={4}>
                    <Paper sx={{ p: 4, textAlign: 'center', borderRadius: 3, height: '100%' }}>
                        <Avatar 
                            sx={{ 
                                width: 120, 
                                height: 120, 
                                mx: 'auto', 
                                mb: 2, 
                                bgcolor: 'primary.main', 
                                fontSize: '3rem',
                                boxShadow: 3 
                            }}
                        >
                            {auth?.user?.name?.charAt(0) || 'U'}
                        </Avatar>
                        <Typography variant="h5" fontWeight="bold" sx={{ mb: 1 }}>
                            {auth?.user?.name || "Kullanıcı Adı"}
                        </Typography>
                        <Typography variant="body1" color="text.secondary" sx={{ mb: 3 }}>
                            {auth?.user?.email}
                        </Typography>
                        
                        <Box sx={{ display: 'flex', gap: 1, justifyContent: 'center', flexWrap: 'wrap', mb: 4 }}>
                             {auth?.user?.roles?.map(role => (
                                <Chip key={role} label={role} color="secondary" size="small" variant="outlined" />
                             ))}
                        </Box>
                        
                        <Divider sx={{ my: 3 }} />
                        
                        <Box sx={{ textAlign: 'left', px: 2 }}>
                            <Grid container spacing={2}>
                                <Grid item xs={12}>
                                    <Typography variant="caption" color="text.secondary" fontWeight="bold">DEPARTMAN</Typography>
                                    <Typography variant="body2" sx={{ fontWeight: 500 }}>Bilgi Teknolojileri</Typography>
                                </Grid>
                                <Grid item xs={12}>
                                    <Typography variant="caption" color="text.secondary" fontWeight="bold">ÜNVAN</Typography>
                                    <Typography variant="body2" sx={{ fontWeight: 500 }}>Yazılım Uzmanı</Typography>
                                </Grid>
                                <Grid item xs={12}>
                                    <Typography variant="caption" color="text.secondary" fontWeight="bold">İŞE GİRİŞ TARİHİ</Typography>
                                    <Typography variant="body2" sx={{ fontWeight: 500 }}>01.01.2023</Typography>
                                </Grid>
                            </Grid>
                        </Box>
                    </Paper>
                </Grid>

                <Grid item xs={12} md={8}>
                    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
                        
                        {/* Personal Information Form */}
                        <Paper sx={{ p: 4, borderRadius: 3 }}>
                            <Box sx={{ display: 'flex', alignItems: 'center', mb: 3 }}>
                                 <Box sx={{ p: 1, borderRadius: 2, bgcolor: 'info.main', color: 'white', mr: 2 }}>
                                    <PersonIcon />
                                 </Box>
                                 <Typography variant="h6" fontWeight="bold">Kişisel Bilgiler</Typography>
                            </Box>

                            <form onSubmit={formikInfo.handleSubmit}>
                                <Grid container spacing={3}>
                                    <Grid item xs={12} md={6}>
                                        <TextField
                                            fullWidth
                                            name="phone"
                                            label="Cep Telefonu"
                                            placeholder="05XX XXX XX XX"
                                            value={formikInfo.values.phone}
                                            onChange={formikInfo.handleChange}
                                            error={formikInfo.touched.phone && Boolean(formikInfo.errors.phone)}
                                            helperText={formikInfo.touched.phone && formikInfo.errors.phone}
                                        />
                                    </Grid>
                                    <Grid item xs={12} md={6}>
                                         {/* Placeholder for future fields like Birth Date if needed */}
                                    </Grid>
                                    <Grid item xs={12}>
                                        <TextField
                                            fullWidth
                                            multiline
                                            rows={2}
                                            name="address"
                                            label="Adres"
                                            placeholder="Açık adresinizi giriniz..."
                                            value={formikInfo.values.address}
                                            onChange={formikInfo.handleChange}
                                            error={formikInfo.touched.address && Boolean(formikInfo.errors.address)}
                                            helperText={formikInfo.touched.address && formikInfo.errors.address}
                                        />
                                    </Grid>
                                    <Grid item xs={12}>
                                        <Box sx={{ display: 'flex', justifyContent: 'flex-end', mt: 1 }}>
                                            <Button 
                                                type="submit" 
                                                variant="contained" 
                                                size="large"
                                                startIcon={<SaveIcon />}
                                                color="info"
                                                disabled={!formikInfo.dirty}
                                            >
                                                Bilgileri Güncelle
                                            </Button>
                                        </Box>
                                    </Grid>
                                </Grid>
                            </form>
                        </Paper>

                        {/* Password Change Form */}
                        <Paper sx={{ p: 4, borderRadius: 3 }}>
                            <Box sx={{ display: 'flex', alignItems: 'center', mb: 3 }}>
                                 <Box sx={{ p: 1, borderRadius: 2, bgcolor: 'warning.main', color: 'white', mr: 2 }}>
                                    <LockIcon />
                                 </Box>
                                 <Typography variant="h6" fontWeight="bold">Güvenlik & Şifre</Typography>
                            </Box>

                            <form onSubmit={formikPassword.handleSubmit}>
                                <Grid container spacing={3}>
                                    <Grid item xs={12}>
                                        <TextField
                                            fullWidth
                                            type="password"
                                            name="currentPassword"
                                            label="Mevcut Şifre"
                                            value={formikPassword.values.currentPassword}
                                            onChange={formikPassword.handleChange}
                                            error={formikPassword.touched.currentPassword && Boolean(formikPassword.errors.currentPassword)}
                                            helperText={formikPassword.touched.currentPassword && formikPassword.errors.currentPassword}
                                        />
                                    </Grid>
                                    <Grid item xs={12} md={6}>
                                        <TextField
                                            fullWidth
                                            type="password"
                                            name="newPassword"
                                            label="Yeni Şifre"
                                            value={formikPassword.values.newPassword}
                                            onChange={formikPassword.handleChange}
                                            error={formikPassword.touched.newPassword && Boolean(formikPassword.errors.newPassword)}
                                            helperText={formikPassword.touched.newPassword && formikPassword.errors.newPassword}
                                        />
                                    </Grid>
                                    <Grid item xs={12} md={6}>
                                        <TextField
                                            fullWidth
                                            type="password"
                                            name="confirmPassword"
                                            label="Yeni Şifre (Tekrar)"
                                            value={formikPassword.values.confirmPassword}
                                            onChange={formikPassword.handleChange}
                                            error={formikPassword.touched.confirmPassword && Boolean(formikPassword.errors.confirmPassword)}
                                            helperText={formikPassword.touched.confirmPassword && formikPassword.errors.confirmPassword}
                                        />
                                    </Grid>
                                    <Grid item xs={12}>
                                        <Box sx={{ display: 'flex', justifyContent: 'flex-end', mt: 1 }}>
                                            <Button 
                                                type="submit" 
                                                variant="contained" 
                                                size="large"
                                                startIcon={<SaveIcon />}
                                                color="warning"
                                                disabled={!formikPassword.isValid || !formikPassword.dirty}
                                            >
                                                Şifreyi Güncelle
                                            </Button>
                                        </Box>
                                    </Grid>
                                </Grid>
                            </form>
                        </Paper>
                    </Box>
                </Grid>
            </Grid>

            <Snackbar
                open={openSnackbar}
                autoHideDuration={4000}
                onClose={() => setOpenSnackbar(false)}
                anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
            >
                <Alert onClose={() => setOpenSnackbar(false)} severity="success" sx={{ width: '100%' }}>
                    {snackbarMsg}
                </Alert>
            </Snackbar>
        </Box>
    );
};

export default Profile;
