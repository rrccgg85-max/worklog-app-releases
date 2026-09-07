import { initializeApp } from 'firebase/app';
import { getFirestore, collection, getDocs } from 'firebase/firestore';

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
  const colRef = collection(db, 'repair_cases', '240826003', 'images');
  const snap = await getDocs(colRef);
  if (snap.empty) {
    console.log("No images subcollection");
  } else {
    console.log("FOUND SUBCOLLECTION 'images':");
    snap.forEach(d => console.log(d.id, d.data()));
  }
  process.exit(0);
}
run().catch(e => { console.error(e); process.exit(1); });
