import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';

// Pages
import Login from './pages/Auth/Login';
import ForgotPassword from './pages/Auth/ForgotPassword';
import ResetPassword from './pages/Auth/ResetPassword';

import Dashboard from './pages/Dashboard/Dashboard';
import ManagerDashboard from './pages/Dashboard/ManagerDashboard'; // Keep if used or remove

import MyLeaves from './pages/Leaves/MyLeaves';
import NewRequest from './pages/Leaves/NewRequest';
import TeamCalendar from './pages/Leaves/TeamCalendar';
import Approvals from './pages/Leaves/Approvals';

import AdminDashboard from './pages/Admin/AdminDashboard';
import LeaveTypes from './pages/Admin/LeaveTypes';
import UserManagement from './pages/Admin/UserManagement';
import DepartmentManagement from './pages/Admin/DepartmentManagement';
import Workflow from './pages/Admin/Workflow';
import Holidays from './pages/Admin/Holidays';

import Reports from './pages/Reports/Reports';
import NotFound from './pages/Common/NotFound';
import Profile from './pages/Common/Profile';

// Layout & Guard
import RequireAuth from './components/Guards/RequireAuth';
import MainLayout from './components/Layout/MainLayout';

function App() {
  return (
    <AuthProvider>
      <Router>
        <Routes>
          {/* Public Routes */}
          <Route path="/login" element={<Login />} />
          <Route path="/forgot-password" element={<ForgotPassword />} />
          <Route path="/reset-password" element={<ResetPassword />} />

          {/* Protected Routes - All require login */}
          <Route element={<RequireAuth />}>
             <Route element={<MainLayout />}>
                {/* Common Routes */}
                <Route path="/dashboard" element={<Dashboard />} />
                <Route path="/my-leaves" element={<MyLeaves />} />
                <Route path="/new-request" element={<NewRequest />} />
                <Route path="/profile" element={<Profile />} />
                
                {/* Manager / HR Routes */}
                <Route element={<RequireAuth allowedRoles={['MANAGER', 'HR']} />}>
                   <Route path="/approvals" element={<Approvals />} />
                   <Route path="/team-calendar" element={<TeamCalendar />} />
                   <Route path="/manager-dashboard" element={<ManagerDashboard />} />
                </Route>

                {/* HR Routes - Admin yetkisi HR rolündedir */}
                <Route element={<RequireAuth allowedRoles={['HR']} />}>
                   <Route path="/admin/dashboard" element={<AdminDashboard />} />
                   <Route path="/admin/leave-types" element={<LeaveTypes />} />
                   <Route path="/admin/users" element={<UserManagement />} />
                   <Route path="/admin/departments" element={<DepartmentManagement />} />
                   <Route path="/admin/workflow" element={<Workflow />} />
                   <Route path="/admin/holidays" element={<Holidays />} />
                </Route>

                {/* Reports - HR ve Muhasebe erişebilir */}
                <Route element={<RequireAuth allowedRoles={['HR', 'ACCOUNTANT']} />}>
                   <Route path="/reports" element={<Reports />} />
                </Route>
             </Route>
          </Route>

          {/* Catch All */}
          <Route path="*" element={<NotFound />} />
        </Routes>
      </Router>
    </AuthProvider>
  );
}

export default App;
