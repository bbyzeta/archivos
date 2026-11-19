export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  userName: string;
  idUsuario: number;
  rol: string;
}