import request from './request'

export const fetchTasks = () => request.get('/tasks')
export const createTask = (data) => request.post('/tasks', data)
export const updateTask = (id, data) => request.put(`/tasks/${encodeURIComponent(id)}`, data)
export const updateTaskStatus = (id, status) => request.put(`/tasks/${encodeURIComponent(id)}/status`, { status })
export const deleteTask = (id) => request.delete(`/tasks/${encodeURIComponent(id)}`)
