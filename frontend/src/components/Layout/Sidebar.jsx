import React, { useState } from 'react';
import { 
    Drawer, 
    List, 
    ListItem, 
    ListItemButton, 
    ListItemIcon, 
    ListItemText, 
    Toolbar, 
    Divider,
    Box,
    Collapse
} from '@mui/material';
import { ExpandLess, ExpandMore } from '@mui/icons-material';
import { 
    Dashboard as DashboardIcon, 
    AddBox as AddBoxIcon, 
    CheckCircle as CheckCircleIcon, 
    CalendarMonth as CalendarMonthIcon, 
    Settings as SettingsIcon,
    History as HistoryIcon,
    Assessment as AssessmentIcon
} from '@mui/icons-material';
import { useNavigate, useLocation } from 'react-router-dom';
import useAuth from '../../hooks/useAuth';

const drawerWidth = 240;

const Sidebar = ({ mobileOpen, handleDrawerToggle, window }) => {
    const { auth } = useAuth();
    const navigate = useNavigate();
    const location = useLocation();
    const [openSubMenus, setOpenSubMenus] = useState({});

    // Roles
    // Assuming backend sends roles as an array of strings e.g., ['EMPLOYEE', 'MANAGER', 'ADMIN']
    // If backend sends a single string, wrap it in array: [auth.user.role]
    const userRoles = auth?.user?.roles || [];

    const handleSubMenuToggle = (menuText) => {
        setOpenSubMenus(prev => ({
            ...prev,
            [menuText]: !prev[menuText]
        }));
    }; 

    const menuItems = [
        {
            text: 'Dashboard',
            icon: <DashboardIcon />,
            path: userRoles.includes('HR') ? '/admin/dashboard' : 
                  userRoles.includes('MANAGER') ? '/manager-dashboard' : '/dashboard',
            roles: ['EMPLOYEE', 'MANAGER', 'HR', 'ACCOUNTANT']
        },
        {
            text: 'İzin Talep Et',
            icon: <AddBoxIcon />,
            path: '/new-request',
            roles: ['EMPLOYEE', 'MANAGER', 'HR']
        },
        {
            text: 'İzinlerim',
            icon: <HistoryIcon />,
            path: '/my-leaves',
            roles: ['EMPLOYEE', 'MANAGER', 'HR']
        },
        {
            text: 'Onay Bekleyenler',
            icon: <CheckCircleIcon />,
            path: '/approvals',
            roles: ['MANAGER', 'HR']
        },
        {
            text: 'Ekip Takvimi',
            icon: <CalendarMonthIcon />,
            path: '/team-calendar',
            roles: ['EMPLOYEE', 'MANAGER', 'HR']
        },
        {
             text: 'Raporlar',
             icon: <AssessmentIcon />,
             path: '/reports',
             roles: ['HR', 'ACCOUNTANT']
        },
        {
            text: 'Yönetim Paneli',
            icon: <SettingsIcon />,
            path: '/admin/dashboard',
            roles: ['HR'],
            hasSubMenu: true,
            subItems: [
                { text: 'Admin Dashboard', path: '/admin/dashboard', roles: ['HR'] },
                { text: 'Kullanıcı Yönetimi', path: '/admin/users', roles: ['HR'] },
                { text: 'Departman Yönetimi', path: '/admin/departments', roles: ['HR'] },
                { text: 'İzin Türleri', path: '/admin/leave-types', roles: ['HR'] },
                { text: 'İş Akışı', path: '/admin/workflow', roles: ['HR'] },
                { text: 'Resmi Tatiller', path: '/admin/holidays', roles: ['HR'] }
            ]
        }
    ];

    const hasRole = (allowedRoles) => {
        // If no roles defined, open to all? Or strict?
        // Assuming user must have at least one of the allowed roles
        return allowedRoles.some(role => userRoles.includes(role));
    };

    const drawerContent = (
        <div>
            <Toolbar sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center' }}>
                 {/* Logo or App Name */}
                 <Box component="span" sx={{ fontWeight: 900, fontSize: '1.5rem', color: 'primary.main', letterSpacing: '-1px' }}>ÇözümTR</Box>
            </Toolbar>
            <Divider />
            <List>
                {menuItems.map((item) => {
                     if (!hasRole(item.roles)) return null;

                     const isSubMenuOpen = openSubMenus[item.text] || false;
                     const hasActiveSubItem = item.subItems?.some(subItem => 
                         location.pathname === subItem.path && hasRole(subItem.roles)
                     ) || false;

                     return (
                        <React.Fragment key={item.text}>
                            <ListItem disablePadding>
                                <ListItemButton 
                                    selected={location.pathname === item.path || hasActiveSubItem}
                                    onClick={() => {
                                        if (item.hasSubMenu) {
                                            handleSubMenuToggle(item.text);
                                        } else {
                                            navigate(item.path);
                                            if (mobileOpen) handleDrawerToggle();
                                        }
                                    }}
                                >
                                    <ListItemIcon>
                                        {item.icon}
                                    </ListItemIcon>
                                    <ListItemText primary={item.text} />
                                    {item.hasSubMenu && (isSubMenuOpen ? <ExpandLess /> : <ExpandMore />)}
                                </ListItemButton>
                            </ListItem>
                            {item.hasSubMenu && item.subItems && (
                                <Collapse in={isSubMenuOpen} timeout="auto" unmountOnExit>
                                    <List component="div" disablePadding>
                                        {item.subItems.map((subItem) => {
                                            if (!hasRole(subItem.roles)) return null;
                                            return (
                                                <ListItemButton
                                                    key={subItem.text}
                                                    sx={{ pl: 4 }}
                                                    selected={location.pathname === subItem.path}
                                                    onClick={() => {
                                                        navigate(subItem.path);
                                                        if (mobileOpen) handleDrawerToggle();
                                                    }}
                                                >
                                                    <ListItemText 
                                                        primary={subItem.text}
                                                        primaryTypographyProps={{
                                                            fontSize: '0.9rem'
                                                        }}
                                                    />
                                                </ListItemButton>
                                            );
                                        })}
                                    </List>
                                </Collapse>
                            )}
                        </React.Fragment>
                     );
                })}
            </List>
        </div>
    );

    const container = window !== undefined ? () => window().document.body : undefined;

    return (
        <Box
            component="nav"
            sx={{ width: { sm: drawerWidth }, flexShrink: { sm: 0 } }}
            aria-label="mailbox folders"
        >
            {/* Mobile Drawer (Temporary) */}
            <Drawer
                container={container}
                variant="temporary"
                open={mobileOpen}
                onClose={handleDrawerToggle}
                ModalProps={{
                    keepMounted: true, // Better open performance on mobile.
                }}
                sx={{
                    display: { xs: 'block', sm: 'none' },
                    '& .MuiDrawer-paper': { boxSizing: 'border-box', width: drawerWidth },
                }}
            >
                {drawerContent}
            </Drawer>
            
            {/* Desktop Drawer (Permanent) */}
            <Drawer
                variant="permanent"
                sx={{
                    display: { xs: 'none', sm: 'block' },
                    '& .MuiDrawer-paper': { boxSizing: 'border-box', width: drawerWidth },
                }}
                open
            >
                {drawerContent}
            </Drawer>
        </Box>
    );
};

export default Sidebar;
