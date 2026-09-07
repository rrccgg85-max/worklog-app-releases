import { db } from "../firebase";
import { collection, addDoc, serverTimestamp, query, limit, onSnapshot } from "firebase/firestore";
import { SystemLog } from "../types";

const PRIMARY_LOGS_COLLECTION = "activity_logs";
const FALLBACK_LOGS_COLLECTION = "system_logs";

export async function addSystemLog(log: Omit<SystemLog, "id" | "timestamp">) {
  try {
    await addDoc(collection(db, PRIMARY_LOGS_COLLECTION), {
      ...log,
      timestamp: serverTimestamp()
    });
  } catch (error) {
    console.warn("Error adding system log:", error);
  }
}

export function subscribeToSystemLogs(callback: (logs: SystemLog[]) => void, maxCount: number = 50, onError?: (err: any) => void) {
  const qPrimary = query(collection(db, PRIMARY_LOGS_COLLECTION), limit(maxCount));
  
  let primaryLogs: SystemLog[] = [];
  let fallbackLogs: SystemLog[] = [];

  const emitCombined = () => {
    const map = new Map<string, SystemLog>();
    for (const l of primaryLogs) map.set(l.id, l);
    for (const l of fallbackLogs) if (!map.has(l.id)) map.set(l.id, l);
    const combined = Array.from(map.values());
    combined.sort((a, b) => {
      const timeA = typeof a.timestamp === "number" ? a.timestamp : (a.timestamp?.toMillis ? a.timestamp.toMillis() : 0);
      const timeB = typeof b.timestamp === "number" ? b.timestamp : (b.timestamp?.toMillis ? b.timestamp.toMillis() : 0);
      return timeB - timeA;
    });
    callback(combined.slice(0, maxCount));
  };

  const unsubPrimary = onSnapshot(qPrimary, (snapshot) => {
    primaryLogs = snapshot.docs.map((docSnap) => {
      const data = docSnap.data();
      return {
        id: docSnap.id,
        action: data.action || "ACTIVITY",
        actor: data.actor || data.user || data.userName || "Admin",
        details: data.details || data.message || data.description || "",
        caseId: data.caseId || "",
        technicianId: data.technicianId || "",
        technicianName: data.technicianName || "",
        timestamp: data.timestamp || data.createdAt || Date.now(),
        ...data
      } as SystemLog;
    });
    emitCombined();
  }, (err) => {
    console.warn("Notice subscribing to 'activity_logs':", err);
    if (onError) onError(err);
  });

  let unsubFallback: (() => void) | undefined;
  try {
    const qFallback = query(collection(db, FALLBACK_LOGS_COLLECTION), limit(maxCount));
    unsubFallback = onSnapshot(qFallback, (snapshot) => {
      fallbackLogs = snapshot.docs.map((docSnap) => ({
        id: docSnap.id,
        ...docSnap.data()
      } as SystemLog));
      emitCombined();
    }, (err) => {
      console.warn("Notice subscribing to 'system_logs':", err);
    });
  } catch (e) {
    // ignore
  }

  return () => {
    unsubPrimary();
    if (unsubFallback) unsubFallback();
  };
}
