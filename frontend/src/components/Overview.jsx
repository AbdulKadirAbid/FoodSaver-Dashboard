import { useEffect, useState } from 'react';
import {
  Bar,
  BarChart,
  CartesianGrid,
  Legend,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from '@/components/ui/chart';
import { fetchMonthlyFoodData } from '@/Api/api';

// Fallback data in case API fails
const sampleData = [
  {
    name: 'Jan',
    'Food Saved': 240,
    'Food Wasted': 80,
    Donations: 180,
  },
  {
    name: 'Feb',
    'Food Saved': 300,
    'Food Wasted': 60,
    Donations: 210,
  },
  {
    name: 'Mar',
    'Food Saved': 280,
    'Food Wasted': 70,
    Donations: 190,
  },
  {
    name: 'Apr',
    'Food Saved': 320,
    'Food Wasted': 50,
    Donations: 220,
  },
  {
    name: 'May',
    'Food Saved': 350,
    'Food Wasted': 45,
    Donations: 250,
  },
  {
    name: 'Jun',
    'Food Saved': 380,
    'Food Wasted': 40,
    Donations: 280,
  },
];

export function Overview() {
  const [chartData, setChartData] = useState(sampleData);
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Hardcoded user ID - in a real app, you would get this from auth context
  const userId = 1;

  // Helper function to convert month number to name
  const getMonthName = (monthNum) => {
    const monthNames = [
      'Jan',
      'Feb',
      'Mar',
      'Apr',
      'May',
      'Jun',
      'Jul',
      'Aug',
      'Sep',
      'Oct',
      'Nov',
      'Dec',
    ];
    return monthNames[monthNum - 1] || `Month ${monthNum}`;
  };

  useEffect(() => {
    const loadChartData = async () => {
      try {
        setLoading(true);

        // Get the current year
        const currentYear = new Date().getFullYear();

        // Fetch monthly food data from API
        const monthlyData = await fetchMonthlyFoodData(userId, currentYear);

        if (
          monthlyData &&
          Array.isArray(monthlyData) &&
          monthlyData.length > 0
        ) {
          // Transform API data to chart format
          const formattedData = monthlyData.map((item) => ({
            name: getMonthName(item.month),
            'Food Saved': item.saved || 0,
            'Food Wasted': item.wasted || 0,
            Donations: item.donations || 0,
          }));

          console.log('API data:', formattedData);

          // Sort by month if needed
          formattedData.sort((a, b) => {
            const monthA = monthNames.indexOf(a.name);
            const monthB = monthNames.indexOf(b.name);
            return monthA - monthB;
          });

          setChartData(formattedData);
          console.log('Chart data loaded:', formattedData);
        } else {
          console.log('No monthly data returned, using sample data');
          setChartData(sampleData);
        }

        setLoading(false);
      } catch (err) {
        console.error('Error fetching chart data:', err);
        setError('Failed to load chart data');

        setChartData(sampleData);
        setLoading(false);
      }
    };

    loadChartData();
  }, [userId]);

  const monthNames = [
    'Jan',
    'Feb',
    'Mar',
    'Apr',
    'May',
    'Jun',
    'Jul',
    'Aug',
    'Sep',
    'Oct',
    'Nov',
    'Dec',
  ];

  return (
    <div className='space-y-8'>
      {loading && (
        <div className='h-10 flex items-center'>
          <span className='text-amber-600'>Loading chart data...</span>
        </div>
      )}

      {error && (
        <div className='text-sm text-red-600 mb-2'>
          {error} (using sample data)
        </div>
      )}

      {/* Chart Section */}
      <ResponsiveContainer width='100%' height={350}>
        <BarChart data={chartData}>
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
          <Bar dataKey='Food Saved' fill='#10b981' radius={[4, 4, 0, 0]} />
          <Bar dataKey='Food Wasted' fill='#ef4444' radius={[4, 4, 0, 0]} />
          <Bar dataKey='Donations' fill='#f59e0b' radius={[4, 4, 0, 0]} />
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}
