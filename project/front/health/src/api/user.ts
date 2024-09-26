import axios from 'axios';

const baseUrl = import.meta.env.VITE_APP_BASE_URL;

export const getUserDetail = async (userId: number) => {
  const response = await axios.get(`${baseUrl}/users/:userId`, { params: { userId } });
  return response;
};
