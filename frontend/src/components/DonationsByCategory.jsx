'use client';

import {
  Cell,
  Legend,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
} from '@/components/ui/chart';

const data = [
  { name: 'Food', value: 540 },
  { name: 'Clothes', value: 320 },
  { name: 'Appliances', value: 180 },
  { name: 'Other', value: 120 },
];

// Food-themed colors
const COLORS = ['#10b981', '#f59e0b', '#3b82f6', '#8b5cf6'];

export function DonationsByCategory() {
  return (
    <ResponsiveContainer width='100%' height={300}>
      <PieChart>
        <Pie
          data={data}
          cx='50%'
          cy='50%'
          labelLine={false}
          outerRadius={100}
          innerRadius={60}
          fill='#8884d8'
          dataKey='value'
          label={({ name, percent }) =>
            `${name} ${(percent * 100).toFixed(0)}%`
          }
          paddingAngle={2}
        >
          {data.map((entry, index) => (
            <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
          ))}
        </Pie>
        <Tooltip
          contentStyle={{
            backgroundColor: 'white',
            borderRadius: '8px',
            border: 'none',
            boxShadow: '0 4px 12px rgba(0,0,0,0.1)',
          }}
        />
        <Legend />
      </PieChart>
    </ResponsiveContainer>
  );
}
