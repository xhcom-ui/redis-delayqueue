import axios from 'axios'

export type QueueType = 'zset' | 'notify' | 'redisson'

export interface DelayMessagePayload {
  queueType: QueueType
  content: string
  delayTime: number
  requestMethod: string
  requestUrl: string
  requestHeaders?: string
  requestBody?: string
  callbackMethod: string
  callbackUrl: string
  callbackHeaders?: string
  callbackBody?: string
}

export interface ExecutionLog {
  eventId: string
  messageId: string
  queueType: QueueType
  content: string
  instanceId: string
  status: string
  requestStatus?: number
  callbackStatus?: number
  errorMessage?: string
  executedAt: number
}

export interface ApiConfig extends DelayMessagePayload {
  id?: number
  name: string
  createdAt?: number
  updatedAt?: number
}

export interface DelayTaskRecord extends DelayMessagePayload {
  id: number
  messageId: string
  createTime: number
  executeTime: number
  status: string
  instanceId?: string
  requestStatus?: number
  requestResponse?: string
  requestError?: string
  callbackStatus?: number
  callbackResponse?: string
  callbackError?: string
  executedAt?: number
  updatedAt: number
}

export async function sendDelayMessage(payload: DelayMessagePayload) {
  const { data } = await axios.post('/delay-queue/send', payload)
  if (data.code !== 0) {
    throw new Error(data.message || '发送失败')
  }
  return data.data
}

export async function clearDelayQueue() {
  const { data } = await axios.post('/delay-queue/clear')
  if (data.code !== 0) {
    throw new Error(data.message || '清空失败')
  }
  return data.data
}

export async function listTasks() {
  const { data } = await axios.get('/delay-queue/tasks')
  if (data.code !== 0) {
    throw new Error(data.message || '查询任务失败')
  }
  return data.data as DelayTaskRecord[]
}

export async function listConfigs() {
  const { data } = await axios.get('/delay-queue/configs')
  if (data.code !== 0) {
    throw new Error(data.message || '查询配置失败')
  }
  return data.data as ApiConfig[]
}

export async function saveConfig(payload: ApiConfig) {
  const { data } = await axios.post('/delay-queue/configs', payload)
  if (data.code !== 0) {
    throw new Error(data.message || '保存配置失败')
  }
  return data.data as ApiConfig
}

export async function deleteConfig(id: number) {
  const { data } = await axios.post(`/delay-queue/configs/${id}/delete`)
  if (data.code !== 0) {
    throw new Error(data.message || '删除配置失败')
  }
  return data.data
}
