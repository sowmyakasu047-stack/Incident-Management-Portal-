import api from './api';

const AiService = {
  status: async () => (await api.get('/ai/status')).data,
  suggest: async (title, description) => (await api.post('/ai/suggest', { title, description })).data,
  similar: async (title, description) => (await api.post('/ai/similar', { title, description })).data,
  summarize: async (id) => (await api.post(`/ai/incidents/${id}/summary`)).data,
};

export default AiService;
