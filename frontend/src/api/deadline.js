import client from './client';

export const deadlineApi = {
  getUpcoming: (month, year) =>
    client.get('/deadlines/upcoming', { params: { month, year } }),
};