import api from './api';

// Drop empty filter values so they are not sent as "?status=".
const cleanParams = (params = {}) =>
  Object.fromEntries(Object.entries(params).filter(([, v]) => v !== '' && v != null));

const IncidentService = {
  getAll: async (filters) => (await api.get('/incidents', { params: cleanParams(filters) })).data,
  getById: async (id) => (await api.get(`/incidents/${id}`)).data,
  create: async (incident) => (await api.post('/incidents', incident)).data,
  update: async (id, incident) => (await api.put(`/incidents/${id}`, incident)).data,
  assign: async (id, userId) => (await api.put(`/incidents/${id}/assign`, null, { params: { userId } })).data,
  changeStatus: async (id, status) => (await api.put(`/incidents/${id}/status`, { status })).data,
  remove: async (id) => api.delete(`/incidents/${id}`),
};

export default IncidentService;
