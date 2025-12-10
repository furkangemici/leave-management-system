import React, { useState, useEffect } from 'react';
import { useFormik } from 'formik';
import * as Yup from 'yup';
import {
    Container,
    Grid,
    Paper,
    Typography,
    TextField,
    MenuItem,
    Button,
    Alert,
    Box,
    FormControl,
    InputLabel,
    Select,
    FormHelperText,
    Chip,
    Divider,
    Stack,
    Card,
    CardContent,
    Fade,
    Zoom,
    IconButton,
    Tooltip
} from '@mui/material';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { AdapterDateFns } from '@mui/x-date-pickers/AdapterDateFns';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import { TimePicker } from '@mui/x-date-pickers/TimePicker';
import { differenceInDays, differenceInHours, isBefore, startOfDay, isAfter, eachDayOfInterval, isWeekend } from 'date-fns';
import { useNavigate } from 'react-router-dom';
import { 
    Send as SendIcon, 
    Cancel as CancelIcon, 
    AccountBalanceWallet as WalletIcon,
    CalendarToday as CalendarIcon,
    AccessTime as TimeIcon,
    Description as DescriptionIcon,
    CheckCircle as CheckCircleIcon,
    Warning as WarningIcon,
    Info as InfoIcon
} from '@mui/icons-material';

const mockLeaveTypes = [
    { id: 1, name: 'Yıllık İzin', unit: 'DAILY', balance: 14, color: '#1976d2' },
    { id: 2, name: 'Mazeret İzni', unit: 'HOURLY', balance: 8, color: '#ed6c02' }, // Aylık 8 Saat
    { id: 3, name: 'Hastalık İzni', unit: 'DAILY', balance: 10, color: '#d32f2f' }
];

const NewRequest = () => {
    const navigate = useNavigate();
    // Mock Team Data for Conflict Checking
    const mockTeamLeaves = [
        { name: 'Ahmet Yılmaz', start: '2025-12-10', end: '2025-12-12' },
        { name: 'Ayşe Demir', start: '2025-12-08', end: '2025-12-09' }
    ];

    const [durationPreview, setDurationPreview] = useState(null);
    const [selectedType, setSelectedType] = useState(null);
    const [conflictWarning, setConflictWarning] = useState(null);

    // Validation
    const validationSchema = Yup.object({
        leaveTypeId: Yup.string().required('İzin türü seçimi zorunludur'),
        description: Yup.string().required('Açıklama alanı zorunludur'),
        startDate: Yup.date()
            .required('Başlangıç tarihi zorunludur')
            .nullable()
            .test('is-future', 'Geçmiş bir tarih seçilemez', value => {
                if (!value) return true;
                return isAfter(value, startOfDay(new Date())) || value.getTime() === startOfDay(new Date()).getTime();
            }),
        // Conditional validations
        endDate: Yup.date().nullable().when('leaveTypeId', (leaveTypeId, schema) => {
            const type = mockLeaveTypes.find(t => t.id === parseInt(leaveTypeId));
            if (type?.unit === 'DAILY') {
                return schema
                    .required('Bitiş tarihi zorunludur')
                    .min(Yup.ref('startDate'), 'Bitiş tarihi başlangıç tarihinden önce olamaz');
            }
            return schema;
        }),
        startTime: Yup.date().nullable().when('leaveTypeId', (leaveTypeId, schema) => {
             const type = mockLeaveTypes.find(t => t.id === parseInt(leaveTypeId));
             if (type?.unit === 'HOURLY') {
                 return schema.required('Başlangıç saati zorunludur');
             }
             return schema;
        }),
        endTime: Yup.date().nullable().when('leaveTypeId', (leaveTypeId, schema) => {
            const type = mockLeaveTypes.find(t => t.id === parseInt(leaveTypeId));
            if (type?.unit === 'HOURLY') {
                return schema
                    .required('Bitiş saati zorunludur')
                    .test('is-after-start', 'Bitiş saati başlangıç saatinden sonra olmalıdır', function(value) {
                         const { startTime } = this.parent;
                         return startTime && value && isAfter(value, startTime);
                    });
            }
            return schema;
        })
    });

    const formik = useFormik({
        initialValues: {
            leaveTypeId: '',
            startDate: null,
            endDate: null,
            startTime: null,
            endTime: null,
            description: ''
        },
        validationSchema: validationSchema,
        onSubmit: (values) => {
            // Mock Submit
            console.log('Form Gönderildi:', values);
            alert('İzin talebiniz başarıyla oluşturuldu! (Demo)');
            navigate('/dashboard');
        }
    });

    // Watch for changes to calculate duration and set type
    useEffect(() => {
        const { leaveTypeId, startDate, endDate, startTime, endTime } = formik.values;
        const type = mockLeaveTypes.find(t => t.id === leaveTypeId);
        setSelectedType(type || null);
        setDurationPreview(null);
        setConflictWarning(null);

        if (!type || !startDate) return;

        // Duration Calculation
        if (type.unit === 'DAILY' && endDate) {
             let days;
             // Yıllık İzin (ID: 1) için hafta sonlarını hariç tut
             if (type.id === 1) {
                 const interval = eachDayOfInterval({ start: startDate, end: endDate });
                 const businessDays = interval.filter(date => !isWeekend(date));
                 days = businessDays.length;
                 if (days > 0) setDurationPreview(`${days} İş Günü`);
             } else {
                 days = differenceInDays(endDate, startDate) + 1;
                 if (days > 0) setDurationPreview(`${days} Gün`);
             }
             
             // Check Conflict logic for Daily
             const myStart = startOfDay(startDate);
             const myEnd = startOfDay(endDate);

             const conflicts = mockTeamLeaves.filter(teamLeave => {
                 const teamStart = startOfDay(new Date(teamLeave.start));
                 const teamEnd = startOfDay(new Date(teamLeave.end));
                 
                 // Simple Overlap Check
                 return (myStart <= teamEnd && myEnd >= teamStart);
             });

             if (conflicts.length > 0) {
                 const names = conflicts.map(c => c.name).join(', ');
                 setConflictWarning(`Seçtiğiniz tarihlerde ekibinizden ${names} de izinli.`);
             }

        } else if (type.unit === 'HOURLY' && startTime && endTime) {
             const hours = differenceInHours(endTime, startTime);
             if (hours > 0) setDurationPreview(`${hours} Saat`);
             else setDurationPreview('Hatalı Saat Aralığı');
        }
    }, [formik.values]);

    return (
        <LocalizationProvider dateAdapter={AdapterDateFns}>
            <Box sx={{ flexGrow: 1, py: 3 }}>
                <Container maxWidth="lg">
                    {/* Page Header */}
                    <Box sx={{ mb: 4, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <Box>
                            <Typography variant="h4" fontWeight="bold" color="text.primary">
                                Yeni İzin Talebi
                            </Typography>
                            <Typography variant="subtitle1" color="text.secondary">
                                İzin formunu eksiksiz doldurarak talebinizi oluşturun
                            </Typography>
                        </Box>
                    </Box>

                    <Grid container spacing={4}>
                        {/* LEFT COLUMN: FORM */}
                        <Grid item xs={12} md={8}>
                            <Paper sx={{ p: 4, borderRadius: 3, boxShadow: '0 4px 20px rgba(0,0,0,0.05)' }}>
                                <form onSubmit={formik.handleSubmit}>
                                    <Grid container spacing={3}>
                                        
                                        {/* Leave Type Selection */}
                                        <Grid item xs={12}>
                                            <FormControl fullWidth error={formik.touched.leaveTypeId && Boolean(formik.errors.leaveTypeId)}>
                                                <InputLabel>İzin Türü</InputLabel>
                                                <Select
                                                    name="leaveTypeId"
                                                    value={formik.values.leaveTypeId}
                                                    label="İzin Türü"
                                                    onChange={(e) => {
                                                        formik.handleChange(e);
                                                        formik.setFieldValue('startDate', null);
                                                        formik.setFieldValue('endDate', null);
                                                        formik.setFieldValue('startTime', null);
                                                        formik.setFieldValue('endTime', null);
                                                    }}
                                                >
                                                    {mockLeaveTypes.map((type) => (
                                                        <MenuItem key={type.id} value={type.id}>
                                                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
                                                                <Box sx={{ width: 10, height: 10, borderRadius: '50%', bgcolor: type.color }} />
                                                                {type.name}
                                                            </Box>
                                                        </MenuItem>
                                                    ))}
                                                </Select>
                                                <FormHelperText>{formik.touched.leaveTypeId && formik.errors.leaveTypeId}</FormHelperText>
                                            </FormControl>
                                        </Grid>

                                        {/* Dynamic Fields based on Type */}
                                        {selectedType?.unit === 'DAILY' && (
                                            <>
                                                <Grid item xs={12} md={6}>
                                                    <DatePicker
                                                        label="Başlangıç Tarihi"
                                                        value={formik.values.startDate}
                                                        onChange={(value) => formik.setFieldValue('startDate', value)}
                                                        minDate={new Date()}
                                                        slotProps={{
                                                            textField: { 
                                                                fullWidth: true,
                                                                error: formik.touched.startDate && Boolean(formik.errors.startDate),
                                                                helperText: formik.touched.startDate && formik.errors.startDate
                                                            }
                                                        }}
                                                    />
                                                </Grid>
                                                <Grid item xs={12} md={6}>
                                                    <DatePicker
                                                        label="Bitiş Tarihi"
                                                        value={formik.values.endDate}
                                                        onChange={(value) => formik.setFieldValue('endDate', value)}
                                                        minDate={formik.values.startDate || new Date()}
                                                        slotProps={{
                                                            textField: { 
                                                                fullWidth: true,
                                                                error: formik.touched.endDate && Boolean(formik.errors.endDate),
                                                                helperText: formik.touched.endDate && formik.errors.endDate
                                                            }
                                                        }}
                                                    />
                                                </Grid>
                                            </>
                                        )}

                                        {selectedType?.unit === 'HOURLY' && (
                                             <>
                                                <Grid item xs={12}>
                                                    <DatePicker
                                                        label="İzin Günü"
                                                        value={formik.values.startDate}
                                                        onChange={(value) => formik.setFieldValue('startDate', value)}
                                                        minDate={new Date()}
                                                        slotProps={{
                                                            textField: { 
                                                                fullWidth: true,
                                                                error: formik.touched.startDate && Boolean(formik.errors.startDate),
                                                                helperText: formik.touched.startDate && formik.errors.startDate
                                                            }
                                                        }}
                                                    />
                                                </Grid>
                                                <Grid item xs={12} md={6}>
                                                    <TimePicker
                                                        label="Başlangıç Saati"
                                                        value={formik.values.startTime}
                                                        onChange={(value) => formik.setFieldValue('startTime', value)}
                                                        slotProps={{
                                                            textField: { 
                                                                fullWidth: true,
                                                                error: formik.touched.startTime && Boolean(formik.errors.startTime),
                                                                helperText: formik.touched.startTime && formik.errors.startTime
                                                            }
                                                        }}
                                                        ampm={false}
                                                    />
                                                </Grid>
                                                <Grid item xs={12} md={6}>
                                                    <TimePicker
                                                        label="Bitiş Saati"
                                                        value={formik.values.endTime}
                                                        onChange={(value) => formik.setFieldValue('endTime', value)}
                                                        minTime={formik.values.startTime}
                                                        slotProps={{
                                                            textField: { 
                                                                fullWidth: true,
                                                                error: formik.touched.endTime && Boolean(formik.errors.endTime),
                                                                helperText: formik.touched.endTime && formik.errors.endTime
                                                            }
                                                        }}
                                                        ampm={false}
                                                    />
                                                </Grid>
                                             </>
                                        )}

                                        {/* Description */}
                                        <Grid item xs={12}>
                                            <TextField
                                                fullWidth
                                                name="description"
                                                label="Açıklama / Mazeret"
                                                placeholder="İzin nedeninizi kısaca belirtiniz..."
                                                multiline
                                                rows={4}
                                                value={formik.values.description}
                                                onChange={formik.handleChange}
                                                error={formik.touched.description && Boolean(formik.errors.description)}
                                                helperText={formik.touched.description && formik.errors.description}
                                            />
                                        </Grid>

                                        {/* Warnings */}
                                        {conflictWarning && (
                                            <Grid item xs={12}>
                                                <Alert severity="warning" variant="outlined" sx={{ borderRadius: 2 }}>
                                                    <Typography variant="subtitle2" fontWeight="bold">Çakışma Tespit Edildi</Typography>
                                                    {conflictWarning}
                                                </Alert>
                                            </Grid>
                                        )}

                                        {/* Actions */}
                                        <Grid item xs={12} sx={{ display: 'flex', gap: 2, justifyContent: 'flex-end', mt: 2 }}>
                                            <Button 
                                                variant="text" 
                                                color="inherit" 
                                                onClick={() => navigate('/dashboard')}
                                                sx={{ px: 3 }}
                                            >
                                                İptal
                                            </Button>
                                            <Button 
                                                type="submit" 
                                                variant="contained" 
                                                size="large"
                                                startIcon={<SendIcon />}
                                                disabled={!formik.isValid || formik.isSubmitting}
                                                sx={{ 
                                                    px: 4, 
                                                    borderRadius: 2,
                                                    textTransform: 'none',
                                                    fontSize: '1rem'
                                                }}
                                            >
                                                Talep Oluştur
                                            </Button>
                                        </Grid>
                                    </Grid>
                                </form>
                            </Paper>
                        </Grid>

                        {/* RIGHT COLUMN: INFO & SUMMARY */}
                        <Grid item xs={12} md={4}>
                            <Stack spacing={3}>
                                {/* Balance Card */}
                                <Paper sx={{ p: 3, borderRadius: 3, bgcolor: 'primary.light', color: 'primary.contrastText' }}>
                                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 2 }}>
                                        <WalletIcon />
                                        <Typography variant="h6" fontWeight="bold">İzin Bakiyeniz</Typography>
                                    </Box>
                                    {selectedType ? (
                                        <Box>
                                            <Typography variant="body2" sx={{ opacity: 0.9 }}>
                                                {selectedType.name} için kalan hakkınız:
                                            </Typography>
                                            <Typography variant="h3" fontWeight="bold" sx={{ mt: 1 }}>
                                                {selectedType.balance} <Typography component="span" variant="h6">{selectedType.unit === 'DAILY' ? 'Gün' : 'Saat'}</Typography>
                                            </Typography>
                                        </Box>
                                    ) : (
                                        <Typography variant="body2" sx={{ opacity: 0.9 }}>
                                            Bakiye bilgisini görüntülemek için lütfen bir izin türü seçiniz.
                                        </Typography>
                                    )}
                                </Paper>

                                {/* Duration Summary Card */}
                                {durationPreview && (
                                    <Zoom in={true} timeout={500}>
                                        <Paper sx={{ p: 3, borderRadius: 3, border: '1px solid', borderColor: 'divider' }}>
                                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 1, color: 'text.primary' }}>
                                                <AccessTimeIcon />
                                                <Typography variant="subtitle1" fontWeight="bold">Süre Özeti</Typography>
                                            </Box>
                                            <Divider sx={{ my: 1.5 }} />
                                            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                                <Typography variant="body2" color="text.secondary">Talep Edilen:</Typography>
                                                <Chip label={durationPreview} color="primary" size="small" />
                                            </Box>
                                        </Paper>
                                    </Zoom>
                                )}

                                {/* Info Card */}
                                <Paper sx={{ p: 3, borderRadius: 3, bgcolor: 'background.default' }}>
                                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1.5, color: 'text.secondary' }}>
                                        <InfoIcon fontSize="small" />
                                        <Typography variant="subtitle2" fontWeight="bold">Bilgilendirme</Typography>
                                    </Box>
                                    <Typography variant="body2" color="text.secondary" paragraph>
                                        • Yıllık izinlerde hafta sonları izin süresinden düşülmektedir.
                                    </Typography>
                                    <Typography variant="body2" color="text.secondary">
                                        • Mazeret izinleri saatlik hesaplanır. Aylık limit 8 saattir ve her ay yenilenir.
                                    </Typography>
                                </Paper>
                            </Stack>
                        </Grid>
                    </Grid>
                </Container>
            </Box>
        </LocalizationProvider>
    );
};

// Helper Icon for Summary Card (AccessTime was renamed in imports, need to fix or use TimeIcon)
const AccessTimeIcon = TimeIcon;

export default NewRequest;
