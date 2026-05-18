/**
 * 🗄️ Database Configuration
 * 
 * Kết nối MongoDB thông qua Mongoose.
 * Đọc URI từ biến môi trường MONGODB_URI.
 */
const mongoose = require('mongoose');

const connectDatabase = async () => {
  const uri = process.env.MONGODB_URI || 'mongodb://localhost:27017/stylemate';

  try {
    await mongoose.connect(uri);
    console.log(`✅ Đã kết nối MongoDB thành công: ${mongoose.connection.host}`);
  } catch (error) {
    console.error('❌ Lỗi kết nối MongoDB:', error.message);
    process.exit(1);
  }

  mongoose.connection.on('error', (err) => {
    console.error('⚠️ Lỗi MongoDB runtime:', err);
  });

  mongoose.connection.on('disconnected', () => {
    console.warn('⚠️ MongoDB đã ngắt kết nối');
  });
};

module.exports = connectDatabase;
