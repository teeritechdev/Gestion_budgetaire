import { Permission } from './permission.model';

export interface Role {
  id?: string;
  code: string;
  libelle: string;
  description?: string;
  permissions?: Permission[];
  createdAt?: string;
  updatedAt?: string;
}

export interface RoleRequestDto {
  role: Partial<Role>;
  permissionIds: string[];
}
