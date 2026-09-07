import { db, storage } from "../firebase";
import { 
  collection, 
  doc, 
  setDoc, 
  updateDoc, 
  deleteDoc, 
  addDoc, 
  getDocs,
  getDoc,
  query, 
  orderBy, 
  onSnapshot, 
  serverTimestamp 
} from "firebase/firestore";
import { ref, uploadBytes, getDownloadURL } from "firebase/storage";
import { RepairCase, CasePriority } from "../types";
import { addSystemLog } from "./logService";

const CASES_COLLECTION = "repair_cases";
const NOTIF_COLLECTION = "notifications";

// ฟังก์ชันสร้าง Case ID รูปแบบ DDMMYYXXX (เช่น 230826001 สำหรับวันที่ 23 สิงหาคม 2026 เคสแรกของวัน)
export async function generateCaseId(): Promise<string> {
  const now = new Date();
  const dd = String(now.getDate()).padStart(2, '0');
  const mm = String(now.getMonth() + 1).padStart(2, '0');
  const yy = String(now.getFullYear()).slice(-2);
  const datePrefix = `${dd}${mm}${yy}`;

  try {
    const q = query(collection(db, CASES_COLLECTION));
    const snapshot = await getDocs(q);
    let maxRunning = 0;
    snapshot.forEach(docSnap => {
      const data = docSnap.data();
      const cid = data.caseId || docSnap.id;
      if (cid && cid.startsWith(datePrefix)) {
        const suffix = cid.slice(6); // 6 characters of DDMMYY
        const num = parseInt(suffix, 10);
        if (!isNaN(num) && num > maxRunning) {
          maxRunning = num;
        }
      }
    });
    const nextNum = maxRunning + 1;
    return `${datePrefix}${String(nextNum).padStart(3, '0')}`;
  } catch (error) {
    console.warn("Error generating sequential caseId, falling back:", error);
    const randomSuffix = String(Math.floor(Math.random() * 900) + 100);
    return `${datePrefix}${randomSuffix}`;
  }
}

// ฟังก์ชันอัปโหลดรูปภาพ (พร้อม fallback รองรับ Data URL ในกรณี Storage offline)
export async function uploadCaseImage(caseId: string, fileOrDataUrl: File | string, prefix: string = "initial"): Promise<string> {
  if (typeof fileOrDataUrl === "string") {
    // Already a URL or Base64 string
    return fileOrDataUrl;
  }

  try {
    const fileExt = fileOrDataUrl.name.split('.').pop() || 'jpg';
    const storageRef = ref(storage, `cases/${caseId}/${prefix}_${Date.now()}.${fileExt}`);
    const snapshot = await uploadBytes(storageRef, fileOrDataUrl);
    return await getDownloadURL(snapshot.ref);
  } catch (error) {
    console.warn("Storage upload failed or bucket restricted, converting to local data URL:", error);
    return new Promise((resolve) => {
      const reader = new FileReader();
      reader.onloadend = () => resolve(reader.result as string);
      reader.readAsDataURL(fileOrDataUrl);
    });
  }
}

export interface CreateCaseParams {
  customerName: string;
  phone: string;
  address: string;
  details: string;
  category: string;
  priority: CasePriority;
  technicianId: string;
  technicianName: string;
  imageFile?: File | null;
  imageUrl?: string;
}

// ฟังก์ชันเปิดเคสใหม่ และบันทึกลง Firestore + แจ้งเตือนแอปช่าง
export async function createNewCase(params: CreateCaseParams): Promise<string> {
  const caseId = await generateCaseId();
  let finalImageUrl = params.imageUrl || "";

  if (params.imageFile) {
    finalImageUrl = await uploadCaseImage(caseId, params.imageFile, "initial");
  }

  // ข้อมูลเคสซ่อม
  const newCaseData: Omit<RepairCase, "id"> = {
    caseId: caseId,
    customerName: params.customerName.trim(),
    phone: params.phone.trim(),
    address: params.address.trim(),
    details: params.details.trim(),
    category: params.category || "งานซ่อมทั่วไป",
    priority: params.priority || "ปกติ",
    technicianId: params.technicianId,
    technicianName: params.technicianName,
    status: "OPEN",
    createdSource: "WEB",
    imageUrl: finalImageUrl,
    imageUrls: finalImageUrl ? [finalImageUrl] : [],
    checkInImageUrl: "",
    checkOutImageUrl: "",
    solutions: "",
    timestamp: Date.now(),
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp()
  };

  // 1. บันทึกลง Firestore โดย Document ID = caseId เสมอ
  await setDoc(doc(db, CASES_COLLECTION, caseId), newCaseData);

  // 2. ส่ง Notification แจ้งเตือนไปยังแอปช่าง (Android)
  try {
    await addDoc(collection(db, NOTIF_COLLECTION), {
      caseId: caseId,
      technicianId: params.technicianId,
      customerName: params.customerName.trim(),
      message: `งานใหม่: ${caseId} (${params.customerName.trim()}) - ${params.category}`,
      isRead: false,
      timestamp: serverTimestamp()
    });
  } catch (err) {
    console.warn("Notification trigger warning:", err);
  }

  // 3. บันทึก System Log
  await addSystemLog({
    action: "CREATE_CASE",
    actor: "Web Admin",
    caseId: caseId,
    technicianId: params.technicianId,
    technicianName: params.technicianName,
    details: `เปิดเคสใหม่ ${caseId} ลูกค้า: ${params.customerName} มอบหมายให้: ${params.technicianName}`
  });

  return caseId;
}

// ฟังก์ชันตรวจสอบว่าเป็นเคสตัวอย่าง (Dummy Case) หรือไม่
export function isDummyCase(c: Partial<RepairCase>): boolean {
  const dummyIds = [
    "CASE-260823001",
    "CASE-260823002",
    "CASE-260823003",
    "CASE-240520001",
    "CASE-240520002",
    "CASE-240520003",
    "CASE-240520004",
    "260823001",
    "260823002",
    "260823003"
  ];
  const caseId = c.caseId || "";
  const customerName = c.customerName || "";
  const details = c.details || "";

  return (
    dummyIds.includes(caseId) ||
    customerName.includes("สมศรี") ||
    customerName.includes("เคเอ็นจี") ||
    customerName.includes("บ้านสวนริมคลอง") ||
    customerName.includes("ธนกร") ||
    customerName.includes("วิชัย (081-xxx-xxxx)") ||
    details.includes("แอร์ไม่เย็น มีน้ำหยด") ||
    details.includes("ไฟดับทั้งชั้น 2") ||
    details.includes("ท่อเมนประปารั่วซึม")
  );
}

function findImageInObject(data: any, keywords: string[]): string {
  if (!data || typeof data !== 'object') return "";
  for (const key in data) {
    const k = key.toLowerCase();
    if (keywords.some(kw => k.includes(kw))) {
      const val = data[key];
      if (typeof val === 'string' && (val.startsWith('http') || val.startsWith('data:image/'))) {
        return val;
      } else if (Array.isArray(val) && val.length > 0 && typeof val[0] === 'string' && (val[0].startsWith('http') || val[0].startsWith('data:image/'))) {
        return val[0];
      } else if (val && typeof val === 'object' && !Array.isArray(val)) {
        for (const subKey in val) {
          if (typeof val[subKey] === 'string' && (val[subKey].startsWith('http') || val[subKey].startsWith('data:image/'))) {
            return val[subKey];
          }
        }
      }
    }
  }
  return "";
}

function parseCaseDoc(docId: string, data: any): RepairCase {
  const rawStatus = (data.status || data.state || data.repairStatus || data.caseStatus || "OPEN").toString().toUpperCase();
  const status = (rawStatus === "CLOSED" || rawStatus === "COMPLETED" || rawStatus === "DONE" || rawStatus === "SUCCESS") ? "CLOSED" : "OPEN";

  let timestamp = data.timestamp;
  if (!timestamp && data.createdAt) {
    timestamp = data.createdAt?.toMillis ? data.createdAt.toMillis() : Date.now();
  }
  if (!timestamp) {
    timestamp = Date.now();
  }

  const checkInImageUrl = data.checkInImageUrl || data.checkInImageUri || data.checkinImageUrl || data.checkInImage || data.check_in_image || data.checkinPhoto || data.checkInPhoto || data.imageUrlCheckIn || data.photoCheckIn || data.arriveImage || data.arriveImageUrl || findImageInObject(data, ['checkin', 'arrive', 'start']);
  const checkOutImageUrl = data.checkOutImageUrl || data.checkOutImageUri || data.checkoutImageUrl || data.checkOutImage || data.check_out_image || data.checkoutPhoto || data.checkOutPhoto || data.imageUrlCheckOut || data.photoCheckOut || data.afterImageUrl || data.afterImage || data.solutionImageUrl || data.finishImage || data.completeImage || data.doneImage || findImageInObject(data, ['checkout', 'after', 'finish', 'done', 'complete', 'solution']);
  const solutions = data.solutions || data.solution || data.resolution || data.notes || data.note || data.repairDetails || data.workNote || "";

  const checkInAt = data.checkInAt || data.checkinAt || data.check_in_at || data.checkInTime || data.checkedInAt || null;
  const closedAt = data.closedAt || data.closeAt || data.closed_at || data.completedAt || data.checkOutAt || data.checkoutAt || null;

  let latitude: number | undefined = undefined;
  let longitude: number | undefined = undefined;

  const rawLat = data.latitude ?? data.lat ?? data.locationLat ?? data.checkInLat ?? data.geoPoint?.latitude ?? data.location?.latitude;
  const rawLng = data.longitude ?? data.lng ?? data.locationLng ?? data.checkInLng ?? data.geoPoint?.longitude ?? data.location?.longitude;

  if (rawLat !== undefined && rawLat !== null && rawLat !== "") {
    const parsed = typeof rawLat === "number" ? rawLat : parseFloat(rawLat);
    if (!isNaN(parsed)) latitude = parsed;
  }
  if (rawLng !== undefined && rawLng !== null && rawLng !== "") {
    const parsed = typeof rawLng === "number" ? rawLng : parseFloat(rawLng);
    if (!isNaN(parsed)) longitude = parsed;
  }

  // Determine creation source (WEB vs APP)
  const rawSource = (
    data.createdSource || 
    data.createdFrom || 
    data.source || 
    data.channel || 
    data.createdVia || 
    data.openedVia || 
    data.openedFrom || 
    data.device || 
    data.platform || 
    ""
  ).toString().toUpperCase();

  let createdSource: "WEB" | "APP" = "WEB";
  if (
    rawSource.includes("APP") || 
    rawSource.includes("MOBILE") || 
    rawSource.includes("TECH") || 
    rawSource.includes("ANDROID") || 
    rawSource.includes("IOS") || 
    data.createdByApp === true || 
    data.isApp === true || 
    data.fromApp === true
  ) {
    createdSource = "APP";
  } else if (rawSource.includes("WEB") || rawSource.includes("ADMIN") || rawSource.includes("BROWSER")) {
    createdSource = "WEB";
  } else if (data.createdBy && (data.createdBy.toString().toUpperCase().includes("TECH") || data.createdBy.toString().includes("ช่าง"))) {
    createdSource = "APP";
  }

  return {
    caseId: data.caseId || docId,
    customerName: data.customerName || data.customer || "ลูกค้าทั่วไป",
    phone: data.phone || data.phoneNumber || data.tel || "-",
    address: data.address || data.location || "-",
    latitude: latitude,
    longitude: longitude,
    details: data.details || data.description || data.issue || "-",
    category: data.category || "งานทั่วไป",
    priority: (data.priority === "ด่วนที่สุด" || data.priority === "ด่วน" || data.priority === "ปกติ") 
      ? data.priority 
      : (data.priority === "URGENT" || data.priority === "HIGH" ? "ด่วนที่สุด" : "ปกติ"),
    technicianId: data.technicianId || data.techId || "",
    technicianName: data.technicianName || data.techName || "ยังไม่ระบุช่าง",
    status: status,
    createdSource: createdSource,
    imageUrl: data.imageUrl || data.image_url || data.photoUrl || data.photo_url || data.initialImageUrl || data.beforeImage || data.evidenceImage || data.reportImage || findImageInObject(data, ['image', 'photo', 'pic', 'before', 'report', 'evidence']) || "",
    imageUrls: Array.isArray(data.imageUrls) ? data.imageUrls : (Array.isArray(data.images) ? data.images : (Array.isArray(data.photos) ? data.photos : (data.imageUrl ? [data.imageUrl] : []))),
    checkInImageUrl: checkInImageUrl,
    checkOutImageUrl: checkOutImageUrl,
    solutions: solutions,
    closedBy: data.closedBy || data.closedByName || "",
    timestamp: timestamp,
    createdAt: data.createdAt,
    updatedAt: data.updatedAt,
    closedAt: closedAt,
    checkInAt: checkInAt
  };
}

// ฟังก์ชัน Realtime Listener สำหรับดึงรายการเคสทั้งหมด (รองรับหลาย collection และชื่อฟิลด์จากแอปช่าง)
export function subscribeToCases(onDataReceived: (cases: RepairCase[]) => void, onError?: (err: any) => void) {
  const collectionNames = ["repair_cases", "cases", "repairCases", "work_orders", "workOrders"];
  const listData: Record<string, RepairCase[]> = {};
  
  collectionNames.forEach(name => {
    listData[name] = [];
  });

  const emitCombined = () => {
    const map = new Map<string, RepairCase>();
    collectionNames.forEach(name => {
      for (const c of (listData[name] || [])) {
        const existing = map.get(c.caseId);
        if (!existing) {
          map.set(c.caseId, c);
        } else {
          const scoreNew = (c.status === "CLOSED" ? 2 : 0) + (c.checkInImageUrl ? 1 : 0) + (c.checkOutImageUrl ? 1 : 0);
          const scoreExisting = (existing.status === "CLOSED" ? 2 : 0) + (existing.checkInImageUrl ? 1 : 0) + (existing.checkOutImageUrl ? 1 : 0);
          if (scoreNew >= scoreExisting) {
            map.set(c.caseId, c);
          }
        }
      }
    });

    const combined = Array.from(map.values());
    combined.sort((a, b) => (b.timestamp || 0) - (a.timestamp || 0));
    onDataReceived(combined);
  };

  const unsubscribers: (() => void)[] = [];

  collectionNames.forEach(colName => {
    try {
      const colRef = collection(db, colName);
      const unsub = onSnapshot(colRef, (snapshot) => {
        listData[colName] = snapshot.docs.map((docSnap) => parseCaseDoc(docSnap.id, docSnap.data()));
        emitCombined();
      }, (error) => {
        // silent catch
      });
      unsubscribers.push(unsub);
    } catch (e) {
      // ignore
    }
  });

  return () => {
    unsubscribers.forEach(unsub => unsub());
  };
}

// ฟังก์ชันดึงข้อมูลเคสรายเดียวตาม ID
export async function getCaseById(caseId: string): Promise<RepairCase | null> {
  const collectionNames = [CASES_COLLECTION, "cases", "repair_tickets"];
  for (const colName of collectionNames) {
    try {
      const docRef = doc(db, colName, caseId);
      const snap = await getDoc(docRef);
      if (snap.exists()) {
        return parseCaseDoc(snap.id, snap.data());
      }
    } catch (e) {
      console.warn(`Error fetching case from ${colName}:`, e);
    }
  }
  return null;
}

// ฟังก์ชัน Realtime listener สำหรับเคสรายเดียว
export function subscribeToCaseById(
  caseId: string, 
  onUpdate: (caseData: RepairCase | null) => void,
  onError?: (error: any) => void
): () => void {
  const docRef = doc(db, CASES_COLLECTION, caseId);
  return onSnapshot(docRef, (snap) => {
    if (snap.exists()) {
      onUpdate(parseCaseDoc(snap.id, snap.data()));
    } else {
      onUpdate(null);
    }
  }, (err) => {
    console.warn("subscribeToCaseById error:", err);
    if (onError) onError(err);
  });
}

// ฟังก์ชันแก้ไขข้อมูลเคส
export async function updateCaseData(caseId: string, updates: Partial<RepairCase>) {
  const caseRef = doc(db, CASES_COLLECTION, caseId);
  await updateDoc(caseRef, {
    ...updates,
    updatedAt: serverTimestamp()
  });

  await addSystemLog({
    action: "UPDATE_CASE",
    actor: "Web Admin",
    caseId: caseId,
    details: `แก้ไขข้อมูลเคส ${caseId}`
  });
}

// ฟังก์ชันเปลี่ยนช่างผู้รับผิดชอบ (Reassign)
export async function reassignTechnician(caseId: string, newTechId: string, newTechName: string, customerName: string) {
  const caseRef = doc(db, CASES_COLLECTION, caseId);
  await updateDoc(caseRef, {
    technicianId: newTechId,
    technicianName: newTechName,
    updatedAt: serverTimestamp()
  });

  // แจ้งเตือนช่างคนใหม่
  try {
    await addDoc(collection(db, NOTIF_COLLECTION), {
      caseId: caseId,
      technicianId: newTechId,
      customerName: customerName,
      message: `โอนย้ายงานใหม่: ${caseId} (${customerName})`,
      isRead: false,
      timestamp: serverTimestamp()
    });
  } catch (e) {
    console.warn("Notification error:", e);
  }

  await addSystemLog({
    action: "REASSIGN_TECH",
    actor: "Web Admin",
    caseId: caseId,
    technicianId: newTechId,
    technicianName: newTechName,
    details: `เปลี่ยนผู้รับผิดชอบเคส ${caseId} เป็น ${newTechName}`
  });
}

// ฟังก์ชันบันทึก Check-in (ใช้งานทั้งจาก Web Admin และ Mobile Android App)
export async function recordCheckIn(caseId: string, checkInImageUrl: string, techName: string) {
  const caseRef = doc(db, CASES_COLLECTION, caseId);
  await updateDoc(caseRef, {
    checkInImageUrl: checkInImageUrl,
    checkInAt: serverTimestamp(),
    updatedAt: serverTimestamp()
  });

  await addSystemLog({
    action: "TECH_CHECKIN",
    actor: techName || "Technician",
    caseId: caseId,
    details: `ช่าง ${techName} เช็คอินเข้าหน้างานพร้อมแนบภาพหลักฐาน`
  });
}

// ฟังก์ชันปิดงาน (Close Case) พร้อมบันทึกผลการซ่อมและรูปปิดงาน
export async function closeRepairCase(
  caseId: string, 
  solutions: string, 
  checkOutImageUrl: string = "", 
  closedBy: string = "Admin / ช่างผู้รับผิดชอบ"
) {
  const caseRef = doc(db, CASES_COLLECTION, caseId);
  await updateDoc(caseRef, {
    status: "CLOSED",
    solutions: solutions.trim(),
    checkOutImageUrl: checkOutImageUrl,
    closedBy: closedBy,
    closedAt: serverTimestamp(),
    updatedAt: serverTimestamp()
  });

  await addSystemLog({
    action: "CLOSE_CASE",
    actor: closedBy,
    caseId: caseId,
    details: `ปิดเคส ${caseId} เรียบร้อยแล้ว (โดย: ${closedBy})`
  });
}

// ฟังก์ชันเปิดเคสกลับมาดำเนินการใหม่ (Reopen)
export async function reopenCase(caseId: string, actor: string = "Web Admin") {
  const caseRef = doc(db, CASES_COLLECTION, caseId);
  await updateDoc(caseRef, {
    status: "OPEN",
    closedAt: null,
    closedBy: "",
    updatedAt: serverTimestamp()
  });

  await addSystemLog({
    action: "UPDATE_CASE",
    actor: actor,
    caseId: caseId,
    details: `เปิดเคส ${caseId} กลับมาดำเนินการใหม่อีกครั้ง`
  });
}

// ฟังก์ชันลบเคส
export async function deleteRepairCase(caseId: string) {
  const caseRef = doc(db, CASES_COLLECTION, caseId);
  await deleteDoc(caseRef);

  await addSystemLog({
    action: "DELETE_CASE",
    actor: "Web Admin",
    caseId: caseId,
    details: `ลบเคส ${caseId} ออกจากระบบ`
  });
}

// ฟังก์ชันลบเคสตัวอย่าง Dummy ออกจากระบบ
export async function clearAllDummyCases(cases: RepairCase[]) {
  const dummyIds = [
    "CASE-260823001",
    "CASE-260823002",
    "CASE-260823003",
    "CASE-240520001",
    "CASE-240520002",
    "CASE-240520003",
    "CASE-240520004"
  ];
  
  for (const c of cases) {
    if (dummyIds.includes(c.caseId) || c.customerName.includes("คุณสมศรี") || c.customerName.includes("เคเอ็นจี") || c.customerName.includes("บ้านสวนริมคลอง")) {
      await deleteDoc(doc(db, CASES_COLLECTION, c.caseId));
    }
  }

  await addSystemLog({
    action: "DELETE_CASE",
    actor: "Web Admin",
    details: "ล้างข้อมูลเคสตัวอย่าง (Dummy Data) ออกจากระบบทั้งหมด"
  });
}

