import { db } from "../firebase";
import { doc, setDoc, getDoc, onSnapshot } from "firebase/firestore";

const SESSION_DOC_REF = doc(db, "admin_sessions", "active_admin");

export async function registerAdminSession(adminEmail: string): Promise<string> {
  const sessionId = "sess_" + Math.random().toString(36).substring(2, 15) + Date.now().toString(36);
  try {
    await setDoc(SESSION_DOC_REF, {
      sessionId,
      adminEmail: adminEmail.toLowerCase(),
      loginAt: Date.now(),
      userAgent: navigator.userAgent
    });
    localStorage.setItem("worklog_admin_session_id", sessionId);
  } catch (e) {
    console.warn("Failed to register session in Firestore, falling back to local storage:", e);
    localStorage.setItem("worklog_admin_session_id", sessionId);
  }
  return sessionId;
}

export function subscribeToAdminSession(currentSessionId: string, onConflict: () => void): () => void {
  const unsubscribe = onSnapshot(SESSION_DOC_REF, (docSnap) => {
    if (docSnap.exists()) {
      const data = docSnap.data();
      const serverSessionId = data.sessionId;
      if (serverSessionId && currentSessionId && serverSessionId !== currentSessionId) {
        // Conflict detected! Logged in from another device/browser
        onConflict();
      }
    }
  }, (err) => {
    console.warn("Session subscription warning:", err);
  });

  return unsubscribe;
}

export async function clearAdminSession(): Promise<void> {
  try {
    localStorage.removeItem("worklog_admin_session_id");
  } catch (e) {
    // ignore
  }
}
