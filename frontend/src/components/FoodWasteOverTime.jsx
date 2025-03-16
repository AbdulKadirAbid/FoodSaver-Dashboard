'use client';

import {
  Area,
  AreaChart,
  CartesianGrid,
  Legend,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from '@/components/ui/chart';

const data = [
  {
    name: 'Week 1',
    'Food Wasted': 80,
    'Food Saved': 240,
  },
  {
    name: 'Week 2',
    'Food Wasted': 75,
    'Food Saved': 250,
  },
  {
    name: 'Week 3',
    'Food Wasted': 65,
    'Food Saved': 270,
  },
  {
    name: 'Week 4',
    'Food Wasted': 60,
    'Food Saved': 290,
  },
  {
    name: 'Week 5',
    'Food Wasted': 55,
    'Food Saved': 310,
  },
  {
    name: 'Week 6',
    'Food Wasted': 50,
    'Food Saved': 330,
  },
  {
    name: 'Week 7',
    'Food Wasted': 45,
    'Food Saved': 350,
  },
  {
    name: 'Week 8',
    'Food Wasted': 40,
    'Food Saved': 370,
  },
];

export function FoodWasteOverTime() {
  return (
    <ResponsiveContainer width='100%' height={350}>
      <AreaChart data={data}>
        <defs>
          <linearGradient id='colorSaved' x1='0' y1='0' x2='0' y2='1'>
            <stop offset='5%' stopColor='#10b981' stopOpacity={0.8} />
            <stop offset='95%' stopColor='#10b981' stopOpacity={0.1} />
          </linearGradient>
          <linearGradient id='colorWasted' x1='0' y1='0' x2='0' y2='1'>
            <stop offset='5%' stopColor='#ef4444' stopOpacity={0.8} />
            <stop offset='95%' stopColor='#ef4444' stopOpacity={0.1} />
          </linearGradient>
        </defs>
        <CartesianGrid strokeDasharray='3 3' stroke='#f0f0f0' />
        <XAxis dataKey='name' tick={{ fill: '#666' }} />
        <YAxis tick={{ fill: '#666' }} />
        <Tooltip
          contentStyle={{
            backgroundColor: 'white',
            borderRadius: '8px',
            border: 'none',
            boxShadow: '0 4px 12px rgba(0,0,0,0.1)',
          }}
        />
        <Legend />
        <Area
          type='monotone'
          dataKey='Food Saved'
          stroke='#10b981'
          fillOpacity={1}
          fill='url(#colorSaved)'
          strokeWidth={2}
        />
        <Area
          type='monotone'
          dataKey='Food Wasted'
          stroke='#ef4444'
          fillOpacity={1}
          fill='url(#colorWasted)'
          strokeWidth={2}
        />
      </AreaChart>
    </ResponsiveContainer>
  );
}
