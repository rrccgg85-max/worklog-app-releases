import { db } from "../firebase";
import { collection, doc, setDoc, updateDoc, deleteDoc, onSnapshot, serverTimestamp } from "firebase/firestore";
import { TechUser } from "../types";
import { addSystemLog } from "./logService";

// Primary collection from Firestore is 'technicians'
const PRIMARY_TECH_COLLECTION = "technicians";
const FALLBACK_TECH_COLLECTION = "tech_users";

function parseTechDoc(docSnapId: string, data: any): TechUser {
  return {
    id: docSnapId,
    name: data.name || data.displayName || data.fullName || data.techName || docSnapId,
    email: data.email || "",
    phone: data.phone || data.tel || data.phoneNumber || "-",
    pin: data.pin || "123456",
    role: (data.role?.toLowerCase() === "admin" ? "admin" : "technician") as "technician" | "admin",
    isActive: data.isActive !== undefined ? Boolean(data.isActive) : data.status !== "inactive",
    skills: Array.isArray(data.skills) ? data.skills : (data.role ? [data.role] : ["IT Support Onsite"]),
    createdAt: data.createdAt || null
  };
}

export function subscribeToTechnicians(callback: (techs: TechUser[]) => void, onError?: (err: any) => void) {
  const primaryColRef = collection(db, PRIMARY_TECH_COLLECTION);
  
  let primaryTechs: TechUser[] = [];
  let fallbackTechs: TechUser[] = [];

  const emitCombined = () => {
    const map = new Map<string, TechUser>();
    // Primary technicians take precedence
    for (const t of primaryTechs) {
      map.set(t.id, t);
    }
    // Add fallback if not present
    for (const t of fallbackTechs) {
      if (!map.has(t.id)) {
        map.set(t.id, t);
      }
    }
    const combined = Array.from(map.values());
    combined.sort((a, b) => a.name.localeCompare(b.name, 'th'));
    callback(combined);
  };

  // 1. Subscribe to 'technicians'
  const unsubPrimary = onSnapshot(primaryColRef, (snapshot) => {
    primaryTechs = snapshot.docs.map((d) => parseTechDoc(d.id, d.data()));
    emitCombined();
  }, (err) => {
    console.warn("Notice subscribing to 'technicians' collection:", err);
    if (onError) onError(err);
  });

  // 2. Subscribe to 'tech_users' as fallback
  let unsubFallback: (() => void) | undefined;
  try {
    const fallbackColRef = collection(db, FALLBACK_TECH_COLLECTION);
    unsubFallback = onSnapshot(fallbackColRef, (snapshot) => {
      fallbackTechs = snapshot.docs.map((d) => parseTechDoc(d.id, d.data()));
      emitCombined();
    }, (err) => {
      console.warn("Notice subscribing to fallback 'tech_users':", err);
    });
  } catch (e) {
    console.warn("Fallback subscription skipped:", e);
  }

  return () => {
    unsubPrimary();
    if (unsubFallback) unsubFallback();
  };
}

export async function addTechnician(tech: Omit<TechUser, "createdAt">) {
  const techDocRef = doc(db, PRIMARY_TECH_COLLECTION, tech.id);
  await setDoc(techDocRef, {
    name: tech.name,
    email: tech.email,
    phone: tech.phone,
    pin: tech.pin,
    role: tech.role,
    isActive: tech.isActive,
    skills: tech.skills || [],
    createdAt: serverTimestamp()
  });

  await addSystemLog({
    action: "ADD_TECH",
    actor: "Web Admin",
    technicianId: tech.id,
    technicianName: tech.name,
    details: `เพิ่มช่างใหม่: ${tech.name} (รหัส ${tech.id}, PIN: ${tech.pin})`
  });
}

export async function updateTechnician(id: string, updates: Partial<TechUser>) {
  const techDocRef = doc(db, PRIMARY_TECH_COLLECTION, id);
  await updateDoc(techDocRef, {
    ...updates,
    updatedAt: serverTimestamp()
  });

  await addSystemLog({
    action: "UPDATE_TECH",
    actor: "Web Admin",
    technicianId: id,
    details: `แก้ไขข้อมูลช่าง ${id}`
  });
}

export async function toggleTechStatus(id: string, currentStatus: boolean, name?: string) {
  const techDocRef = doc(db, PRIMARY_TECH_COLLECTION, id);
  await updateDoc(techDocRef, {
    isActive: !currentStatus,
    updatedAt: serverTimestamp()
  });

  await addSystemLog({
    action: "UPDATE_TECH",
    actor: "Web Admin",
    technicianId: id,
    technicianName: name,
    details: `เปลี่ยนสถานะช่าง ${name || id} เป็น ${!currentStatus ? "พร้อมรับงาน (Active)" : "พักงาน/ปิดรับงาน (Inactive)"}`
  });
}

export async function deleteTechnician(id: string, name?: string) {
  await deleteDoc(doc(db, PRIMARY_TECH_COLLECTION, id));
  await addSystemLog({
    action: "DELETE_TECH",
    actor: "Web Admin",
    technicianId: id,
    technicianName: name,
    details: `ลบรายชื่อช่าง ${name || id} ออกจากระบบ`
  });
}
