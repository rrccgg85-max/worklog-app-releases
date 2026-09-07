import { initializeApp } from 'firebase/app';
import { getFirestore, collection, getDocs, limit } from 'firebase/firestore';

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
  const rootCols = ['images', 'photos', 'attachments', 'case_images'];
  for (const col of rootCols) {
    const colRef = collection(db, col);
    const snap = await getDocs(colRef);
    if (!snap.empty) {
      console.log(`FOUND ROOT COLLECTION: ${col} with ${snap.size} docs`);
      // Print first doc
      console.log(snap.docs[0].id, snap.docs[0].data());
    }
  }
  console.log("Root collections check complete.");
  process.exit(0);
}
run().catch(e => { console.error(e); process.exit(1); });
