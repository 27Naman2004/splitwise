import React from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { LogOut, Receipt, Users, PlusCircle, CreditCard, LayoutDashboard } from 'lucide-react';

export const Navbar: React.FC = () => {
  const { user, logout, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  if (!isAuthenticated) return null;

  const isActive = (path: string) => location.pathname === path;

  return (
    <nav className="navbar">
      <div className="flex items-center space-x-4">
        <Link to="/" className="navbar-brand">
          Splitwise Auditor
        </Link>
        <div className="navbar-links">
          <Link to="/" className={`navbar-link ${isActive('/') ? 'active' : ''}`}>
            <LayoutDashboard size={16} />
            <span>Dashboard</span>
          </Link>
          <Link to="/expenses" className={`navbar-link ${isActive('/expenses') ? 'active' : ''}`}>
            <Receipt size={16} />
            <span>Expenses</span>
          </Link>
          <Link to="/expenses/create" className={`navbar-link ${isActive('/expenses/create') ? 'active' : ''}`}>
            <PlusCircle size={16} />
            <span>Add Expense</span>
          </Link>
          <Link to="/balances" className={`navbar-link ${isActive('/balances') ? 'active' : ''}`}>
            <Users size={16} />
            <span>Balances</span>
          </Link>
          <Link to="/settlements" className={`navbar-link ${isActive('/settlements') ? 'active' : ''}`}>
            <CreditCard size={16} />
            <span>Settlements</span>
          </Link>
        </div>
      </div>
      <div className="flex items-center space-x-4">
        <span className="text-muted" style={{ fontSize: '13px' }}>
          Logged in as <strong style={{ color: '#fff' }}>{user?.name}</strong>
        </span>
        <button
          onClick={handleLogout}
          className="btn btn-rose"
          style={{ padding: '6px 12px', fontSize: '12px', width: 'auto' }}
        >
          <LogOut size={14} />
          <span>Logout</span>
        </button>
      </div>
    </nav>
  );
};
