// API utilities for food statistics

/**
 * Fetches food statistics summary for a specific user
 * @param {number} userId - The ID of the user
 * @returns {Promise} Promise containing the food stats data
 */
export const fetchFoodSummary = async (userId) => {
  try {
    const response = await fetch(
      `http://localhost:8080/api/stats/food/summary/${userId}`
    );

    if (!response.ok) {
      throw new Error(`API error: ${response.status}`);
    }

    return await response.json();
  } catch (error) {
    console.error('Error fetching food summary:', error);
    throw error;
  }
};

/**
 * Fetches monthly food waste and savings data
 * @param {number} userId - The ID of the user
 * @param {number} year - The year to get data for
 * @returns {Promise} Promise containing the monthly data
 */
export const fetchMonthlyFoodData = async (
  userId,
  year = new Date().getFullYear()
) => {
  try {
    const response = await fetch(
      `http://localhost:8080/api/stats/food/monthly/${userId}`
    );

    if (!response.ok) {
      throw new Error(`API error: ${response.status}`);
    }

    return await response.json();
  } catch (error) {
    console.error('Error fetching monthly food data:', error);
    throw error;
  }
};

/**
 * Fetches food wasted in the last week for a specific user
 * @param {number} userId - The ID of the user
 * @returns {Promise} Promise containing last week's food waste data
 */
export const fetchLastWeekWaste = async (userId) => {
  try {
    const response = await fetch(
      `http://localhost:8080/api/stats/food/wasted/last-week/${userId}`
    );

    if (!response.ok) {
      throw new Error(`API error: ${response.status}`);
    }

    return await response.json();
  } catch (error) {
    console.error('Error fetching last week waste data:', error);
    throw error;
  }
};

/**
 * Fetches food wasted in the last month for a specific user
 * @param {number} userId - The ID of the user
 * @returns {Promise} Promise containing last month's food waste data
 */
export const fetchLastMonthWaste = async (userId) => {
  try {
    const response = await fetch(
      `http://localhost:8080/api/stats/food/wasted/last-month/${userId}`
    );

    if (!response.ok) {
      throw new Error(`API error: ${response.status}`);
    }

    return await response.json();
  } catch (error) {
    console.error('Error fetching last month waste data:', error);
    throw error;
  }
};

/**
 * Fetches donation statistics
 * @param {number} userId - The ID of the user
 * @returns {Promise} Promise containing donation stats
 */
export const fetchDonationStats = async (userId) => {
  try {
    const response = await fetch(
      `http://localhost:8080/api/stats/donations/${userId}`
    );

    if (!response.ok) {
      throw new Error(`API error: ${response.status}`);
    }

    return await response.json();
  } catch (error) {
    console.error('Error fetching donation stats:', error);
    throw error;
  }
};

/**
 * Fetches donation summary for a specific user
 * @param {number} userId - The ID of the user
 * @returns {Promise} Promise containing donation summary data
 */
export const fetchDonationSummary = async (userId) => {
  try {
    const response = await fetch(
      `http://localhost:8080/api/stats/donations/summary/${userId}`
    );

    if (!response.ok) {
      throw new Error(`API error: ${response.status}`);
    }

    return await response.json();
  } catch (error) {
    console.error('Error fetching donation summary:', error);
    throw error;
  }
};

/**
 * Fetches last month's donation data for a specific user
 * @param {number} userId - The ID of the user
 * @returns {Promise} Promise containing last month's donation data
 */
export const fetchLastMonthDonations = async (userId) => {
  try {
    const response = await fetch(
      `http://localhost:8080/api/stats/donations/last-month/${userId}`
    );

    if (!response.ok) {
      throw new Error(`API error: ${response.status}`);
    }

    return await response.json();
  } catch (error) {
    console.error('Error fetching last month donations:', error);
    throw error;
  }
};
