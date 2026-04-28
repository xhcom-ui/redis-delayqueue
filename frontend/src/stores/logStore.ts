import { defineStore } from 'pinia'
import type { ExecutionLog } from '../api/delayQueue'

export const useLogStore = defineStore('delay-log', {
  state: () => ({
    logs: [] as ExecutionLog[],
    eventIds: new Set<string>()
  }),
  actions: {
    add(log: ExecutionLog) {
      // 多个后端实例可能同时广播同一条执行结果，页面按 eventId 做本地去重。
      if (this.eventIds.has(log.eventId)) {
        return
      }
      this.eventIds.add(log.eventId)
      this.logs.unshift(log)
      if (this.logs.length > 200) {
        const removed = this.logs.splice(200)
        removed.forEach((item) => this.eventIds.delete(item.eventId))
      }
    },
    clearLocal() {
      this.logs = []
      this.eventIds.clear()
    }
  }
})
