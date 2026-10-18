import api from './api';

const UserService = {
  getAll: async () => (await api.get('/users')).data,
  create: async (user) => (await api.post('/users', user)).data,
  updateRole: async (id, role) => (await api.put(`/users/${id}/role`, { role })).data,
};

export default UserService;
