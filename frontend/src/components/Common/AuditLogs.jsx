import React from 'react';
import { 
    Box, 
    Typography, 
    Stepper, 
    Step, 
    StepLabel, 
    StepContent, 
    Paper 
} from '@mui/material';
import { 
    Circle as CircleIcon, 
    CheckCircle as CheckCircleIcon,
    Cancel as CancelIcon,
    Pending as PendingIcon
} from '@mui/icons-material';

// Mock Log Data
const mockLogs = [
    {
        label: 'Talep Oluşturuldu',
        description: 'Ahmet Yılmaz tarafından talep sisteme girildi.',
        date: '10.12.2025 09:00',
        author: 'Ahmet Yılmaz',
        status: 'default'
    },
    {
        label: 'Yönetici Onayı',
        description: 'Birim yöneticisi Mehmet Demir tarafından onaylandı.',
        date: '10.12.2025 10:45',
        author: 'Mehmet Demir',
        status: 'success'
    },
    {
        label: 'İK Onayı Bekleniyor',
        description: 'Talep İK departmanına iletildi, son onay bekleniyor.',
        date: '10.12.2025 10:46',
        author: 'Sistem',
        status: 'pending'
    }
];

const AuditLogs = ({ logs = mockLogs }) => {
  return (
    <Box sx={{ maxWidth: 600 }}>
      {/* <Typography variant="h6" fontWeight="bold" sx={{ mb: 2 }}>İşlem Geçmişi</Typography> */}
      <Stepper orientation="vertical" activeStep={logs.length - 1}>
        {logs.map((step, index) => (
          <Step key={step.label} active={true}>
            <StepLabel
                StepIconComponent={() => {
                   if (step.status === 'success') return <CheckCircleIcon color="success" />;
                   if (step.status === 'error') return <CancelIcon color="error" />;
                   if (step.status === 'pending') return <PendingIcon color="warning" />;
                   return <CircleIcon color="disabled" />;
                }}
            >
              <Box sx={{ display: 'flex', justifyContent: 'space-between', width: '100%', alignItems: 'center' }}>
                  <Typography variant="subtitle2" fontWeight="bold">{step.label}</Typography>
                  <Typography variant="caption" color="text.secondary" sx={{ ml: 2 }}>{step.date}</Typography>
              </Box>
            </StepLabel>
            <StepContent>
              <Paper elevation={0} sx={{ p: 2, bgcolor: '#f8fafc', mb: 1, borderRadius: 2 }}>
                  <Typography variant="body2">{step.description}</Typography>
                  <Typography variant="caption" sx={{ display: 'block', mt: 1, fontWeight: 'bold' }}>
                      İşlemi Yapan: {step.author}
                  </Typography>
              </Paper>
            </StepContent>
          </Step>
        ))}
      </Stepper>
    </Box>
  );
};

export default AuditLogs;
