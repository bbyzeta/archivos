export type UserRole = 'ADMIN' | 'USER';

export interface User {
  id: string;
  username: string;
  fullName: string;
  role: UserRole;
  active: boolean;
  createdAt: string;
}

export interface CreateUserRequest {
  username: string;
  fullName: string;
  password: string;
  role: UserRole;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}
