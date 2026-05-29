const fs = require('fs');
const path = require('path');
const axios = require('axios');
const FormData = require('form-data');

const BASE_URL = process.env.STYLEMATE_BASE_URL || 'http://localhost:3000/';
const TOKEN = process.env.TEST_AUTH_TOKEN;
const SAMPLE_PATH = process.env.SAMPLE_IMAGE_PATH || path.join(__dirname, 'sample.jpg');

const run = async () => {
  if (!TOKEN) {
    console.log('SKIP: TEST_AUTH_TOKEN chưa được cấu hình.');
    process.exit(0);
  }

  if (!fs.existsSync(SAMPLE_PATH)) {
    console.log(`SKIP: Không tìm thấy ảnh mẫu tại ${SAMPLE_PATH}`);
    process.exit(0);
  }

  const form = new FormData();
  form.append('image', fs.createReadStream(SAMPLE_PATH));

  const url = new URL('api/images/remove-bg', BASE_URL).toString();

  const response = await axios.post(url, form, {
    headers: {
      ...form.getHeaders(),
      Authorization: `Bearer ${TOKEN}`
    },
    responseType: 'arraybuffer',
    timeout: 30000
  });

  if (response.status !== 200) {
    throw new Error(`Remove-bg failed: ${response.status}`);
  }

  const outputPath = path.join(__dirname, 'remove_bg_output.png');
  fs.writeFileSync(outputPath, response.data);
  console.log(`OK: Saved output to ${outputPath}`);
};

run().catch((error) => {
  console.error('FAIL:', error.message);
  process.exit(1);
});

