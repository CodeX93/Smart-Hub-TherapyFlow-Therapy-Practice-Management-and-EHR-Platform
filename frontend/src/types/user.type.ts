type UserRole = 'super_admin' | 'hospital_staff' | 'patient';

export interface MenuItem {
  id: string;
  label: string;
  icon: React.ReactNode;
  path: string;
  badge?: number;
  children?: MenuItem[];
}

export interface User {
  name: string;
  role: UserRole;
  email: string;
  avatar: string;
  specialty?: string;
}

export const userData = {
    name: "John Doe",
    role: "super_admin" as UserRole,
    email: "john.doe@example.com",
    avatar: "/avatars/john_doe.png",
}

export const userStaffData = {
    name: "John Doe",
    role: "hospital_staff" as UserRole,
    email: "john.doe@example.com",
    avatar: "/avatars/john_doe.png",
}

export const patientData = {
    name: "Jane Smith",
    role: "patient" as UserRole,
    email: "jane.smith@example.com",
    avatar: "/avatars/jane_smith.png",
}