import React from 'react';
import { Breadcrumbs as MUIBreadcrumbs, Typography, Link, Box } from '@mui/material';
import { Link as RouterLink, useLocation } from 'react-router-dom';
import { NavigateNext as NavigateNextIcon } from '@mui/icons-material';

const routeNameMap = {
    'dashboard': 'Ana Sayfa',
    'my-leaves': 'İzinlerim',
    'new-request': 'İzin Talep Et',
    'approvals': 'Onay Merkezi',
    'team-calendar': 'Ekip Takvimi',
    'reports': 'Raporlar',
    'admin': 'Yönetim Paneli',
    'users': 'Kullanıcı Yönetimi',
    'departments': 'Departman Yönetimi',
    'leave-types': 'İzin Türleri',
    'workflow': 'İş Akışı',
    'holidays': 'Resmi Tatiller',
    'profile': 'Profilim'
};

const Breadcrumbs = () => {
    const location = useLocation();
    const pathnames = location.pathname.split('/').filter((x) => x);

    // Don't show on Dashboard root
    if (location.pathname === '/' || location.pathname === '/dashboard') {
        return null;
    }

    return (
        <Box sx={{ mb: 3 }}>
            <MUIBreadcrumbs 
                separator={<NavigateNextIcon fontSize="small" />} 
                aria-label="breadcrumb"
            >
                <Link component={RouterLink} to="/dashboard" underline="hover" color="inherit">
                    Ana Sayfa
                </Link>
                {pathnames.map((value, index) => {
                    const last = index === pathnames.length - 1;
                    const to = `/${pathnames.slice(0, index + 1).join('/')}`;
                    const displayName = routeNameMap[value] || value;

                    return last ? (
                        <Typography color="text.primary" key={to} fontWeight="bold">
                            {displayName}
                        </Typography>
                    ) : (
                        <Link component={RouterLink} to={to} underline="hover" color="inherit" key={to}>
                            {displayName}
                        </Link>
                    );
                })}
            </MUIBreadcrumbs>
        </Box>
    );
};

export default Breadcrumbs;
