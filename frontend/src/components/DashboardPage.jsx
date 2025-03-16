import { useState, useEffect } from 'react';

import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Overview } from '@/components/Overview';
import { RecentActivity } from '@/components/RecentActivity';
import { DonationsByCategory } from '@/components/DonationsByCategory';
import { FoodWasteOverTime } from '@/components/FoodWasteOverTime';
import { DonationStats } from '@/components/DonationStats';
import { FoodWasteStats } from '@/components/FoodWasteStats';
import { Carrot, Leaf, ShoppingBag, Utensils } from 'lucide-react';
import { fetchFoodSummary, fetchDonationSummary } from '@/Api/api';

export default function DashboardPage() {
  const [dateRange, setDateRange] = useState({
    from: new Date(new Date().setDate(new Date().getDate() - 30)),
    to: new Date(),
  });

  const [summaryData, setSummaryData] = useState({
    totalSaved: 0,
    totalWasted: 0,
    wastedLastMonth: 0,
    donations: {
      total: 0,
    },
    wasteReductionRate: 0,
  });

  const [loading, setLoading] = useState(true);

  const userId = 1;

  useEffect(() => {
    const loadSummaryData = async () => {
      try {
        setLoading(true);

        const [foodStats, donationStats] = await Promise.all([
          fetchFoodSummary(userId),
          fetchDonationSummary(userId),
        ]);

        const totalFood = foodStats.totalSaved + foodStats.totalWasted;
        const wasteReductionRate =
          totalFood > 0
            ? Math.round((foodStats.totalSaved / totalFood) * 100)
            : 0;

        setSummaryData({
          ...foodStats,
          donations: donationStats,
          wasteReductionRate,
        });

        setLoading(false);
      } catch (error) {
        console.error('Error loading summary data:', error);
        setLoading(false);
      }
    };

    loadSummaryData();
  }, [userId]);

  const getPercentChange = (current, previous) => {
    if (!previous) return 0;
    return (((current - previous) / previous) * 100).toFixed(1);
  };

  return (
    <div className='flex min-h-screen w-full flex-col bg-gradient-to-b from-amber-50 to-white'>
      <div className='border-b border-slate-200 bg-white/50 backdrop-blur-sm'>
        <div className='flex h-16 items-center px-4 md:px-8'>
          <div className='flex items-center gap-2'>
            <Leaf className='h-8 w-8 text-emerald-500' />
            <h1 className='text-xl font-bold text-emerald-800'>
              FoodSaver Dashboard
            </h1>
          </div>
        </div>
      </div>

      <div className='flex-1 space-y-6 p-4 pt-6 md:p-8'>
        <div className='grid gap-4 md:grid-cols-2 lg:grid-cols-4'>
          {/* Food Saved Card */}
          <Card className='border-none bg-gradient-to-br from-emerald-50 to-emerald-100 shadow-md'>
            <CardHeader className='flex flex-row items-center justify-between space-y-0 pb-2'>
              <CardTitle className='text-sm font-medium text-emerald-800'>
                Total Food Saved
              </CardTitle>
              <Leaf className='h-5 w-5 text-emerald-600' />
            </CardHeader>
            <CardContent>
              <div className='text-2xl font-bold text-emerald-700'>
                {loading ? 'Loading...' : `${summaryData.totalSaved} kg`}
              </div>
              <p className='text-xs text-emerald-600'>
                +
                {getPercentChange(
                  summaryData.totalSaved,
                  summaryData.totalSaved - 200
                )}
                % from last month
              </p>
            </CardContent>
          </Card>

          {/* Food Wasted Card */}
          <Card className='border-none bg-gradient-to-br from-red-50 to-red-100 shadow-md'>
            <CardHeader className='flex flex-row items-center justify-between space-y-0 pb-2'>
              <CardTitle className='text-sm font-medium text-red-800'>
                Total Food Wasted
              </CardTitle>
              <Utensils className='h-5 w-5 text-red-600' />
            </CardHeader>
            <CardContent>
              <div className='text-2xl font-bold text-red-700'>
                {loading ? 'Loading...' : `${summaryData.totalWasted} kg`}
              </div>
              <p className='text-xs text-green-600'>
                {getPercentChange(
                  summaryData.wastedLastMonth,
                  summaryData.wastedLastMonth * 1.05
                ) < 0
                  ? ''
                  : '-'}
                {Math.abs(
                  getPercentChange(
                    summaryData.wastedLastMonth,
                    summaryData.wastedLastMonth * 1.05
                  )
                )}
                % from last month
              </p>
            </CardContent>
          </Card>

          {/* Donations Card */}
          <Card className='border-none bg-gradient-to-br from-amber-50 to-amber-100 shadow-md'>
            <CardHeader className='flex flex-row items-center justify-between space-y-0 pb-2'>
              <CardTitle className='text-sm font-medium text-amber-800'>
                Total Donations
              </CardTitle>
              <ShoppingBag className='h-5 w-5 text-amber-600' />
            </CardHeader>
            <CardContent>
              <div className='text-2xl font-bold text-amber-700'>
                {loading ? 'Loading...' : summaryData.donations?.total || 0}
              </div>
              <p className='text-xs text-green-600'>
                +
                {getPercentChange(
                  summaryData.donations?.total || 0,
                  (summaryData.donations?.total || 0) - 270
                )}
                % from last month
              </p>
            </CardContent>
          </Card>

          {/* Waste Reduction Card */}
          <Card className='border-none bg-gradient-to-br from-blue-50 to-blue-100 shadow-md'>
            <CardHeader className='flex flex-row items-center justify-between space-y-0 pb-2'>
              <CardTitle className='text-sm font-medium text-blue-800'>
                Waste Reduction Rate
              </CardTitle>
              <Carrot className='h-5 w-5 text-blue-600' />
            </CardHeader>
            <CardContent>
              <div className='text-2xl font-bold text-blue-700'>
                {loading ? 'Loading...' : `${summaryData.wasteReductionRate}%`}
              </div>
              <p className='text-xs text-green-600'>
                +
                {getPercentChange(
                  summaryData.wasteReductionRate,
                  summaryData.wasteReductionRate - 4.1
                )}
                % from last month
              </p>
            </CardContent>
          </Card>
        </div>
        <Tabs defaultValue='overview' className='space-y-6'>
          <TabsList className='bg-white/70 backdrop-blur-sm'>
            <TabsTrigger
              value='overview'
              className='data-[state=active]:bg-emerald-100 data-[state=active]:text-emerald-800 cu'
            >
              Overview
            </TabsTrigger>
            <TabsTrigger
              value='food-waste'
              className='data-[state=active]:bg-red-100 data-[state=active]:text-red-800'
            >
              Food Waste
            </TabsTrigger>
            <TabsTrigger
              value='donations'
              className='data-[state=active]:bg-amber-100 data-[state=active]:text-amber-800'
            >
              Donations
            </TabsTrigger>
          </TabsList>

          <TabsContent value='overview' className='space-y-6'>
            <div className='grid gap-6 md:grid-cols-2 lg:grid-cols-7'>
              <Card className='col-span-4 border-none bg-white shadow-md'>
                <CardHeader className='border-b border-slate-200 bg-gradient-to-r from-emerald-50 to-emerald-100 !bg-clip-border p-4'>
                  <CardTitle className='text-emerald-800'>
                    Monthly Overview
                  </CardTitle>
                  <CardDescription className='text-emerald-600'>
                    Track your progress over time
                  </CardDescription>
                </CardHeader>
                <CardContent className='p-6'>
                  <Overview />
                </CardContent>
              </Card>

              <Card className='col-span-3 border-none bg-white shadow-md'>
                <CardHeader className='border-b border-slate-200 !bg-gradient-to-r from-amber-50 to-amber-100  !bg-clip-border p-4'>
                  <CardTitle className='text-amber-800'>
                    Donations by Category
                  </CardTitle>
                  <CardDescription className='text-amber-600'>
                    Distribution across categories
                  </CardDescription>
                </CardHeader>
                <CardContent className='p-6'>
                  <DonationsByCategory />
                </CardContent>
              </Card>
            </div>

            <Card className='border-none bg-white shadow-md'>
              <CardHeader className='border-b border-slate-200 bg-gradient-to-r from-blue-50 to-blue-100 p-4'>
                <CardTitle className='text-blue-800'>Recent Activity</CardTitle>
                <CardDescription className='text-blue-600'>
                  Latest donations and waste records
                </CardDescription>
              </CardHeader>
              <CardContent className='p-6'>
                <RecentActivity />
              </CardContent>
            </Card>
          </TabsContent>

          <TabsContent value='food-waste' className='space-y-6'>
            <div className='grid gap-6 md:grid-cols-2 lg:grid-cols-3'>
              <Card className='col-span-2 border-none bg-white shadow-md'>
                <CardHeader className='border-b border-slate-200 bg-gradient-to-r from-red-50 to-red-100 p-4'>
                  <CardTitle className='text-red-800'>
                    Food Waste Over Time
                  </CardTitle>
                  <CardDescription className='text-red-600'>
                    Track your food waste reduction progress
                  </CardDescription>
                </CardHeader>
                <CardContent className='p-6'>
                  <FoodWasteOverTime />
                </CardContent>
              </Card>

              <Card className='border-none bg-white shadow-md'>
                <CardHeader className='border-b border-slate-200 p-4 bg-gradient-to-r from-red-50 to-red-100'>
                  <CardTitle className='text-red-800'>
                    Food Waste Statistics
                  </CardTitle>
                  <CardDescription className='text-red-600'>
                    Detailed breakdown of waste metrics
                  </CardDescription>
                </CardHeader>
                <CardContent className='p-6'>
                  <FoodWasteStats />
                </CardContent>
              </Card>
            </div>
          </TabsContent>

          <TabsContent value='donations' className='space-y-6'>
            <div className='grid gap-6 md:grid-cols-2 lg:grid-cols-3'>
              <Card className='col-span-2 border-none bg-white shadow-md'>
                <CardHeader className='border-b border-slate-200 p-4 bg-gradient-to-r from-amber-50 to-amber-100'>
                  <CardTitle className='text-amber-800'>
                    Donations Over Time
                  </CardTitle>
                  <CardDescription className='text-amber-600'>
                    Track your donation history
                  </CardDescription>
                </CardHeader>
                <CardContent className='p-6'>
                  <Overview />
                </CardContent>
              </Card>

              <Card className='border-none bg-white shadow-md'>
                <CardHeader className='border-b border-slate-200 p-4 bg-gradient-to-r from-amber-50 to-amber-100'>
                  <CardTitle className='text-amber-800'>
                    Donation Statistics
                  </CardTitle>
                  <CardDescription className='text-amber-600'>
                    Detailed breakdown of donation metrics
                  </CardDescription>
                </CardHeader>
                <CardContent className='p-6'>
                  <DonationStats />
                </CardContent>
              </Card>
            </div>
          </TabsContent>
        </Tabs>
      </div>
    </div>
  );
}
