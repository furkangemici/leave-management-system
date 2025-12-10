import { useState } from 'react';
import { useFormik } from 'formik';
import * as Yup from 'yup';
import { 
    Button, 
    TextField, 
    FormControlLabel, 
    Checkbox, 
    Box, 
    Typography, 
    Paper, 
    Alert,
    CircularProgress,
    IconButton,
    InputAdornment,
    Fade,
    Link
} from '@mui/material';
import { 
    EmailOutlined, 
    LockOutlined, 
    Visibility, 
    VisibilityOff,
    ArrowForward
} from '@mui/icons-material';
import { useNavigate, useLocation } from 'react-router-dom';
import useAuth from '../../hooks/useAuth';

// Validation Schema
const validationSchema = Yup.object({
  email: Yup.string()
    .email('Geçerli bir e-posta adresi giriniz')
    .required('E-posta alanı zorunludur'),
  password: Yup.string()
    .min(6, 'Şifre en az 6 karakter olmalıdır')
    .required('Şifre alanı zorunludur'),
});

const Login = () => {
  const { setAuth } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  
  // Determine default redirect path based on role
  const getDefaultPath = (roles) => {
    if (roles?.includes('HR')) {
      return '/admin/dashboard';
    } else if (roles?.includes('MANAGER')) {
      return '/manager-dashboard';
    } else if (roles?.includes('ACCOUNTANT')) {
      return '/reports';
    }
    return '/dashboard';
  };
  
  const from = location.state?.from?.pathname;

  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);

  // Form handling
  const formik = useFormik({
    initialValues: {
      email: '',
      password: '',
      rememberMe: false,
    },
    validationSchema: validationSchema,
    onSubmit: async (values) => {
        setLoading(true);
        setError('');
        try {
            // Simulated API Delay
            await new Promise(resolve => setTimeout(resolve, 1000));
            
            // Mock Role Logic based on Email
            // Dokümana göre 4 rol: EMPLOYEE, MANAGER (Departman Yöneticisi), HR (Admin yetkisi), ACCOUNTANT
            let roles = ['EMPLOYEE'];
            let name = 'Çalışan';
            const email = values.email.toLowerCase();

            if (email.includes('admin') || email.includes('ik') || email.includes('hr')) {
                // HR rolü Admin yetkisine sahip (dokümana göre)
                roles = ['HR', 'EMPLOYEE'];
                name = 'İK / Sistem Yöneticisi';
            } else if (email.includes('manager') || email.includes('yonetici') || email.includes('müdür')) {
                // Departman Yöneticisi - İş akışı için kritik rol
                roles = ['MANAGER', 'EMPLOYEE'];
                name = 'Departman Yöneticisi';
            } else if (email.includes('muhasebe') || email.includes('accountant') || email.includes('muh')) {
                // Muhasebe - Hibrit Rol: Hem Raporlara hem de Çalışan ekranlarına erişim
                roles = ['ACCOUNTANT', 'EMPLOYEE'];
                name = 'Muhasebe Personeli';
            }

            const accessToken = "mock_token_12345";
            const user = { 
                email: values.email, 
                roles: roles,
                name: name,
                avatar: ''
            };

            setAuth(user, accessToken, values.rememberMe);
            const redirectPath = from || getDefaultPath(roles);
            navigate(redirectPath, { replace: true });
        } catch (err) {
            setError('Giriş yapılamadı. Bilgilerinizi kontrol edin.');
        } finally {
            setLoading(false);
        }
    },
  });

  return (
    <Box 
        sx={{ 
            minHeight: '100vh', 
            width: '100%',
            display: 'flex', 
            alignItems: 'center', 
            justifyContent: 'center',
            position: 'relative',
            background: `
                radial-gradient(circle at 0% 0%, #0d47a1 0%, transparent 50%), 
                radial-gradient(circle at 100% 0%, #1565c0 0%, transparent 50%), 
                radial-gradient(circle at 100% 100%, #1976d2 0%, transparent 50%), 
                radial-gradient(circle at 0% 100%, #0d47a1 0%, transparent 50%),
                linear-gradient(135deg, #0a1929 0%, #001e3c 100%)
            `,
            backgroundSize: 'cover',
            p: 2
        }}
    >
        {/* Animated Particles/Overlay (Subtle noise) */}
        <Box sx={{
            position: 'absolute',
            top: 0, left: 0, right: 0, bottom: 0,
            opacity: 0.05,
            backgroundImage: 'url("data:image/svg+xml,%3Csvg viewBox=\'0 0 200 200\' xmlns=\'http://www.w3.org/2000/svg\'%3E%3Cfilter id=\'noiseFilter\'%3E%3CfeTurbulence type=\'fractalNoise\' baseFrequency=\'0.65\' numOctaves=\'3\' stitchTiles=\'stitch\'/%3E%3C/filter%3E%3Crect width=\'100%25\' height=\'100%25\' filter=\'url(%23noiseFilter)\'/%3E%3C/svg%3E")',
            pointerEvents: 'none'
        }} />

        <Fade in={true} timeout={1000}>
            <Paper
                elevation={24}
                sx={{
                    width: '100%',
                    maxWidth: 450,
                    p: { xs: 4, md: 5 },
                    borderRadius: 5,
                    backdropFilter: 'blur(20px)',
                    backgroundColor: 'rgba(255, 255, 255, 0.95)',
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'center',
                    boxShadow: '0 20px 50px rgba(0,0,0,0.3)',
                    border: '1px solid rgba(255,255,255,0.5)'
                }}
            >
                {/* Logo Area */}
                <Box sx={{ mb: 4, textAlign: 'center' }}>
                     <Box 
                        sx={{ 
                            width: 60, 
                            height: 60, 
                            bgcolor: 'primary.main', 
                            borderRadius: 3, 
                            display: 'flex', 
                            alignItems: 'center', 
                            justifyContent: 'center',
                            mx: 'auto',
                            mb: 2,
                            boxShadow: '0 8px 16px rgba(13, 71, 161, 0.3)',
                            transform: 'rotate(-5deg)'
                        }}
                    >
                        <LockOutlined sx={{ color: 'white', fontSize: 32 }} />
                    </Box>
                    <Typography variant="h4" fontWeight="800" sx={{ color: '#0f172a', letterSpacing: '-0.5px' }}>
                        ÇözümTR
                    </Typography>
                    <Typography variant="body2" sx={{ color: 'text.secondary', fontWeight: 500, mt: 0.5 }}>
                        Payment Systems
                    </Typography>
                </Box>

                {error && (
                    <Alert severity="error" sx={{ width: '100%', mb: 3, borderRadius: 2 }}>
                        {error}
                    </Alert>
                )}

                <Box component="form" onSubmit={formik.handleSubmit} sx={{ width: '100%' }}>
                    <Box sx={{ mb: 3 }}>
                         <Typography variant="caption" sx={{ color: 'text.secondary', fontWeight: 600, ml: 1, mb: 0.5, display: 'block' }}>
                            E-Posta
                        </Typography>
                        <TextField
                            fullWidth
                            id="email"
                            name="email"
                            placeholder="ornek@cozumtr.com"
                            value={formik.values.email}
                            onChange={formik.handleChange}
                            onBlur={formik.handleBlur}
                            error={formik.touched.email && Boolean(formik.errors.email)}
                            helperText={formik.touched.email && formik.errors.email}
                            InputProps={{
                                startAdornment: (
                                    <InputAdornment position="start">
                                        <EmailOutlined color="action" />
                                    </InputAdornment>
                                ),
                                sx: { borderRadius: 3, bgcolor: '#f8fafc' }
                            }}
                            variant="outlined"
                        />
                    </Box>

                    <Box sx={{ mb: 3 }}>
                        <Typography variant="caption" sx={{ color: 'text.secondary', fontWeight: 600, ml: 1, mb: 0.5, display: 'block' }}>
                            Şifre
                        </Typography>
                        <TextField
                            fullWidth
                            name="password"
                            type={showPassword ? 'text' : 'password'}
                            placeholder="••••••"
                            value={formik.values.password}
                            onChange={formik.handleChange}
                            onBlur={formik.handleBlur}
                            error={formik.touched.password && Boolean(formik.errors.password)}
                            helperText={formik.touched.password && formik.errors.password}
                            InputProps={{
                                startAdornment: (
                                    <InputAdornment position="start">
                                        <LockOutlined color="action" />
                                    </InputAdornment>
                                ),
                                endAdornment: (
                                    <InputAdornment position="end">
                                        <IconButton
                                            onClick={() => setShowPassword(!showPassword)}
                                            edge="end"
                                        >
                                            {showPassword ? <VisibilityOff /> : <Visibility />}
                                        </IconButton>
                                    </InputAdornment>
                                ),
                                sx: { borderRadius: 3, bgcolor: '#f8fafc' }
                            }}
                            variant="outlined"
                        />
                    </Box>

                    <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
                        <FormControlLabel
                            control={
                                <Checkbox 
                                    name="rememberMe"
                                    color="primary" 
                                    checked={formik.values.rememberMe}
                                    onChange={formik.handleChange}
                                    sx={{ '& .MuiSvgIcon-root': { fontSize: 20 } }}
                                />
                            }
                            label={<Typography variant="body2" color="text.secondary">Beni Hatırla</Typography>}
                        />
                        <Link href="/forgot-password" variant="body2" underline="none" fontWeight="600">
                            Şifremi Unuttum?
                        </Link>
                    </Box>

                    <Button
                        type="submit"
                        fullWidth
                        variant="contained"
                        size="large"
                        endIcon={!loading && <ArrowForward />}
                        disabled={loading}
                        sx={{
                            py: 1.5,
                            borderRadius: 3,
                            fontWeight: 700,
                            textTransform: 'none',
                            fontSize: '1rem',
                            boxShadow: '0 4px 14px 0 rgba(13, 71, 161, 0.4)',
                            background: 'linear-gradient(45deg, #0d47a1 30%, #1976d2 90%)',
                            '&:hover': {
                                boxShadow: '0 6px 20px rgba(13, 71, 161, 0.6)',
                                background: 'linear-gradient(45deg, #0d47a1 30%, #1976d2 90%)',
                                transform: 'scale(1.02)'
                            },
                             transition: 'all 0.2s ease-in-out'
                        }}
                    >
                        {loading ? <CircularProgress size={24} color="inherit" /> : 'Giriş Yap'}
                    </Button>
                </Box>
                
                {/* Simplified Demo Info */}
                 <Box sx={{ mt: 4, textAlign: 'center', opacity: 0.7 }}>
                    <Typography variant="caption" sx={{ display: 'block', mb: 1, fontWeight: 600 }}>
                        DEMO ERİŞİM - Şifre: 123456
                    </Typography>
                    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1, alignItems: 'center' }}>
                        <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap', justifyContent: 'center' }}>
                            <Typography variant="caption" fontWeight="bold" color="error">
                                HR/Admin: admin@ / ik@ / hr@
                            </Typography>
                            <Typography variant="caption" fontWeight="bold" color="warning.main">
                                Yönetici: manager@ / yonetici@
                            </Typography>
                        </Box>
                        <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap', justifyContent: 'center' }}>
                            <Typography variant="caption" fontWeight="bold" color="info.main">
                                Çalışan: calisan@ / employee@ / ahmet@
                            </Typography>
                            <Typography variant="caption" fontWeight="bold" color="success.main">
                                Muhasebe: muhasebe@ / accountant@
                            </Typography>
                        </Box>
                    </Box>
                </Box>
            </Paper>
        </Fade>
    </Box>
  );
};

export default Login;
