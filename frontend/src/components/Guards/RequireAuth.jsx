import { useLocation, Navigate, Outlet } from 'react-router-dom';
import useAuth from '../../hooks/useAuth';

const RequireAuth = ({ allowedRoles }) => {
    const { auth } = useAuth();
    const location = useLocation();

    // Check if user is authenticated
    if (!auth?.user) {
        return <Navigate to="/login" state={{ from: location }} replace />;
    }

    // Check if user has required role (if allowedRoles is provided)
    // Assuming backend returns user.roles as array OR user.role as string
    if (allowedRoles) {
        const userRoles = auth.user.roles || [auth.user.role];
        const hasRole = userRoles.some(role => allowedRoles.includes(role));
        
        if (!hasRole) {
            // User authorized but not for this specific page -> Redirect to unauthorized or dashboard
            return <Navigate to="/dashboard" replace />; // Or /unauthorized page
        }
    }

    return <Outlet />;
};

export default RequireAuth;
