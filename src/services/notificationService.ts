import { db } from "../firebase";
import { collection, query, orderBy, limit, onSnapshot, doc, updateDoc, deleteDoc } from "firebase/firestore";
import { NotificationDoc } from "../types";

const NOTIF_COLLECTION = "notifications";

export function subscribeToNotifications(callback: (notifications: NotificationDoc[]) => void, maxCount: number = 20, onError?: (err: any) => void) {
  const q = query(collection(db, NOTIF_COLLECTION), limit(maxCount));
  return onSnapshot(q, async (snapshot) => {
    const rawNotifs: NotificationDoc[] = snapshot.docs.map((d) => ({
      id: d.id,
      ...d.data()
    } as NotificationDoc));

    const validNotifs = rawNotifs.filter(n => {
      const isDummy = 
        n.caseId?.startsWith("CASE-260823") || 
        n.caseId?.startsWith("CASE-240520") ||
        n.customerName?.includes("สมศรี") || 
        n.customerName?.includes("เคเอ็นจี") || 
        n.customerName?.includes("บ้านสวนริมคลอง");
      return !isDummy;
    });

    callback(validNotifs);

    // Delete dummy notifications
    const dummyNotifs = rawNotifs.filter(n => !validNotifs.includes(n));
    for (const d of dummyNotifs) {
      try {
        await deleteDoc(doc(db, NOTIF_COLLECTION, d.id));
      } catch (e) {
        console.warn("Delete dummy notif error:", e);
      }
    }
  }, (err) => {
    console.error("Notifications subscription error:", err);
    if (onError) onError(err);
  });
}

export async function markNotificationAsRead(id: string) {
  try {
    const notifRef = doc(db, NOTIF_COLLECTION, id);
    await updateDoc(notifRef, {
      isRead: true
    });
  } catch (error) {
    console.warn("Could not mark notif as read:", error);
  }
}
