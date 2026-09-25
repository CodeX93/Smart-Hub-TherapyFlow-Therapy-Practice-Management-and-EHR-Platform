export interface OptionValue {
  id: string;
  label: string;
  value: string;
  isDefault?: boolean;
  isSystem?: boolean;
  isActive?: boolean;
}

export interface OptionCategory {
  id: string;
  name: string;
  code: string;
  description?: string;
  options: OptionValue[];
  isCustom?: boolean;
  isActive?: boolean;
}

export interface ServicePrice {
  id: string;
  code: string;
  name: string;
  duration: string;
  price: string;
}

export interface ServiceVisibility extends ServicePrice {
  id: string;
  therapistVisible: boolean;
  clientPortalVisible: boolean;
}

export interface TherapyRoom {
  id: string;
  number: string;
  name: string;
  capacity?: number;
  equipment?: string;
  roomType?: "PHYSICAL" | "VIRTUAL";
  isActive: boolean;
}

export interface PracticeConfig {
  name: string;
  subtitle: string;
  description: string;
  address: string;
  phone: string;
  email: string;
  website: string;
  timezone: string;
}
