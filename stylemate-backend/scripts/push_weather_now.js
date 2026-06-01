const { runWeatherNotificationJob } = require('../jobs/weatherNotificationJob');

const parseUserId = () => {
  const flagIndex = process.argv.indexOf('--userId');
  if (flagIndex === -1) return null;
  return process.argv[flagIndex + 1] || null;
};

const run = async () => {
  const userId = parseUserId();
  try {
    const result = await runWeatherNotificationJob({ userId });
    console.log('Weather push result:', result);
    process.exit(0);
  } catch (error) {
    console.error('Weather push failed:', error.message);
    process.exit(1);
  }
};

run();

