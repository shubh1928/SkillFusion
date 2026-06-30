export default interface User {
  id: string;
  name: string;
  email: string;
  enabled: boolean;
  image?: string;
  updatedAt?: string;
  createdAt?: string;
  provider: string;
  role: string;
}