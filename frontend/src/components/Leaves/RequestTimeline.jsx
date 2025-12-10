import React from 'react';
import { 
    Box, 
    Stepper, 
    Step, 
    StepLabel, 
    Typography, 
    StepContent 
} from '@mui/material';
import { 
    CheckCircle as CheckCircleIcon,
    HourglassEmpty as HourglassEmptyIcon,
    Cancel as CancelIcon,
    RadioButtonUnchecked as RadioButtonUncheckedIcon
} from '@mui/icons-material';

const RequestTimeline = ({ request }) => {
    // Determine active step based on status
    let activeStep = 0;
    let isError = false;

    // Default steps
    const steps = [
        { label: 'Talep Oluşturuldu', date: request?.createdAt || 'Tarih Yok' },
        { label: 'Yönetici Onayı', date: null },
        { label: 'İK Onayı', date: null },
        { label: 'Tamamlandı', date: null }
    ];

    if (request?.status === 'PENDING') {
        activeStep = 1; // Waiting for Manager
        steps[1].date = 'Bekleniyor...';
    } else if (request?.status === 'APPROVED') {
        activeStep = 3; // Completed
        steps[1].date = 'Onaylandı';
        steps[2].date = 'Onaylandı';
        steps[3].date = 'İşlem Tamam';
    } else if (request?.status === 'REJECTED') {
        activeStep = 1; 
        isError = true;
        steps[1].label = 'Reddedildi';
        steps[1].date = request.rejectionReason || 'Gerekçe belirtilmedi';
    }

    return (
        <Box sx={{ width: '100%', mt: 2 }}>
            <Stepper activeStep={activeStep} orientation="vertical">
                {steps.map((step, index) => {
                    const isStepFailed = isError && index === activeStep;
                    
                    return (
                        <Step key={step.label} expanded={true}>
                            <StepLabel
                                error={isStepFailed}
                                StepIconComponent={() => {
                                    if (isStepFailed) return <CancelIcon color="error" />;
                                    if (index < activeStep || (index === activeStep && request?.status === 'APPROVED')) return <CheckCircleIcon color="success" />;
                                    if (index === activeStep) return <HourglassEmptyIcon color="warning" />;
                                    return <RadioButtonUncheckedIcon color="disabled" />;
                                }}
                            >
                                <Typography variant="subtitle2" fontWeight="bold">{step.label}</Typography>
                            </StepLabel>
                            <StepContent>
                                <Typography variant="body2" color="text.secondary">{step.date}</Typography>
                            </StepContent>
                        </Step>
                    );
                })}
            </Stepper>
        </Box>
    );
};

export default RequestTimeline;
