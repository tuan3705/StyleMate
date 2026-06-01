const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');

let firebaseApp;

const loadServiceAccount = () => {
  const rawJson = process.env.FIREBASE_SERVICE_ACCOUNT_JSON;
  if (rawJson) {
    return JSON.parse(rawJson);
  }

  const accountPath = process.env.FIREBASE_SERVICE_ACCOUNT_PATH;
  if (!accountPath) {
    throw new Error('FIREBASE_SERVICE_ACCOUNT_PATH hoặc FIREBASE_SERVICE_ACCOUNT_JSON chưa được cấu hình');
  }

  const resolvedPath = path.isAbsolute(accountPath)
    ? accountPath
    : path.join(process.cwd(), accountPath);

  const content = fs.readFileSync(resolvedPath, 'utf-8');
  return JSON.parse(content);
};

const getMessaging = () => {
  if (!firebaseApp) {
    const serviceAccount = loadServiceAccount();
    firebaseApp = admin.initializeApp({
      credential: admin.credential.cert(serviceAccount)
    });
  }
  return admin.messaging();
};

const sendMulticast = async (tokens, payload) => {
  if (!tokens.length) {
    return { successCount: 0, failureCount: 0, responses: [] };
  }
  const messaging = getMessaging();
  return messaging.sendEachForMulticast({
    tokens,
    notification: payload.notification,
    data: payload.data
  });
};

module.exports = {
  sendMulticast
};

