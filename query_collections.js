import { initializeApp } from 'firebase/app';
import { getFirestore, doc, collection, getDocs } from 'firebase/firestore';

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
  
  // Also check if there are subcollections. We can't list subcollections in Web SDK,
  // but we can try known names.
  const subcols = ['images', 'photos', 'attachments', 'files', 'checkin', 'checkout'];
  for (const sub of subcols) {
    const colRef = collection(docRef, sub);
    const snap = await getDocs(colRef);
    if (!snap.empty) {
      console.log(`FOUND SUBCOLLECTION: ${sub}`);
      snap.forEach(d => console.log(d.id, d.data()));
    }
  }
  
  console.log("Subcollection check complete.");
  process.exit(0);
}
run().catch(e => { console.error(e); process.exit(1); });
