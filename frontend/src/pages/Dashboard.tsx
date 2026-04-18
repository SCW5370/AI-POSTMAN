import { useState } from 'react'
import { ChartBarIcon, ArrowTrendingUpIcon, UsersIcon, EnvelopeIcon, ArrowPathIcon, CheckCircleIcon, ClockIcon, ExclamationCircleIcon } from '@heroicons/react/24/outline'
import { apiFetch } from '../utils/api'

type ActionStatus = 'idle' | 'loading' | 'success' | 'error'

/** 轮询异步任务，直到 status 为 success / failed / 超时 */
async function pollTask(
  pollUrl: string,
  intervalMs = 1500,
  maxAttempts = 40,
): Promise<{ status: string; message?: string; [key: string]: unknown }> {
  for (let i = 0; i < maxAttempts; i++) {
    await new Promise(r => setTimeout(r, intervalMs))
    const res = await apiFetch(pollUrl)
    if (!res.ok) throw new Error(`poll failed: ${res.status}`)
    const body = await res.json()
    const taskStatus: string = body?.data?.status ?? body?.status ?? ''
    if (taskStatus === 'success' || taskStatus === 'failed') {
      return body?.data ?? body
    }
  }
  throw new Error('任务超时，请稍后刷新查询结果')
}

const Dashboard: React.FC = () => {
  const [status, setStatus] = useState<{ fetch: ActionStatus; build: ActionStatus; send: ActionStatus }>({
    fetch: 'idle',
    build: 'idle',
    send: 'idle'
  })
  const [statusMsg, setStatusMsg] = useState<{ fetch: string; build: string; send: string }>({
    fetch: '',
    build: '',
    send: '',
  })
  const [userId, setUserId] = useState('')
  const [digestDate, setDigestDate] = useState(new Date().toISOString().split('T')[0])
  const [forceLlm, setForceLlm] = useState(false)
  const [latestDigestId, setLatestDigestId] = useState<string | null>(null)

  const handleFetch = async () => {
    setStatus(prev => ({ ...prev, fetch: 'loading' }))
    setStatusMsg(prev => ({ ...prev, fetch: '提交抓取任务…' }))
    try {
      const res = await apiFetch('/api/admin/fetch-async', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ sourceIds: [] }),
      })
      if (!res.ok) throw new Error(`HTTP ${res.status}`)
      const body = await res.json()
      const taskId: string = body?.data?.taskId ?? body?.taskId
      if (!taskId) throw new Error('未获得 taskId')
      setStatusMsg(prev => ({ ...prev, fetch: `任务已提交 (${taskId})，轮询中…` }))
      const result = await pollTask(`/api/admin/fetch-async/${taskId}`)
      if (result.status === 'failed') throw new Error(result.message as string ?? '抓取失败')
      setStatusMsg(prev => ({ ...prev, fetch: `抓取完成，新条目：${result.savedCount ?? 0}` }))
      setStatus(prev => ({ ...prev, fetch: 'success' }))
    } catch (e: unknown) {
      const msg = e instanceof Error ? e.message : String(e)
      setStatusMsg(prev => ({ ...prev, fetch: msg }))
      setStatus(prev => ({ ...prev, fetch: 'error' }))
    }
  }

  const handleBuild = async () => {
    if (!userId) {
      alert('请输入用户ID')
      return
    }
    setStatus(prev => ({ ...prev, build: 'loading' }))
    setStatusMsg(prev => ({ ...prev, build: '提交构建任务…' }))
    try {
      const res = await apiFetch('/api/admin/digests/build-async', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ userId: Number(userId), digestDate, forceLlm }),
      })
      if (!res.ok) throw new Error(`HTTP ${res.status}`)
      const body = await res.json()
      const taskId: string = body?.data?.taskId ?? body?.taskId
      if (!taskId) throw new Error('未获得 taskId')
      setStatusMsg(prev => ({ ...prev, build: `任务已提交 (${taskId})，轮询中…` }))
      const result = await pollTask(`/api/admin/digests/build-async/${taskId}`)
      if (result.status === 'failed') throw new Error(result.message as string ?? '构建失败')
      const digestId: string | null = (result as { digest?: { id?: unknown } }).digest?.id != null
        ? String((result as { digest?: { id?: unknown } }).digest!.id)
        : null
      if (digestId) setLatestDigestId(digestId)
      setStatusMsg(prev => ({ ...prev, build: `构建完成${digestId ? `，digestId=${digestId}` : ''}` }))
      setStatus(prev => ({ ...prev, build: 'success' }))
    } catch (e: unknown) {
      const msg = e instanceof Error ? e.message : String(e)
      setStatusMsg(prev => ({ ...prev, build: msg }))
      setStatus(prev => ({ ...prev, build: 'error' }))
    }
  }

  const handleSend = async () => {
    if (!latestDigestId) {
      alert('请先构建日报')
      return
    }
    setStatus(prev => ({ ...prev, send: 'loading' }))
    setStatusMsg(prev => ({ ...prev, send: '发送邮件中…' }))
    try {
      const res = await apiFetch(`/api/admin/digests/send/${latestDigestId}`, { method: 'POST' })
      if (!res.ok) {
        const err = await res.text()
        throw new Error(`HTTP ${res.status}: ${err}`)
      }
      setStatusMsg(prev => ({ ...prev, send: '邮件发送成功' }))
      setStatus(prev => ({ ...prev, send: 'success' }))
    } catch (e: unknown) {
      const msg = e instanceof Error ? e.message : String(e)
      setStatusMsg(prev => ({ ...prev, send: msg }))
      setStatus(prev => ({ ...prev, send: 'error' }))
    }
  }

  return (
    <div className="space-y-6">
      {/* 页面标题 */}
      <div className="flex justify-between items-center">
        <h1 className="text-2xl font-bold">仪表盘</h1>
        <div className="flex items-center space-x-2">
          <span className="text-sm text-gray-500 dark:text-gray-400">上次更新: 2026-04-14 10:30</span>
          <button className="p-2 rounded-full hover:bg-gray-100 dark:hover:bg-gray-700">
            <ArrowPathIcon className="w-5 h-5" />
          </button>
        </div>
      </div>

      {/* 系统概览卡片 */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <StatusCard 
          title="系统状态" 
          value="正常" 
          icon={<CheckCircleIcon className="w-6 h-6 text-green-500" />} 
          color="bg-green-50 dark:bg-green-900/20 border-green-200 dark:border-green-800"
        />
        <StatusCard 
          title="今日抓取" 
          value="128 条" 
          icon={<ChartBarIcon className="w-6 h-6 text-blue-500" />} 
          color="bg-blue-50 dark:bg-blue-900/20 border-blue-200 dark:border-blue-800"
        />
        <StatusCard 
          title="活跃用户" 
          value="24" 
          icon={<UsersIcon className="w-6 h-6 text-purple-500" />} 
          color="bg-purple-50 dark:bg-purple-900/20 border-purple-200 dark:border-purple-800"
        />
        <StatusCard 
          title="邮件发送" 
          value="18" 
          icon={<EnvelopeIcon className="w-6 h-6 text-yellow-500" />} 
          color="bg-yellow-50 dark:bg-yellow-900/20 border-yellow-200 dark:border-yellow-800"
        />
      </div>

      {/* 业务流程操作 */}
      <div className="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-200 dark:border-gray-700 p-6">
        <h2 className="text-lg font-semibold mb-6">业务流程操作</h2>
        
        <div className="space-y-4">
          {/* 输入表单 */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">用户 ID</label>
              <input
                type="text"
                value={userId}
                onChange={(e) => setUserId(e.target.value)}
                className="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent dark:bg-gray-700"
                placeholder="输入用户ID"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">日报日期</label>
              <input
                type="date"
                value={digestDate}
                onChange={(e) => setDigestDate(e.target.value)}
                className="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent dark:bg-gray-700"
              />
            </div>
          </div>
          
          <div className="flex items-center">
            <input
              type="checkbox"
              id="forceLlm"
              checked={forceLlm}
              onChange={(e) => setForceLlm(e.target.checked)}
              className="w-4 h-4 text-primary-600 rounded focus:ring-primary-500"
            />
            <label htmlFor="forceLlm" className="ml-2 text-sm text-gray-700 dark:text-gray-300">
              强制 LLM 参与构建
            </label>
          </div>

          {/* 操作按钮 */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            <ActionButton
              label="抓取内容"
              status={status.fetch}
              onClick={handleFetch}
              icon={<ChartBarIcon className="w-5 h-5" />}
            />
            <ActionButton
              label="构建日报"
              status={status.build}
              onClick={handleBuild}
              icon={<ArrowTrendingUpIcon className="w-5 h-5" />}
            />
            <ActionButton
              label="发送邮件"
              status={status.send}
              onClick={handleSend}
              icon={<EnvelopeIcon className="w-5 h-5" />}
            />
          </div>

          {/* 操作状态 */}
          <div className="mt-4">
            <h3 className="text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">操作状态</h3>
            <div className="space-y-2">
              <StatusItem label="抓取状态" status={status.fetch} message={statusMsg.fetch} />
              <StatusItem label="构建状态" status={status.build} message={statusMsg.build} />
              <StatusItem label="发送状态" status={status.send} message={statusMsg.send} />
            </div>
          </div>

          {/* 预览链接 */}
          {latestDigestId && (
            <div className="mt-4 p-4 bg-gray-50 dark:bg-gray-700 rounded-lg">
              <p className="text-sm text-gray-700 dark:text-gray-300 mb-2">日报预览</p>
              <a
                href={`/api/digests/detail/${latestDigestId}/html`}
                target="_blank"
                rel="noopener noreferrer"
                className="text-primary-600 dark:text-primary-400 hover:underline flex items-center space-x-2"
              >
                <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14" />
                </svg>
                <span>查看最新日报</span>
              </a>
            </div>
          )}
        </div>
      </div>

      {/* 最近活动 */}
      <div className="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-200 dark:border-gray-700 p-6">
        <h2 className="text-lg font-semibold mb-4">最近活动</h2>
        <div className="space-y-3">
          <ActivityItem time="10:30" action="系统自动抓取内容" status="success" />
          <ActivityItem time="09:15" action="用户 123 构建日报" status="success" />
          <ActivityItem time="08:00" action="系统自动发送邮件" status="success" />
          <ActivityItem time="00:00" action="系统夜间预构建" status="success" />
        </div>
      </div>
    </div>
  )
}

interface StatusCardProps {
  title: string
  value: string
  icon: React.ReactNode
  color: string
}

const StatusCard: React.FC<StatusCardProps> = ({ title, value, icon, color }) => {
  return (
    <div className={`${color} rounded-xl p-6 border`}>
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-sm font-medium text-gray-600 dark:text-gray-400">{title}</h3>
        {icon}
      </div>
      <p className="text-2xl font-bold">{value}</p>
    </div>
  )
}

interface ActionButtonProps {
  label: string
  status: ActionStatus
  onClick: () => void
  icon: React.ReactNode
}

const ActionButton: React.FC<ActionButtonProps> = ({ label, status, onClick, icon }) => {
  const isLoading = status === 'loading'
  const isSuccess = status === 'success'
  const isError = status === 'error'

  return (
    <button
      onClick={onClick}
      disabled={isLoading}
      className={`w-full flex items-center justify-center space-x-2 px-4 py-3 rounded-lg transition-colors ${isLoading
        ? 'bg-gray-200 dark:bg-gray-700 text-gray-500 dark:text-gray-400 cursor-not-allowed'
        : isSuccess
        ? 'bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-300'
        : isError
        ? 'bg-red-100 dark:bg-red-900/30 text-red-700 dark:text-red-300'
        : 'bg-primary-500 hover:bg-primary-600 text-white'
        }`}
    >
      {isLoading ? (
        <svg className="animate-spin -ml-1 mr-2 h-4 w-4 text-gray-500 dark:text-gray-400" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
          <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
          <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
        </svg>
      ) : isSuccess ? (
        <CheckCircleIcon className="w-4 h-4" />
      ) : isError ? (
        <ExclamationCircleIcon className="w-4 h-4" />
      ) : (
        icon
      )}
      <span>{label}</span>
    </button>
  )
}

interface StatusItemProps {
  label: string
  status: ActionStatus
  message?: string
}

const StatusItem: React.FC<StatusItemProps> = ({ label, status, message }) => {
  const getStatusColor = () => {
    switch (status) {
      case 'loading': return 'text-yellow-500'
      case 'success': return 'text-green-500'
      case 'error':   return 'text-red-500'
      default:        return 'text-gray-500'
    }
  }

  const getStatusText = () => {
    switch (status) {
      case 'loading': return '执行中'
      case 'success': return '成功'
      case 'error':   return '失败'
      default:        return '未执行'
    }
  }

  return (
    <div className="flex flex-col gap-0.5">
      <div className="flex items-center justify-between">
        <span className="text-sm text-gray-700 dark:text-gray-300">{label}</span>
        <span className={`text-sm font-medium flex items-center gap-1 ${getStatusColor()}`}>
          {status === 'loading' && (
            <svg className="animate-spin h-4 w-4" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
          )}
          {getStatusText()}
        </span>
      </div>
      {message && (
        <p className={`text-xs pl-0.5 ${status === 'error' ? 'text-red-400' : 'text-gray-400 dark:text-gray-500'}`}>
          {message}
        </p>
      )}
    </div>
  )
}

interface ActivityItemProps {
  time: string
  action: string
  status: 'success' | 'error' | 'warning'
}

const ActivityItem: React.FC<ActivityItemProps> = ({ time, action, status }) => {
  const getStatusIcon = () => {
    switch (status) {
      case 'success':
        return <CheckCircleIcon className="w-4 h-4 text-green-500" />
      case 'error':
        return <ExclamationCircleIcon className="w-4 h-4 text-red-500" />
      case 'warning':
        return <ClockIcon className="w-4 h-4 text-yellow-500" />
      default:
        return null
    }
  }

  return (
    <div className="flex items-center space-x-3">
      <div className="w-8 h-8 rounded-full bg-gray-100 dark:bg-gray-700 flex items-center justify-center">
        {getStatusIcon()}
      </div>
      <div className="flex-1">
        <p className="text-sm font-medium">{action}</p>
        <p className="text-xs text-gray-500 dark:text-gray-400">{time}</p>
      </div>
    </div>
  )
}

export default Dashboard
