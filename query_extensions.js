import { initializeApp } from 'firebase/app';
import { getFirestore, doc, getDoc } from 'firebase/firestore';

const firebaseConfig = {
  projectId: "work-log-fcca2",
  appId: "1:682447251370:web:3bdd8ca771f441d5ceedd3",
  apiKey: "AIzaSyBn3wqAXeoxSXL-niBtPHVN0Q93GifSkqQ",
  authDomain: "work-log-fcca2.firebaseapp.com",
  storageBucket: "work-log-fcca2.firebasestorage.app"
};

const app = initializeApp(firebaseConfig);
const db = getFirestore(app);

async function run() {
  const docRef = doc(db, 'repair_cases', '240826003');
  const snap = await getDoc(docRef);
  
  if (snap.exists()) {
    const data = snap.data();
    for (const key in data) {
      const val = data[key];
      if (typeof val === 'string' && (val.includes('jpg') || val.includes('png') || val.includes('jpeg') || val.includes('%2F'))) {
         console.log(`Potential path in ${key}: ${val}`);
      }
    }
  }
  process.exit(0);
}
run().catch(e => { console.error(e); process.exit(1); });
