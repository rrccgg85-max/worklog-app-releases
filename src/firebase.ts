import { initializeApp, getApps, getApp } from "firebase/app";
import { getFirestore } from "firebase/firestore";
import { getAuth } from "firebase/auth";
import { getStorage } from "firebase/storage";
import { initializeAppCheck, ReCaptchaV3Provider } from "firebase/app-check";

const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY,
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN,
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID,
  storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET,
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID,
  appId: import.meta.env.VITE_FIREBASE_APP_ID
};

// Initialize Firebase App
const app = !getApps().length ? initializeApp(firebaseConfig) : getApp();

// Initialize Firebase App Check in browser environment
if (typeof window !== 'undefined') {
  // ใช้ Debug Token แบบคงที่ (Static) เพื่อไม่ให้เปลี่ยนทุกครั้งที่รีเฟรชหน้าจอ
  // คุณสามารถนำ Token นี้ไปเพิ่มใน Firebase Console ได้ง่ายๆ เพียงครั้งเดียว
  if (import.meta.env.DEV || window.location.hostname.includes("run.app") || window.location.hostname.includes("localhost")) {
    (self as any).FIREBASE_APPCHECK_DEBUG_TOKEN = "worklog-admin-debug-token-99999";
  }
  try {
    const recaptchaKey = import.meta.env.VITE_RECAPTCHA_SITE_KEY || "6Lcr6ZQtAAAAACeea9K76oam1OkB0jM4lo7iRkL9";
    if (recaptchaKey) {
      initializeAppCheck(app, {
        provider: new ReCaptchaV3Provider(recaptchaKey),
        isTokenAutoRefreshEnabled: true
      });
    }
  } catch (e) {
    console.warn("App Check initialization note:", e);
  }
}

// Initialize Firestore
export const db = getFirestore(app);

// Initialize Firebase Auth
export const auth = getAuth(app);

// Authentication is handled explicitly by LoginScreen and other components.
// We removed anonymous sign-in because the app requires proper email/password authentication
// according to the security rules.

// Initialize Firebase Storage
export const storage = getStorage(app);
export default app;

