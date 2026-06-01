const cron = require('node-cron');
const { runWeatherNotificationJob } = require('../jobs/weatherNotificationJob');

const startScheduler = () => {
  cron.schedule(
    '01 00 * * *',
    async () => {
      try {
        const result = await runWeatherNotificationJob();
        console.log('[CRON] Weather push result:', result);
      } catch (error) {
        console.error('[CRON] Weather push failed:', error.message);
      }
    },
    {
      timezone: 'Asia/Ho_Chi_Minh'
    }
  );
};

module.exports = {
  startScheduler
};
