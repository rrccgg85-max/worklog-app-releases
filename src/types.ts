export type CasePriority = "ด่วนที่สุด" | "ด่วน" | "ปกติ";
export type CaseStatus = "OPEN" | "CLOSED";

export interface RepairCase {
  // --- การระบุเอกสาร (Document ID = caseId เสมอ) ---
  caseId: string;
  
  // --- ข้อมูลลูกค้า & สถานที่ ---
  customerName: string;
  phone: string;
  address: string;
  latitude?: number;
  longitude?: number;
  details: string;
  category: string;
  priority: CasePriority;
  
  // --- ช่างที่ได้รับมอบหมาย ---
  technicianId: string;
  technicianName: string;
  
  // --- สถานะเคส ---
  status: CaseStatus;
  
  // --- ช่องทางเปิดเคส (Web / App) ---
  createdSource?: "WEB" | "APP";
  
  // --- รูปภาพหลักฐาน & สื่อ ---
  imageUrl?: string;
  imageUrls?: string[];
  checkInImageUrl?: string;
  checkOutImageUrl?: string;
  
  // --- ข้อมูลสรุปปิดงาน ---
  solutions?: string;
  closedBy?: string;
  
  // --- เวลา & Timestamp ---
  timestamp?: number | any;
  createdAt?: any;
  updatedAt?: any;
  closedAt?: any;
  checkInAt?: any;
}

export interface TechUser {
  id: string;                  // e.g. "TECH-001"
  name: string;                // e.g. "ช่างสมชาย เจริญการช่าง"
  email: string;
  phone: string;
  pin: string;                 // 6-digit PIN for Android app
  role: "technician" | "admin";
  isActive: boolean;
  skills?: string[];
  createdAt?: any;
  activeCaseCount?: number;
}

export interface NotificationDoc {
  id?: string;
  caseId: string;
  technicianId: string;
  customerName: string;
  message: string;
  isRead: boolean;
  timestamp: any;
}

export interface SystemLog {
  id?: string;
  action: "CREATE_CASE" | "UPDATE_CASE" | "REASSIGN_TECH" | "CLOSE_CASE" | "DELETE_CASE" | "ADD_TECH" | "UPDATE_TECH" | "DELETE_TECH" | "TECH_CHECKIN" | "TECH_CLOSE";
  caseId?: string;
  technicianId?: string;
  technicianName?: string;
  details: string;
  actor: string;
  timestamp: any;
}

export interface CaseFilterState {
  searchQuery: string;
  status: "ALL" | CaseStatus;
  priority: "ALL" | CasePriority;
  category: string;
  technicianId: string;
  dateRange: "ALL" | "TODAY" | "THIS_WEEK" | "THIS_MONTH";
}

export const REPAIR_CATEGORIES = [
  "ส่งสินค้า",
  "ซ่อมบำรุง",
  "ติดตั้ง",
  "บริการลูกค้า",
  "งานทั่วไป"
] as const;

