import React, { useState } from 'react';
import { Outlet } from 'react-router-dom';
import { Box, CssBaseline, Toolbar } from '@mui/material';
import Sidebar from './Sidebar';
import Header from './Header';
import Breadcrumbs from './Breadcrumbs';

const drawerWidth = 240;

const MainLayout = () => {
    const [mobileOpen, setMobileOpen] = useState(false);

    const handleDrawerToggle = () => {
        setMobileOpen(!mobileOpen);
    };

    return (
        <Box sx={{ display: 'flex' }}>
            <CssBaseline />
            
            <Header 
                handleDrawerToggle={handleDrawerToggle} 
                drawerWidth={drawerWidth} 
            />
            
            <Sidebar 
                mobileOpen={mobileOpen} 
                handleDrawerToggle={handleDrawerToggle} 
                drawerWidth={drawerWidth} // Pass width to sidebar if needed for calculations, though css handles it
            />
            
            <Box
                component="main"
                sx={{ flexGrow: 1, p: 3, width: { sm: `calc(100% - ${drawerWidth}px)` } }}
            >
                <Toolbar /> {/* Spacer for keeping content below AppBar */}
                <Breadcrumbs />
                <Outlet />
            </Box>
        </Box>
    );
};

export default MainLayout;
