import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { ProtectedRoute } from './components/ProtectedRoute';
import { Navbar } from './components/Navbar';
import { Login } from './pages/Login';
import { Dashboard } from './pages/Dashboard';
import { ImportReport } from './pages/ImportReport';
import { ExpenseList } from './pages/ExpenseList';
import { CreateExpense } from './pages/CreateExpense';
import { BalanceSummary } from './pages/BalanceSummary';
import { Settlements } from './pages/Settlements';

function App() {
  return (
    <AuthProvider>
      <Router>
        <div className="min-h-screen bg-slate-950 flex flex-col font-sans">
          <Navbar />
          <div className="flex-grow">
            <Routes>
              {/* Public Access */}
              <Route path="/login" element={<Login />} />

              {/* Protected Access */}
              <Route
                path="/"
                element={
                  <ProtectedRoute>
                    <Dashboard />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/import-report"
                element={
                  <ProtectedRoute>
                    <ImportReport />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/expenses"
                element={
                  <ProtectedRoute>
                    <ExpenseList />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/expenses/create"
                element={
                  <ProtectedRoute>
                    <CreateExpense />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/balances"
                element={
                  <ProtectedRoute>
                    <BalanceSummary />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/settlements"
                element={
                  <ProtectedRoute>
                    <Settlements />
                  </ProtectedRoute>
                }
              />
            </Routes>
          </div>
        </div>
      </Router>
    </AuthProvider>
  );
}

export default App;
