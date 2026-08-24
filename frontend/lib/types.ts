export type LinkStatus = "ACTIVE" | "DISABLED" | "EXPIRED";
export type CampaignStatus = "ACTIVE" | "PAUSED" | "ARCHIVED";
export type DomainVerificationStatus = "PENDING" | "VERIFIED" | "FAILED";
export type QRFormat = "PNG" | "SVG" | "PDF";

export interface UserSummary {
  id: string;
  email: string;
  name: string;
  role: string;
  verified: boolean;
  createdAt: string;
}

export interface Campaign {
  id: string;
  name: string;
  description: string | null;
  status: CampaignStatus;
  createdAt: string;
  updatedAt: string;
}

export interface LinkItem {
  id: string;
  shortCode: string;
  originalUrl: string;
  title: string | null;
  status: LinkStatus;
  campaignId: string | null;
  clickCount: number;
  expiresAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface QRCodeItem {
  id: string;
  linkId: string;
  format: QRFormat;
  foregroundColor: string;
  backgroundColor: string;
  size: number;
  createdAt: string;
  imageUrl: string;
}

export interface DomainItem {
  id: string;
  domain: string;
  verificationStatus: DomainVerificationStatus;
  verifiedAt: string | null;
  createdAt: string;
}
