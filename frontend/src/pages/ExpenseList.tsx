import React, { useState, useEffect } from 'react';
import api from '../services/api';
import { Search, Trash2, Filter, RefreshCw } from 'lucide-react';

interface Expense {
  id: string;
  description: string;
  amount: number;
  currency: string;
  splitType: string;
  isSettlement: boolean;
  expenseDate: string;
  paidBy: {
    id: string;
    name: string;
  };
}

export const ExpenseList: React.FC = () => {
  const [expenses, setExpenses] = useState<Expense[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string>('');
  
  // Search & Filter state
  const [search, setSearch] = useState<string>('');
  const [payerFilter, setPayerFilter] = useState<string>('');
  const [currencyFilter, setCurrencyFilter] = useState<string>('');
  const [sortBy, setSortBy] = useState<string>('date_desc');

  useEffect(() => {
    fetchActiveGroupAndExpenses();
  }, []);

  const fetchActiveGroupAndExpenses = async () => {
    try {
      setLoading(true);
      const groupResponse = await api.get('/api/groups');
      if (groupResponse.data && groupResponse.data.length > 0) {
        const id = groupResponse.data[0].id;
        fetchExpenses(id);
      } else {
        setLoading(false);
      }
    } catch (err: any) {
      setError('Failed to load active group space.');
      setLoading(false);
    }
  };

  const fetchExpenses = async (gId: string) => {
    try {
      const response = await api.get(`/api/groups/${gId}/expenses`);
      const list = response.data.filter((e: Expense) => !e.isSettlement);
      setExpenses(list);
    } catch (err: any) {
      setError('Failed to fetch expenses list.');
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id: string) => {
    if (!window.confirm('Are you sure you want to delete this expense? This will recalculate balances.')) {
      return;
    }
    try {
      await api.delete(`/api/expenses/${id}`);
      setExpenses((prev) => prev.filter((e) => e.id !== id));
    } catch (err: any) {
      setError('Failed to delete expense record.');
    }
  };

  const payers = Array.from(new Set(expenses.map((e) => e.paidBy.name)));

  const filteredExpenses = expenses
    .filter((e) => {
      const matchesSearch = e.description.toLowerCase().includes(search.toLowerCase());
      const matchesPayer = payerFilter === '' || e.paidBy.name === payerFilter;
      const matchesCurrency = currencyFilter === '' || e.currency === currencyFilter;
      return matchesSearch && matchesPayer && matchesCurrency;
    })
    .sort((a, b) => {
      if (sortBy === 'date_desc') {
        return new Date(b.expenseDate).getTime() - new Date(a.expenseDate).getTime();
      }
      if (sortBy === 'date_asc') {
        return new Date(a.expenseDate).getTime() - new Date(b.expenseDate).getTime();
      }
      if (sortBy === 'amount_desc') {
        return b.amount - a.amount;
      }
      if (sortBy === 'amount_asc') {
        return a.amount - b.amount;
      }
      return 0;
    });

  if (loading) {
    return (
      <div className="container text-center" style={{ padding: '80px 24px' }}>
        <h2 className="flex justify-center items-center space-x-2" style={{ fontSize: '20px' }}>
          <RefreshCw className="animate-spin text-emerald" />
          <span>Fetching Expense Logs...</span>
        </h2>
      </div>
    );
  }

  return (
    <div className="container space-y-6">
      {/* Title */}
      <div>
        <h2 className="font-bold" style={{ fontSize: '28px' }}>Expenses Ledger</h2>
        <p className="text-muted" style={{ fontSize: '14px', marginTop: '4px' }}>View, query, and manage active group expenses</p>
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      {/* Filter Toolbar */}
      <div className="card grid grid-cols-3" style={{ padding: '20px', gap: '16px' }}>
        {/* Search */}
        <div style={{ position: 'relative' }}>
          <input
            type="text"
            placeholder="Search description..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="input-field"
            style={{ paddingLeft: '40px' }}
          />
          <Search size={16} className="text-muted" style={{ position: 'absolute', left: '14px', top: '14px' }} />
        </div>

        {/* Payer Filter */}
        <div className="flex items-center space-x-2">
          <Filter size={16} className="text-muted" />
          <select
            value={payerFilter}
            onChange={(e) => setPayerFilter(e.target.value)}
            className="input-field"
          >
            <option value="">All Payers</option>
            {payers.map((payer) => (
              <option key={payer} value={payer}>{payer}</option>
            ))}
          </select>
        </div>

        {/* Currency Filter */}
        <select
          value={currencyFilter}
          onChange={(e) => setCurrencyFilter(e.target.value)}
          className="input-field"
        >
          <option value="">All Currencies</option>
          <option value="INR">INR (₹)</option>
          <option value="USD">USD ($)</option>
        </select>

        {/* Sorting */}
        <select
          value={sortBy}
          onChange={(e) => setSortBy(e.target.value)}
          className="input-field"
        >
          <option value="date_desc">Newest First</option>
          <option value="date_asc">Oldest First</option>
          <option value="amount_desc">Highest Cost</option>
          <option value="amount_asc">Lowest Cost</option>
        </select>
      </div>

      {/* Expenses Table */}
      <div className="table-container">
        {filteredExpenses.length === 0 ? (
          <div className="text-center text-muted" style={{ padding: '32px', fontSize: '14px' }}>
            No expenses match the specified search queries.
          </div>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>Date</th>
                <th>Description</th>
                <th>Paid By</th>
                <th>Split Type</th>
                <th className="text-right">Amount</th>
                <th className="text-center">Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredExpenses.map((expense) => (
                <tr key={expense.id}>
                  <td className="font-mono text-muted" style={{ fontSize: '12px' }}>
                    {new Date(expense.expenseDate).toLocaleDateString('en-GB')}
                  </td>
                  <td className="font-semibold">{expense.description}</td>
                  <td>{expense.paidBy.name}</td>
                  <td className="font-mono text-muted" style={{ fontSize: '11px' }}>{expense.splitType}</td>
                  <td className="text-right font-mono font-bold text-emerald">
                    {expense.currency === 'INR' ? '₹' : '$'} {expense.amount.toFixed(2)}
                  </td>
                  <td className="text-center">
                    <button
                      onClick={() => handleDelete(expense.id)}
                      className="btn btn-rose"
                      style={{ padding: '6px', width: 'auto', border: 'none', background: 'none' }}
                    >
                      <Trash2 size={16} />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
};
