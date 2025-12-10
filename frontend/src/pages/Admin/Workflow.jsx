import React, { useState } from 'react';
import { 
    Box, 
    Typography, 
    Paper, 
    Grid, 
    Card, 
    CardContent, 
    Switch, 
    FormControlLabel, 
    Divider,
    FormControl,
    InputLabel,
    Select,
    MenuItem,
    Chip,
    Button,
    Dialog,
    DialogTitle,
    DialogContent,
    DialogActions,
    TextField,
    Alert,
    IconButton
} from '@mui/material';
import { 
    AccountTree as WorkflowIcon, 
    SupervisorAccount,
    ImportContacts, 
    VerifiedUser,
    Add as AddIcon,
    Delete as DeleteIcon,
    Edit as EditIcon
} from '@mui/icons-material';

// Mock Data
const mockLeaveTypes = [
    { id: 1, name: 'Yıllık İzin' },
    { id: 2, name: 'Mazeret İzni' },
    { id: 3, name: 'Hastalık İzni' },
    { id: 4, name: 'Ücretsiz İzin' }
];

const approvalStepTypes = [
    { value: 'DEPARTMENT_MANAGER', label: 'Departman Yöneticisi', icon: SupervisorAccount },
    { value: 'HR', label: 'İnsan Kaynakları (Admin Yetkisi)', icon: ImportContacts },
    { value: 'GENERAL_MANAGER', label: 'Genel Müdür', icon: VerifiedUser }
];

const initialWorkflows = {
    1: [ // Yıllık İzin
        { id: 1, type: 'DEPARTMENT_MANAGER', order: 1, required: true, active: true },
        { id: 2, type: 'HR', order: 2, required: true, active: true }
    ],
    2: [ // Mazeret İzni
        { id: 3, type: 'DEPARTMENT_MANAGER', order: 1, required: true, active: true }
    ],
    3: [ // Hastalık İzni
        { id: 4, type: 'DEPARTMENT_MANAGER', order: 1, required: true, active: true },
        { id: 5, type: 'HR', order: 2, required: false, active: true }
    ]
};

const Workflow = () => {
    console.log('Workflow component rendering...');
    const [selectedLeaveType, setSelectedLeaveType] = useState(1);
    const [workflows, setWorkflows] = useState(initialWorkflows);
    
    // Dialog States
    const [editDialogOpen, setEditDialogOpen] = useState(false);
    const [editMode, setEditMode] = useState(false);
    
    // Form States (for Dialog)
    const [formStepId, setFormStepId] = useState(null);
    const [formStepType, setFormStepType] = useState('');
    const [formRequired, setFormRequired] = useState(false);
    const [formActive, setFormActive] = useState(true);

    const currentWorkflow = workflows[selectedLeaveType] || [];

    // Helper to open dialog
    const handleOpenDialog = (step = null) => {
        if (step) {
            // Edit Mode
            setEditMode(true);
            setFormStepId(step.id);
            setFormStepType(step.type);
            setFormRequired(step.required);
            setFormActive(step.active);
        } else {
            // Add Mode
            setEditMode(false);
            setFormStepId(null);
            setFormStepType('');
            setFormRequired(true); // Default to required
            setFormActive(true);
        }
        setEditDialogOpen(true);
    };

    const handleCloseDialog = () => {
        setEditDialogOpen(false);
        // Reset form
        setFormStepId(null);
        setFormStepType('');
    };

    const handleSaveStep = () => {
        if (!formStepType) return;

        setWorkflows(prev => {
            const currentSteps = prev[selectedLeaveType] || [];
            let updatedSteps;

            if (editMode) {
                // Update existing
                updatedSteps = currentSteps.map(step => 
                    step.id === formStepId 
                        ? { ...step, type: formStepType, required: formRequired, active: formActive } 
                        : step
                );
            } else {
                // Create new
                const newId = Math.max(...currentSteps.map(s => s.id), 0) + 1;
                const newOrder = currentSteps.length + 1;
                updatedSteps = [...currentSteps, {
                    id: newId,
                    type: formStepType,
                    order: newOrder,
                    required: formRequired,
                    active: formActive
                }];
            }

            return {
                ...prev,
                [selectedLeaveType]: updatedSteps
            };
        });
        handleCloseDialog();
    };

    const handleDeleteStep = (stepId) => {
        if(window.confirm('Bu onay adımını silmek istediğinize emin misiniz?')) {
            setWorkflows(prev => ({
                ...prev,
                [selectedLeaveType]: prev[selectedLeaveType].filter(step => step.id !== stepId)
            }));
        }
    };

    const getStepInfo = (stepType) => {
        return approvalStepTypes.find(t => t.value === stepType) || approvalStepTypes[0];
    };

    return (
        <Box sx={{ width: '100%' }}>
            <Box sx={{ mb: 4 }}>
                <Typography variant="h4" sx={{ mb: 1, fontWeight: 'bold' }}>
                    Onay Akış Yönetimi
                </Typography>
                <Typography variant="body1" sx={{ color: 'text.secondary' }}>
                    İzin taleplerinin hangi onay aşamalarından geçeceğini buradan yapılandırabilirsiniz.
                </Typography>
            </Box>

            {/* Leave Type Selector */}
            <Paper sx={{ p: 3, mb: 3, borderRadius: 3 }}>
                <FormControl fullWidth>
                    <InputLabel>İzin Türü Seçiniz</InputLabel>
                    <Select
                        value={selectedLeaveType}
                        label="İzin Türü Seçiniz"
                        onChange={(e) => setSelectedLeaveType(e.target.value)}
                    >
                        {mockLeaveTypes.map((type) => (
                            <MenuItem key={type.id} value={type.id}>
                                {type.name}
                            </MenuItem>
                        ))}
                    </Select>
                </FormControl>
            </Paper>

            {/* Workflow Steps Grid */}
            <Grid container spacing={3}>
                {currentWorkflow.map((step, index) => {
                    const stepInfo = getStepInfo(step.type);
                    const IconComponent = stepInfo.icon;
                    return (
                        <Grid item xs={12} md={4} key={step.id}>
                            <Card 
                                sx={{ 
                                    height: '100%', 
                                    borderTop: `4px solid ${step.active ? '#3b82f6' : '#9ca3af'}`,
                                    opacity: step.active ? 1 : 0.6,
                                    position: 'relative'
                                }}
                            >
                                <CardContent>
                                    <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 2 }}>
                                        <Box sx={{ display: 'flex', alignItems: 'center' }}>
                                            <IconComponent 
                                                color={step.active ? 'primary' : 'disabled'} 
                                                sx={{ fontSize: 40, mr: 2 }} 
                                            />
                                            <Box>
                                                <Typography variant="h6">
                                                    {index + 1}. {stepInfo.label}
                                                </Typography>
                                                <Chip 
                                                    label={step.required ? 'Zorunlu' : 'Opsiyonel'} 
                                                    size="small" 
                                                    color={step.required ? 'error' : 'default'}
                                                    sx={{ mt: 0.5 }}
                                                />
                                            </Box>
                                        </Box>
                                        <Box>
                                            <IconButton 
                                                size="small" 
                                                color="primary"
                                                onClick={() => handleOpenDialog(step)}
                                            >
                                                <EditIcon fontSize="small" />
                                            </IconButton>
                                            <IconButton 
                                                size="small" 
                                                color="error"
                                                onClick={() => handleDeleteStep(step.id)}
                                            >
                                                <DeleteIcon fontSize="small" />
                                            </IconButton>
                                        </Box>
                                    </Box>
                                    <Divider sx={{ my: 2 }} />
                                    <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                        <Typography variant="body2" color="text.secondary">Adım Durumu:</Typography>
                                         <Chip 
                                            label={step.active ? 'Aktif' : 'Pasif'} 
                                            color={step.active ? 'success' : 'default'} 
                                            size="small"
                                            variant="outlined"
                                        />
                                    </Box>
                                </CardContent>
                            </Card>
                        </Grid>
                    );
                })}

                {/* Add Step Button */}
                <Grid item xs={12} md={4}>
                    <Card 
                        sx={{ 
                            height: '100%', 
                            border: '2px dashed',
                            borderColor: 'divider',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            cursor: 'pointer',
                            minHeight: 180,
                            '&:hover': {
                                borderColor: 'primary.main',
                                bgcolor: 'action.hover'
                            }
                        }}
                        onClick={() => handleOpenDialog(null)}
                    >
                        <CardContent sx={{ textAlign: 'center' }}>
                            <AddIcon sx={{ fontSize: 48, color: 'text.secondary', mb: 1 }} />
                            <Typography variant="body1" color="text.secondary">
                                Yeni Onay Adımı Ekle
                            </Typography>
                        </CardContent>
                    </Card>
                </Grid>
            </Grid>

            {/* Add/Edit Dialog */}
            <Dialog open={editDialogOpen} onClose={handleCloseDialog} maxWidth="xs" fullWidth>
                <DialogTitle>
                    {editMode ? 'Onay Adımını Düzenle' : 'Yeni Onay Adımı Ekle'}
                </DialogTitle>
                <DialogContent>
                    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3, mt: 2 }}>
                        <FormControl fullWidth>
                            <InputLabel>Rol / Onay Merci</InputLabel>
                            <Select
                                value={formStepType}
                                label="Rol / Onay Merci"
                                onChange={(e) => setFormStepType(e.target.value)}
                            >
                                {approvalStepTypes.map((type) => (
                                    <MenuItem 
                                        key={type.value} 
                                        value={type.value}
                                        disabled={!editMode && currentWorkflow.some(s => s.type === type.value)}
                                    >
                                        {type.label}
                                    </MenuItem>
                                ))}
                            </Select>
                        </FormControl>

                        <FormControlLabel 
                            control={
                                <Switch 
                                    checked={formRequired} 
                                    onChange={(e) => setFormRequired(e.target.checked)} 
                                    color="error"
                                />
                            } 
                            label="Bu adım zorunludur (Atlanamaz)" 
                        />

                        <FormControlLabel 
                             control={
                                <Switch 
                                    checked={formActive} 
                                    onChange={(e) => setFormActive(e.target.checked)} 
                                    color="primary"
                                />
                            } 
                            label="Bu adım şu anda aktif" 
                        />
                    </Box>
                </DialogContent>
                <DialogActions>
                    <Button onClick={handleCloseDialog}>İptal</Button>
                    <Button onClick={handleSaveStep} variant="contained" disabled={!formStepType}>
                        Kaydet
                    </Button>
                </DialogActions>
            </Dialog>
        </Box>
    );
};

export default Workflow;
