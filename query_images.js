import { initializeApp } from 'firebase/app';
import { getFirestore, collection, getDocs, query, limit } from 'firebase/firestore';

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
  const colRef = collection(db, 'repair_cases');
  const snap = await getDocs(query(colRef, limit(100)));
  
  let casesWithImages = 0;
  snap.forEach(doc => {
    const data = doc.data();
    let hasImage = false;
    
    // Check all keys for anything that looks like an image
    for (const key in data) {
      const val = data[key];
      if (typeof val === 'string' && (val.startsWith('http') || val.startsWith('data:image/'))) {
        hasImage = true;
      }
    }
    
    if (hasImage) {
      console.log(`\nCase ID: ${doc.id}`);
      console.log(`Data keys: ${Object.keys(data).join(', ')}`);
      for (const key in data) {
        const val = data[key];
        if (typeof val === 'string' && val.length > 50 && val.includes('http')) {
           console.log(`  ${key}: ${val.substring(0, 50)}...`);
        } else if (typeof val === 'string' && (val.includes('image') || val.includes('photo') || val.includes('url'))) {
           console.log(`  ${key}: ${val}`);
        }
      }
      casesWithImages++;
    }
  });
  
  console.log(`\nFound ${casesWithImages} cases containing at least one image URL.`);
  process.exit(0);
}
run().catch(e => { console.error(e); process.exit(1); });
