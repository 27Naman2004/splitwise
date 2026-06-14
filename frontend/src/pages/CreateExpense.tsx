import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';
import { AlertCircle, PlusCircle, User, ArrowLeft } from 'lucide-react';

interface GroupMember {
  id: string;
  name: string;
  email: string;
}

export const CreateExpense: React.FC = () => {
  const navigate = useNavigate();
  const [groupId, setGroupId] = useState<string>('');
  const [members, setMembers] = useState<GroupMember[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string>('');
  const [submitting, setSubmitting] = useState<boolean>(false);

  // Form states
  const [description, setDescription] = useState<string>('');
  const [amount, setAmount] = useState<string>('');
  const [currency, setCurrency] = useState<string>('INR');
  const [paidBy, setPaidBy] = useState<string>('');
  const [splitType, setSplitType] = useState<string>('EQUAL');
  const [expenseDate, setExpenseDate] = useState<string>(new Date().toISOString().substring(0, 16));
  const [notes, setNotes] = useState<string>('');

  const [checkedMembers, setCheckedMembers] = useState<Record<string, boolean>>({});
  const [splitValues, setSplitValues] = useState<Record<string, string>>({});

  useEffect(() => {
    fetchActiveGroupAndMembers();
  }, []);

  const fetchActiveGroupAndMembers = async () => {
    try {
      setLoading(true);
      const groupResponse = await api.get('/api/groups');
      if (groupResponse.data && groupResponse.data.length > 0) {
        const activeGroup = groupResponse.data[0];
        setGroupId(activeGroup.id);
        
        const membersResponse = await api.get(`/api/groups/${activeGroup.id}/members`);
        setMembers(membersResponse.data);
        
        if (membersResponse.data.length > 0) {
          setPaidBy(membersResponse.data[0].id);
          const initialChecked: Record<string, boolean> = {};
          const initialValues: Record<string, string> = {};
          membersResponse.data.forEach((m: GroupMember) => {
            initialChecked[m.id] = true;
            initialValues[m.id] = '1';
          });
          setCheckedMembers(initialChecked);
          setSplitValues(initialValues);
        }
      }
    } catch (err: any) {
      setError('Failed to configure group members roster.');
    } finally {
      setLoading(false);
    }
  };

  const handleCheckboxChange = (userId: string) => {
    setCheckedMembers((prev) => ({ ...prev, [userId]: !prev[userId] }));
  };

  const handleSplitValueChange = (userId: string, val: string) => {
    setSplitValues((prev) => ({ ...prev, [userId]: val }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!groupId) return;

    setError('');
    setSubmitting(true);

    const activeParticipants = members.filter((m) => checkedMembers[m.id]);
    if (activeParticipants.length === 0) {
      setError('Select at least one participant to split the expense with.');
      setSubmitting(false);
      return;
    }

    let splitsPayload;
    try {
      splitsPayload = activeParticipants.map((m) => {
        let splitVal = 1;
        if (splitType === 'PERCENTAGE' || splitType === 'SHARE' || splitType === 'UNEQUAL') {
          const inputVal = parseFloat(splitValues[m.id] || '0');
          if (isNaN(inputVal) || inputVal <= 0) {
            setError(`Invalid split value for member: ${m.name}`);
            throw new Error('Validation failed');
          }
          splitVal = inputVal;
        }
        return {
          userId: m.id,
          splitValue: splitVal,
        };
      });
    } catch (err) {
      setSubmitting(false);
      return;
    }

    try {
      const payload = {
        groupId,
        paidBy,
        description,
        amount: parseFloat(amount),
        currency,
        splitType,
        isSettlement: false,
        expenseDate: new Date(expenseDate).toISOString(),
        notes,
        splits: splitsPayload,
      };

      await api.post('/api/expenses', payload);
      navigate('/expenses');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to create expense. Verify values.');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="container text-center" style={{ padding: '80px 24px' }}>
        <h2 style={{ fontSize: '20px' }}>Configuring Creator Wizard...</h2>
      </div>
    );
  }

  return (
    <div className="container space-y-6">
      {/* Header */}
      <div>
        <button
          onClick={() => navigate('/expenses')}
          className="btn btn-secondary flex items-center mb-4"
          style={{ width: 'auto', padding: '6px 12px', fontSize: '13px' }}
        >
          <ArrowLeft size={14} />
          <span>Back to Ledger</span>
        </button>
        <h2 className="font-bold" style={{ fontSize: '28px' }}>Log Expense</h2>
        <p className="text-muted" style={{ fontSize: '14px', marginTop: '4px' }}>Record a shared transaction and compute splits dynamically</p>
      </div>

      {error && (
        <div className="alert alert-error">
          <AlertCircle size={18} />
          <span>{error}</span>
        </div>
      )}

      {/* Expense Form */}
      <form onSubmit={handleSubmit} className="grid grid-cols-2" style={{ gap: '32px' }}>
        {/* Left Hand: Core Info */}
        <div className="card space-y-4">
          <div className="form-group">
            <label className="form-label">Description</label>
            <input
              type="text"
              required
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="e.g. Groceries DMart"
              className="input-field"
            />
          </div>

          <div className="grid grid-cols-2" style={{ gap: '16px' }}>
            <div className="form-group">
              <label className="form-label">Amount</label>
              <input
                type="number"
                step="0.01"
                required
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                placeholder="2105.00"
                className="input-field"
              />
            </div>

            <div className="form-group">
              <label className="form-label">Currency</label>
              <select
                value={currency}
                onChange={(e) => setCurrency(e.target.value)}
                className="input-field"
              >
                <option value="INR">INR (₹)</option>
                <option value="USD">USD ($)</option>
              </select>
            </div>
          </div>

          <div className="form-group">
            <label className="form-label">Paid By</label>
            <select
              value={paidBy}
              onChange={(e) => setPaidBy(e.target.value)}
              className="input-field"
            >
              {members.map((m) => (
                <option key={m.id} value={m.id}>{m.name}</option>
              ))}
            </select>
          </div>

          <div className="form-group">
            <label className="form-label">Split Strategy</label>
            <select
              value={splitType}
              onChange={(e) => setSplitType(e.target.value)}
              className="input-field"
            >
              <option value="EQUAL">Split Equally</option>
              <option value="PERCENTAGE">Split by Percentages</option>
              <option value="SHARE">Split by Shares/Weights</option>
              <option value="UNEQUAL">Split by Exact Amounts</option>
            </select>
          </div>

          <div className="form-group">
            <label className="form-label">Expense Date</label>
            <input
              type="datetime-local"
              required
              value={expenseDate}
              onChange={(e) => setExpenseDate(e.target.value)}
              className="input-field"
            />
          </div>

          <div className="form-group">
            <label className="form-label">Notes</label>
            <textarea
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              placeholder="Additional comments..."
              rows={3}
              className="input-field"
              style={{ resize: 'none' }}
            />
          </div>
        </div>

        {/* Right Hand: Splits Breakdown */}
        <div className="card flex flex-col justify-between" style={{ backgroundColor: 'var(--bg-input)' }}>
          <div className="space-y-4">
            <h4 className="font-bold uppercase tracking-wider text-muted flex items-center space-x-2" style={{ fontSize: '13px' }}>
              <User size={16} />
              <span>Splits Breakdown</span>
            </h4>

            <div className="space-y-3" style={{ maxHeight: '350px', overflowY: 'auto', paddingRight: '8px' }}>
              {members.map((m) => (
                <div key={m.id} className="flex justify-between items-center" style={{ padding: '8px 0', borderBottom: '1px solid var(--border-slate)' }}>
                  <label className="flex items-center space-x-3 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={!!checkedMembers[m.id]}
                      onChange={() => handleCheckboxChange(m.id)}
                      style={{ marginRight: '8px' }}
                    />
                    <span className="font-medium" style={{ fontSize: '14px', color: checkedMembers[m.id] ? '#fff' : 'var(--text-muted)', textDecoration: checkedMembers[m.id] ? 'none' : 'line-through' }}>
                      {m.name}
                    </span>
                  </label>

                  {checkedMembers[m.id] && splitType !== 'EQUAL' && (
                    <div className="flex items-center space-x-2">
                      <input
                        type="number"
                        step="any"
                        value={splitValues[m.id] || ''}
                        onChange={(e) => handleSplitValueChange(m.id, e.target.value)}
                        required
                        className="input-field"
                        style={{ width: '80px', padding: '4px 8px', fontSize: '12px', textAlign: 'right' }}
                      />
                      <span className="text-muted" style={{ fontSize: '11px', fontWeight: 600 }}>
                        {splitType === 'PERCENTAGE' ? '%' : splitType === 'SHARE' ? 'sh.' : currency === 'INR' ? '₹' : '$'}
                      </span>
                    </div>
                  )}
                </div>
              ))}
            </div>
          </div>

          <button
            type="submit"
            disabled={submitting}
            className="btn btn-primary mt-6"
          >
            <PlusCircle size={18} />
            <span>{submitting ? 'Creating Expense...' : 'Create Expense'}</span>
          </button>
        </div>
      </form>
    </div>
  );
};
