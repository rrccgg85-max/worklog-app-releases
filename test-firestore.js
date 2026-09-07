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
    console.log("DATA:");
    console.log(JSON.stringify(snap.data(), null, 2));
  } else {
    console.log("No such document!");
  }
}
run().catch(console.error);
