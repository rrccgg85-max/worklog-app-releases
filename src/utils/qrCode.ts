import QRCode from "qrcode";

export interface QROptions {
  width?: number;
  margin?: number;
  color?: {
    dark?: string;
    light?: string;
  };
}

/**
 * Returns the absolute URL linking directly to the mobile check-in view for a specific repair case.
 */
export function getMobileCheckInUrl(caseId: string): string {
  if (typeof window === "undefined") return `?view=checkin&caseId=${caseId}`;
  const origin = window.location.origin;
  const pathname = window.location.pathname.replace(/\/$/, "");
  return `${origin}${pathname}?view=checkin&caseId=${encodeURIComponent(caseId)}`;
}

/**
 * Generates a high-resolution QR code (Data URL) for a case that links directly to the mobile check-in view.
 */
export async function generateCaseCheckInQrCode(
  caseId: string,
  options?: QROptions
): Promise<string> {
  const url = getMobileCheckInUrl(caseId);
  try {
    const dataUrl = await QRCode.toDataURL(url, {
      width: options?.width || 320,
      margin: options?.margin ?? 2,
      color: {
        dark: options?.color?.dark || "#0f172a",
        light: options?.color?.light || "#ffffff"
      },
      errorCorrectionLevel: "M"
    });
    return dataUrl;
  } catch (error) {
    console.error("Failed to generate QR code:", error);
    throw error;
  }
}

/**
 * Triggers a download of the QR code image.
 */
export function downloadQrCodeImage(dataUrl: string, filename: string): void {
  const a = document.createElement("a");
  a.href = dataUrl;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
}
