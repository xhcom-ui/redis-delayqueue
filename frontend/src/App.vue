<template>
  <a-config-provider :theme="theme">
    <main class="page-shell">
      <section class="workspace">
        <header class="topbar">
          <div>
            <h1>Redis 延时队列</h1>
            <p>延时触发外部接口，到期后执行回调，任务记录落 MySQL。</p>
          </div>
          <a-tag :color="connected ? 'success' : 'error'">
            {{ connected ? 'SSE 已连接' : 'SSE 未连接' }}
          </a-tag>
        </header>

        <div class="content-grid">
          <section class="panel composer">
            <div class="panel-head">
              <div>
                <div class="panel-title">任务配置</div>
                <span class="muted">Headers 请填写 JSON 对象。</span>
              </div>
              <a-button :loading="loadingConfigs" @click="loadConfigs">
                <template #icon><ReloadOutlined /></template>
              </a-button>
            </div>

            <div class="quick-add">
              <div class="quick-add-head">
                <div>
                  <div class="quick-add-title">快速添加</div>
                  <span class="muted">选择队列和延迟时间，到期后页面会收到 SSE 推送。</span>
                </div>
              </div>
              <div class="quick-add-controls">
                <div class="quick-field">
                  <span class="field-label">队列方案</span>
                  <a-segmented v-model:value="quickForm.queueType" :options="queueOptions" block />
                </div>
                <div class="quick-inline">
                  <div class="quick-field">
                    <span class="field-label">延迟时间</span>
                    <a-input-number
                      v-model:value="quickForm.delayTime"
                      :min="1"
                      :max="86400"
                      addon-after="秒"
                      class="quick-delay"
                    />
                  </div>
                  <a-button class="quick-submit" type="primary" :loading="quickSending" @click="quickSend">
                    <template #icon><PlusOutlined /></template>
                    添加任务
                  </a-button>
                </div>
              </div>
            </div>

            <a-form layout="vertical">
              <a-form-item label="已保存配置">
                <a-select
                  v-model:value="selectedConfigId"
                  placeholder="选择配置"
                  allow-clear
                  @change="applyConfig"
                >
                  <a-select-option v-for="item in configs" :key="item.id" :value="item.id">
                    {{ item.name }}
                  </a-select-option>
                </a-select>
              </a-form-item>
              <a-form-item label="配置名称">
                <a-input v-model:value="configName" placeholder="用于保存当前接口配置" />
              </a-form-item>
              <a-form-item label="队列方案">
                <a-segmented v-model:value="form.queueType" :options="queueOptions" block />
              </a-form-item>
              <a-form-item label="任务说明">
                <a-input v-model:value="form.content" :maxlength="120" show-count />
              </a-form-item>
              <a-form-item label="延迟秒数">
                <a-input-number v-model:value="form.delayTime" :min="1" :max="86400" class="full-width" />
              </a-form-item>

              <div class="sub-title">外部接口</div>
              <a-form-item label="请求方法">
                <a-segmented v-model:value="form.requestMethod" :options="methodOptions" block />
              </a-form-item>
              <a-form-item label="请求 URL">
                <a-input v-model:value="form.requestUrl" placeholder="https://example.com/api/run" />
              </a-form-item>
              <a-form-item label="请求 Headers">
                <a-textarea v-model:value="form.requestHeaders" :rows="3" placeholder='{"Authorization":"Bearer token"}' />
              </a-form-item>
              <a-form-item label="请求 Body">
                <a-textarea v-model:value="form.requestBody" :rows="4" placeholder='{"orderId":"1001"}' />
              </a-form-item>

              <div class="sub-title">回调接口</div>
              <a-form-item label="回调方法">
                <a-segmented v-model:value="form.callbackMethod" :options="methodOptions" block />
              </a-form-item>
              <a-form-item label="回调 URL">
                <a-input v-model:value="form.callbackUrl" placeholder="https://example.com/api/callback" />
              </a-form-item>
              <a-form-item label="回调 Headers">
                <a-textarea v-model:value="form.callbackHeaders" :rows="3" placeholder='{"Content-Type":"application/json"}' />
              </a-form-item>
              <a-form-item label="回调 Body">
                <a-textarea v-model:value="form.callbackBody" :rows="4" placeholder="为空时自动回传任务执行结果" />
              </a-form-item>

              <div class="actions">
                <a-button type="primary" :loading="sending" @click="send">
                  <template #icon><SendOutlined /></template>
                  发送
                </a-button>
                <a-button :loading="savingConfig" @click="persistConfig">
                  <template #icon><SaveOutlined /></template>
                  保存配置
                </a-button>
                <a-button danger :disabled="!selectedConfigId" @click="removeConfig">
                  <template #icon><DeleteOutlined /></template>
                  删除配置
                </a-button>
                <a-button :loading="clearing" @click="clearRemote">清空队列</a-button>
              </div>
            </a-form>
          </section>

          <section class="right-column">
            <section class="panel log-panel">
              <div class="panel-head">
                <div>
                  <div class="panel-title">实时执行日志</div>
                  <span class="muted">多实例环境下按 eventId 去重。</span>
                </div>
                <a-button @click="logStore.clearLocal">
                  <template #icon><ClearOutlined /></template>
                  清空日志
                </a-button>
              </div>

              <a-empty v-if="logStore.logs.length === 0" description="暂无执行日志" />
              <a-list v-else class="logs" :data-source="logStore.logs">
                <template #renderItem="{ item }">
                  <a-list-item>
                    <div class="log-row">
                      <a-tag>{{ getQueueLabel(item.queueType) }}</a-tag>
                      <div class="log-main">
                        <div class="log-content">{{ item.content }}</div>
                        <div class="log-meta">
                          <span>{{ item.status }}</span>
                          <span>请求 {{ item.requestStatus || '-' }}</span>
                          <span>回调 {{ item.callbackStatus || '-' }}</span>
                          <span>{{ formatTime(item.executedAt) }}</span>
                          <span>实例 {{ item.instanceId }}</span>
                        </div>
                        <div v-if="item.errorMessage" class="error-line">{{ item.errorMessage }}</div>
                      </div>
                    </div>
                  </a-list-item>
                </template>
              </a-list>
            </section>

            <section class="panel task-panel">
              <div class="panel-head">
                <div>
                  <div class="panel-title">MySQL 任务记录</div>
                  <span class="muted">显示最近 100 条。</span>
                </div>
                <a-button :loading="loadingTasks" @click="loadTasks">
                  <template #icon><ReloadOutlined /></template>
                  刷新
                </a-button>
              </div>
              <a-table
                :data-source="tasks"
                :columns="taskColumns"
                :pagination="{ pageSize: 6 }"
                size="small"
                row-key="messageId"
              />
            </section>
          </section>
        </div>
      </section>
    </main>
  </a-config-provider>
</template>

<script setup lang="ts">
import { h, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import dayjs from 'dayjs'
import { message, Tag } from 'ant-design-vue'
import { ClearOutlined, DeleteOutlined, PlusOutlined, ReloadOutlined, SaveOutlined, SendOutlined } from '@ant-design/icons-vue'
import {
  clearDelayQueue,
  deleteConfig,
  listConfigs,
  listTasks,
  saveConfig,
  sendDelayMessage,
  type ApiConfig,
  type DelayMessagePayload,
  type DelayTaskRecord,
  type QueueType
} from './api/delayQueue'
import { useLogStore } from './stores/logStore'

const theme = {
  token: {
    colorPrimary: '#111827',
    colorTextBase: '#1F2937',
    colorTextSecondary: '#6B7280',
    colorBorder: '#E5E7EB',
    borderRadius: 6
  }
}

const queueOptions = [
  { label: 'ZSET 轮询', value: 'zset' },
  { label: '过期通知', value: 'notify' },
  { label: 'Redisson', value: 'redisson' }
]
const methodOptions = ['GET', 'POST', 'PUT', 'PATCH', 'DELETE']
const queueLabel: Record<QueueType, string> = { zset: 'ZSET', notify: 'Notify', redisson: 'Redisson' }

const form = reactive<DelayMessagePayload>({
  queueType: 'redisson',
  content: '延时外部接口任务',
  delayTime: 5,
  requestMethod: 'POST',
  requestUrl: '',
  requestHeaders: '{"Content-Type":"application/json"}',
  requestBody: '{}',
  callbackMethod: 'POST',
  callbackUrl: '',
  callbackHeaders: '{"Content-Type":"application/json"}',
  callbackBody: ''
})

const quickForm = reactive({
  queueType: 'redisson' as QueueType,
  delayTime: 5
})

const logStore = useLogStore()
const configs = ref<ApiConfig[]>([])
const tasks = ref<DelayTaskRecord[]>([])
const selectedConfigId = ref<number>()
const configName = ref('')
const sending = ref(false)
const quickSending = ref(false)
const clearing = ref(false)
const savingConfig = ref(false)
const loadingConfigs = ref(false)
const loadingTasks = ref(false)
const connected = ref(false)
let eventSource: EventSource | null = null

const taskColumns = [
  { title: '内容', dataIndex: 'content', ellipsis: true },
  { title: '状态', dataIndex: 'status', customRender: ({ text }: { text: string }) => h(Tag, { color: statusColor(text) }, () => text) },
  { title: '请求', dataIndex: 'requestStatus' },
  { title: '回调', dataIndex: 'callbackStatus' },
  { title: '实例', dataIndex: 'instanceId', ellipsis: true },
  { title: '创建时间', dataIndex: 'createTime', customRender: ({ text }: { text: number }) => formatTime(text) }
]

function connectSse() {
  const clientId = `${Date.now()}-${Math.random().toString(16).slice(2)}`
  eventSource = new EventSource(`/delay-queue/sse/${clientId}`)
  eventSource.onopen = () => {
    connected.value = true
  }
  eventSource.onerror = () => {
    connected.value = false
  }
  // 后端所有实例通过 Redis Pub/Sub 汇总事件，前端只监听 delay-log 并刷新任务表。
  eventSource.addEventListener('delay-log', (event) => {
    logStore.add(JSON.parse((event as MessageEvent).data))
    loadTasks()
  })
}

async function send() {
  if (!form.requestUrl.trim()) {
    message.warning('请填写外部接口 URL')
    return
  }
  if (!form.callbackUrl.trim()) {
    message.warning('请填写回调 URL')
    return
  }
  if (!validateJsonField(form.requestHeaders, '请求 Headers') || !validateJsonField(form.callbackHeaders, '回调 Headers')) {
    return
  }
  sending.value = true
  try {
    await sendDelayMessage(form)
    await loadTasks()
    message.success('发送成功')
  } catch (error) {
    message.error(error instanceof Error ? error.message : '发送失败')
  } finally {
    sending.value = false
  }
}

async function quickSend() {
  quickSending.value = true
  // 快速添加用于演示完整链路：延迟到期后调用本项目 mock 外部接口，再调用 mock 回调接口。
  const apiBase = (import.meta.env.VITE_API_TARGET || window.location.origin).replace(/\/$/, '')
  const payload: DelayMessagePayload = {
    queueType: quickForm.queueType,
    delayTime: quickForm.delayTime,
    content: `快速延时任务 ${quickForm.delayTime} 秒`,
    requestMethod: 'POST',
    requestUrl: `${apiBase}/mock/external`,
    requestHeaders: '{"Content-Type":"application/json"}',
    requestBody: JSON.stringify({
      source: 'quick-add',
      queueType: quickForm.queueType,
      delayTime: quickForm.delayTime
    }),
    callbackMethod: 'POST',
    callbackUrl: `${apiBase}/mock/callback`,
    callbackHeaders: '{"Content-Type":"application/json"}',
    callbackBody: ''
  }

  try {
    await sendDelayMessage(payload)
    await loadTasks()
    message.success(`已添加，约 ${quickForm.delayTime} 秒后推送到实时日志`)
  } catch (error) {
    message.error(error instanceof Error ? error.message : '添加失败')
  } finally {
    quickSending.value = false
  }
}

async function persistConfig() {
  if (!configName.value.trim()) {
    message.warning('请填写配置名称')
    return
  }
  if (!form.requestUrl.trim()) {
    message.warning('请填写外部接口 URL')
    return
  }
  if (!form.callbackUrl.trim()) {
    message.warning('请填写回调 URL')
    return
  }
  if (!validateJsonField(form.requestHeaders, '请求 Headers') || !validateJsonField(form.callbackHeaders, '回调 Headers')) {
    return
  }
  savingConfig.value = true
  try {
    const saved = await saveConfig({ ...form, id: selectedConfigId.value, name: configName.value })
    selectedConfigId.value = saved.id
    await loadConfigs()
    message.success('配置已保存')
  } catch (error) {
    message.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    savingConfig.value = false
  }
}

async function removeConfig() {
  if (!selectedConfigId.value) return
  try {
    await deleteConfig(selectedConfigId.value)
    selectedConfigId.value = undefined
    configName.value = ''
    await loadConfigs()
    message.success('配置已删除')
  } catch (error) {
    message.error(error instanceof Error ? error.message : '删除失败')
  }
}

function applyConfig(id?: number) {
  const item = configs.value.find((config) => config.id === id)
  if (!item) return
  configName.value = item.name
  Object.assign(form, {
    queueType: item.queueType,
    content: item.content,
    delayTime: item.delayTime,
    requestMethod: item.requestMethod,
    requestUrl: item.requestUrl,
    requestHeaders: item.requestHeaders || '',
    requestBody: item.requestBody || '',
    callbackMethod: item.callbackMethod,
    callbackUrl: item.callbackUrl || '',
    callbackHeaders: item.callbackHeaders || '',
    callbackBody: item.callbackBody || ''
  })
}

async function clearRemote() {
  clearing.value = true
  try {
    await clearDelayQueue()
    logStore.clearLocal()
    await loadTasks()
    message.success('已清空')
  } catch (error) {
    message.error(error instanceof Error ? error.message : '清空失败')
  } finally {
    clearing.value = false
  }
}

async function loadConfigs() {
  loadingConfigs.value = true
  try {
    configs.value = await listConfigs()
  } catch (error) {
    message.error(error instanceof Error ? error.message : '配置加载失败')
  } finally {
    loadingConfigs.value = false
  }
}

async function loadTasks() {
  loadingTasks.value = true
  try {
    tasks.value = await listTasks()
  } catch (error) {
    message.error(error instanceof Error ? error.message : '任务加载失败')
  } finally {
    loadingTasks.value = false
  }
}

function formatTime(value?: number) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm:ss') : '-'
}

function getQueueLabel(value: QueueType) {
  return queueLabel[value]
}

function statusColor(value: string) {
  if (value === 'SUCCESS') return 'success'
  if (value === 'PENDING') return 'default'
  if (value === 'RUNNING') return 'processing'
  return 'error'
}

function validateJsonField(value: string | undefined, label: string) {
  if (!value || !value.trim()) {
    return true
  }
  try {
    JSON.parse(value)
    return true
  } catch {
    message.warning(`${label} 不是合法 JSON`)
    return false
  }
}

onMounted(() => {
  connectSse()
  loadConfigs()
  loadTasks()
})
onBeforeUnmount(() => eventSource?.close())
</script>
