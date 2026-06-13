const path = require('path');
const dotenv = require('dotenv');
const connectDatabase = require('../config/database');
const { runWeatherNotificationJob } = require('../jobs/weatherNotificationJob');

// ⚡ Load .env trước khi connect database
dotenv.config({ path: path.join(__dirname, '..', '.env'), override: true });

const parseUserId = () => {
  const flagIndex = process.argv.indexOf('--userId');
  if (flagIndex === -1) return null;
  return process.argv[flagIndex + 1] || null;
};

const run = async () => {
  const userId = parseUserId();
  try {
    await connectDatabase();
    const result = await runWeatherNotificationJob({ userId });
    console.log('Weather push result:', result);
    process.exit(0);
  } catch (error) {
    console.error('Weather push failed:', error.message);
    process.exit(1);
  }
};

run();

