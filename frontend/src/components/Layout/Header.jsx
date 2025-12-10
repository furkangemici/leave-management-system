import React, { useState } from 'react';
import { 
    AppBar, 
    Toolbar, 
    Typography, 
    IconButton, 
    Box, 
    Avatar, 
    Badge, 
    Menu, 
    MenuItem, 
    ListItemIcon,
    Divider
} from '@mui/material';
import { 
    Menu as MenuIcon, 
    Logout as LogoutIcon, 
    Notifications as NotificationsIcon,
    Person as PersonIcon,
    Settings as SettingsIcon
} from '@mui/icons-material';
import useAuth from '../../hooks/useAuth';
import { useNavigate } from 'react-router-dom';

const Header = ({ handleDrawerToggle, drawerWidth }) => {
    const { auth, logout } = useAuth();
    const navigate = useNavigate();

    // Menu States
    const [anchorElUser, setAnchorElUser] = useState(null);
    const [anchorElNotif, setAnchorElNotif] = useState(null);

    // Mock Notifications
    const notifications = [
        { id: 1, text: 'Ahmet Yılmaz yeni izin talebi oluşturdu.', time: '5 dk önce' },
        { id: 2, text: 'Yıllık izin talebiniz onaylandı.', time: '1 saat önce' },
        { id: 3, text: 'Sistem bakımı hatırlatması.', time: '2 gün önce' }
    ];

    const handleOpenUserMenu = (event) => setAnchorElUser(event.currentTarget);
    const handleCloseUserMenu = () => setAnchorElUser(null);
    const handleOpenNotifMenu = (event) => setAnchorElNotif(event.currentTarget);
    const handleCloseNotifMenu = () => setAnchorElNotif(null);

    const handleLogout = () => {
        handleCloseUserMenu();
        logout();
        navigate('/login');
    };

    const handleProfile = () => {
        handleCloseUserMenu();
        navigate('/profile');
    };

    return (
        <AppBar
            position="fixed"
            sx={{
                width: { sm: `calc(100% - ${drawerWidth}px)` },
                ml: { sm: `${drawerWidth}px` },
                bgcolor: 'background.paper',
                color: 'text.primary',
                boxShadow: 1
            }}
        >
            <Toolbar>
                <IconButton
                    color="inherit"
                    aria-label="open drawer"
                    edge="start"
                    onClick={handleDrawerToggle}
                    sx={{ mr: 2, display: { sm: 'none' } }}
                >
                    <MenuIcon />
                </IconButton>
                <Typography variant="h6" noWrap component="div" sx={{ flexGrow: 1, fontWeight: 800, color: 'primary.main', letterSpacing: '-0.5px' }}>
                    Entegre İzin Platformu
                </Typography>
                
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    {/* Notifications */}
                    <IconButton color="inherit" onClick={handleOpenNotifMenu}>
                        <Badge badgeContent={notifications.length} color="error">
                            <NotificationsIcon />
                        </Badge>
                    </IconButton>
                    <Menu
                        sx={{ mt: '45px' }}
                        id="menu-appbar-notif"
                        anchorEl={anchorElNotif}
                        anchorOrigin={{ vertical: 'top', horizontal: 'right' }}
                        keepMounted
                        transformOrigin={{ vertical: 'top', horizontal: 'right' }}
                        open={Boolean(anchorElNotif)}
                        onClose={handleCloseNotifMenu}
                    >
                        {notifications.map((notif) => (
                            <MenuItem key={notif.id} onClick={handleCloseNotifMenu}>
                                <Box sx={{ display: 'flex', flexDirection: 'column' }}>
                                    <Typography variant="body2" sx={{ fontWeight: 600 }}>{notif.text}</Typography>
                                    <Typography variant="caption" color="text.secondary">{notif.time}</Typography>
                                </Box>
                            </MenuItem>
                        ))}
                    </Menu>

                    {/* User Profile */}
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, cursor: 'pointer' }} onClick={handleOpenUserMenu}>
                        <Box sx={{ display: { xs: 'none', sm: 'block' }, textAlign: 'right' }}>
                            <Typography variant="subtitle2" sx={{ fontWeight: 700, lineHeight: 1.2 }}>
                                {auth?.user?.name || "Kullanıcı"}
                            </Typography>
                            <Typography variant="caption" color="text.secondary" sx={{ display: 'block' }}>
                                {auth?.user?.roles?.[0] || "Employee"}
                            </Typography>
                        </Box>
                        <Avatar sx={{ bgcolor: 'primary.main', width: 40, height: 40, fontSize: '1rem', fontWeight: 'bold' }}>
                            {(auth?.user?.name?.charAt(0) || "U")}
                        </Avatar>
                    </Box>
                    <Menu
                        sx={{ mt: '45px' }}
                        id="menu-appbar-user"
                        anchorEl={anchorElUser}
                        anchorOrigin={{ vertical: 'top', horizontal: 'right' }}
                        keepMounted
                        transformOrigin={{ vertical: 'top', horizontal: 'right' }}
                        open={Boolean(anchorElUser)}
                        onClose={handleCloseUserMenu}
                    >
                        <MenuItem onClick={handleProfile}>
                            <ListItemIcon><PersonIcon fontSize="small" /></ListItemIcon>
                            Profilim
                        </MenuItem>
                        {/* <MenuItem onClick={handleCloseUserMenu}>
                            <ListItemIcon><SettingsIcon fontSize="small" /></ListItemIcon>
                            Ayarlar
                        </MenuItem> */}
                        <Divider />
                        <MenuItem onClick={handleLogout}>
                            <ListItemIcon><LogoutIcon fontSize="small" color="error" /></ListItemIcon>
                            <Typography color="error">Çıkış Yap</Typography>
                        </MenuItem>
                    </Menu>
                </Box>
            </Toolbar>
        </AppBar>
    );
};

export default Header;
