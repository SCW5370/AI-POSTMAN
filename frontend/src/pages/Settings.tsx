import React, { useState, useEffect } from 'react'
import { ChevronDownIcon, ChevronUpIcon, CheckIcon, ExclamationCircleIcon } from '@heroicons/react/24/outline'
import { getCurrentUserId, setCurrentUserId } from '../utils/currentUser'
import { apiFetch } from '../utils/api'
import EmailTestSection from '../components/EmailTestSection'
import GenerateEmailSection from '../components/GenerateEmailSection'

const USER_SETTINGS_KEY = 'userSettings'
const DEFAULT_SETTINGS = {
  email: '3276532080@qq.com',
  deliveryTime: '08:00',
  deliveryMode: 'BALANCED',
  deliveryFrequency: 'daily',
  sourceUrl: '',
  cacheMinutes: '60'
}

const readErrorMessage = async (response: Response, fallback: string) => {
  const result = await response.json().catch(() => null)
  return result?.message || fallback
}

const Settings: React.FC = () => {
  const [currentUserId, setCurrentUserIdState] = useState(getCurrentUserId())
  const [activeTab, setActiveTab] = useState('general')
  const [showAdvanced, setShowAdvanced] = useState(false)
  const [formData, setFormData] = useState(() => {
    const savedSettings = localStorage.getItem(USER_SETTINGS_KEY)
    if (!savedSettings) {
      return DEFAULT_SETTINGS
    }
    try {
      return { ...DEFAULT_SETTINGS, ...JSON.parse(savedSettings) }
    } catch {
      return DEFAULT_SETTINGS
    }
  })
  const [saved, setSaved] = useState(false)
  const [saveStatus, setSaveStatus] = useState<{ success: boolean; message: string } | null>(null)
  const [testEmailStatus, setTestEmailStatus] = useState<{ success: boolean; message: string } | null>(null)
  const [generateEmailStatus, setGenerateEmailStatus] = useState<{ success: boolean; message: string } | null>(null)
  const [generateEmailProgress, setGenerateEmailProgress] = useState<{ status: string; message: string } | null>(null)
  const [generateEmailTaskId, setGenerateEmailTaskId] = useState<string | null>(null)
  const [lastBuildMeta, setLastBuildMeta] = useState<{ llmUsed: boolean; strategy: string } | null>(null)

  // 从后端获取用户设置并同步到本地存储
  useEffect(() => {
    const fetchSettings = async () => {
      const savedSettings = localStorage.getItem(USER_SETTINGS_KEY)
      let localSettings = DEFAULT_SETTINGS
      if (savedSettings) {
        try {
          localSettings = { ...DEFAULT_SETTINGS, ...JSON.parse(savedSettings) }
          setFormData(localSettings)
        } catch (error) {
          console.error('解析本地设置失败:', error)
        }
      }

      try {
        const [preferenceResponse, userResponse] = await Promise.all([
          apiFetch(`/api/preferences/${currentUserId}`),
          apiFetch(`/api/users/${currentUserId}`)
        ])

        const nextSettings = { ...localSettings }
        if (userResponse.ok) {
          const userResult = await userResponse.json()
          if (userResult.data?.email) {
            nextSettings.email = userResult.data.email
          }
        }

        if (preferenceResponse.ok) {
          const result = await preferenceResponse.json()
          if (result.data) {
            nextSettings.deliveryTime = result.data.deliveryTime || nextSettings.deliveryTime
            nextSettings.deliveryMode = result.data.deliveryMode || nextSettings.deliveryMode
            nextSettings.deliveryFrequency = result.data.deliveryFrequency || nextSettings.deliveryFrequency
          }
        }
        setFormData(nextSettings)
        localStorage.setItem(USER_SETTINGS_KEY, JSON.stringify(nextSettings))
      } catch (error) {
        console.error('获取设置失败:', error)
      }
    }
    fetchSettings()
  }, [currentUserId])

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target
    const newFormData = { ...formData, [name]: value }
    setFormData(newFormData)
    localStorage.setItem(USER_SETTINGS_KEY, JSON.stringify(newFormData))
  }

  const handleSave = async () => {
    try {
      localStorage.setItem(USER_SETTINGS_KEY, JSON.stringify(formData))
      setSaveStatus(null)

      const userResponse = await apiFetch('/api/users', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          email: formData.email,
          displayName: null,
          timezone: 'Asia/Shanghai'
        })
      })

      if (!userResponse.ok) {
        const userResult = await userResponse.json().catch(() => null)
        throw new Error(userResult?.message || '邮箱保存到后端失败')
      }
      const userResult = await userResponse.json()
      const resolvedUserId = userResult?.data?.id
      if (!resolvedUserId) {
        throw new Error('邮箱保存成功，但未返回有效用户ID')
      }
      setCurrentUserId(resolvedUserId)
      setCurrentUserIdState(resolvedUserId)
      
      const preferenceResponse = await apiFetch(`/api/preferences/${resolvedUserId}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          deliveryMode: formData.deliveryMode,
          deliveryTime: formData.deliveryTime,
          deliveryFrequency: formData.deliveryFrequency
        })
      })
      
      if (!preferenceResponse.ok) {
        const preferenceResult = await preferenceResponse.json().catch(() => null)
        throw new Error(preferenceResult?.message || '偏好设置保存失败')
      }

      console.log('保存设置:', formData)
      setSaved(true)
      setSaveStatus({ success: true, message: '邮箱、发送时间、发送模式和发送频率已保存' })
      setTimeout(() => setSaved(false), 3000)
      setTimeout(() => setSaveStatus(null), 5000)
    } catch (error) {
      console.error('保存设置失败:', error)
      setSaved(false)
      setSaveStatus({ success: false, message: `保存失败: ${error instanceof Error ? error.message : '未知错误'}` })
      setTimeout(() => setSaveStatus(null), 5000)
    }
  }

  const handleTestEmail = async () => {
    try {
      // 调用后端的测试邮件接口
      console.log('正在发送测试邮件...')
      const response = await apiFetch('/api/health/test-email', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          to: formData.email
        })
      })

      const result = await response.json()
      if (result.success) {
        setTestEmailStatus({ success: true, message: result.message || '测试邮件已成功发送，请检查您的邮箱' })
      } else {
        setTestEmailStatus({ success: false, message: `邮件发送失败: ${result.message || '未知错误'}` })
      }
    } catch (error) {
      console.error('测试邮件发送失败:', error)
      setTestEmailStatus({ success: false, message: '邮件发送失败: 网络错误或后端服务未启动' })
    }
    setTimeout(() => setTestEmailStatus(null), 5000)
  }

  const handleGenerateEmail = async (forceLlm = false) => {
    try {
      // 重置状态
      setGenerateEmailStatus(null)
      setGenerateEmailProgress({ status: 'initializing', message: '开始生成邮件内容...' })
      setGenerateEmailTaskId(null)
      setLastBuildMeta(null)

      // 1. 调用异步构建接口
      console.log('正在初始化邮件生成任务...')
      const buildResponse = await apiFetch('/api/admin/digests/build-async', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          userId: currentUserId,
          digestDate: new Date().toISOString().split('T')[0],
          forceLlm
        })
      })

      if (!buildResponse.ok) {
        const message = await readErrorMessage(buildResponse, `HTTP error! status: ${buildResponse.status}`)
        throw new Error(message)
      }

      const buildResult = await buildResponse.json()
      console.log('构建任务创建结果:', buildResult)
      
      if (!buildResult.success || !buildResult.data) {
        throw new Error(`任务创建失败: ${buildResult.message || '未知错误'}`)
      }

      const taskId = buildResult.data.taskId
      setGenerateEmailTaskId(taskId)
      setGenerateEmailProgress({ status: 'building', message: '正在生成邮件内容...' })

      // 轮询开始时间
      const pollingStartTime = Date.now()
      // 轮询超时时间（4分钟）：强制 LLM 模式下可能超过 60 秒
      const pollingTimeout = 240000

      // 2. 轮询任务状态
      const checkTaskStatus = async () => {
        // 检查是否超时
        if (Date.now() - pollingStartTime > pollingTimeout) {
          console.error('任务状态检查超时')
          setGenerateEmailStatus({ success: false, message: '邮件处理超时（超过 4 分钟），任务可能仍在后台运行，请稍后刷新查看结果' })
          setGenerateEmailProgress(null)
          setGenerateEmailTaskId(null)
          
          // 5秒后清除状态
          setTimeout(() => {
            setGenerateEmailStatus(null)
          }, 5000)
          return
        }
        try {
          const statusResponse = await apiFetch(`/api/admin/digests/build-async/${taskId}`)
          
          if (!statusResponse.ok) {
            const message = await readErrorMessage(statusResponse, `HTTP error! status: ${statusResponse.status}`)
            throw new Error(message)
          }

          const statusResult = await statusResponse.json()
          console.log('任务状态:', statusResult)
          console.log('任务状态数据:', statusResult.data)
          
          if (!statusResult.success || !statusResult.data) {
            throw new Error(`获取任务状态失败: ${statusResult.message || '未知错误'}`)
          }

          const taskStatus = statusResult.data.status
          const normalizedTaskStatus = String(taskStatus || '').toLowerCase()
          const taskMessage = statusResult.data.message || '处理中...'
          const digest = statusResult.data.digest

          console.log('任务状态:', taskStatus)
          console.log('任务消息:', taskMessage)
          console.log('任务摘要:', digest)

          setGenerateEmailProgress({ status: normalizedTaskStatus, message: taskMessage })

          // 检查任务是否完成
          if (normalizedTaskStatus === 'success' && statusResult.data.digest) {
            const digestData = statusResult.data.digest
            const digestId = digestData.id
            setLastBuildMeta({
              llmUsed: Boolean(digestData.llmUsed),
              strategy: digestData.editorialStrategy || 'unknown'
            })

            if (digestData.status === 'sent') {
              setGenerateEmailStatus({
                success: true,
                message: digestData.totalItems > 0
                  ? `这份日报之前已经发送过，请检查邮箱：${formData.email}`
                  : '本次没有可发送的日报内容，系统未新发邮件。'
              })
              setGenerateEmailProgress(null)
              setGenerateEmailTaskId(null)
              
              // 5秒后清除状态
              setTimeout(() => {
                setGenerateEmailStatus(null)
              }, 5000)
              return
            }

            if (!digestData.totalItems || digestData.totalItems <= 0) {
              setGenerateEmailStatus({
                success: false,
                message: '本次构建结果没有可发送内容，因此未执行邮件发送。'
              })
              setGenerateEmailProgress(null)
              setGenerateEmailTaskId(null)
              
              // 5秒后清除状态
              setTimeout(() => {
                setGenerateEmailStatus(null)
              }, 5000)
              return
            }
            
            // 3. 发送邮件
            setGenerateEmailProgress({ status: 'sending', message: '正在发送邮件...' })
            const sendResponse = await apiFetch(`/api/admin/digests/send/${digestId}`, {
              method: 'POST',
              headers: {
                'Content-Type': 'application/json'
              }
            })

            if (!sendResponse.ok) {
              const message = await readErrorMessage(sendResponse, `HTTP error! status: ${sendResponse.status}`)
              throw new Error(message)
            }

            const sendResult = await sendResponse.json()
            console.log('邮件发送结果:', sendResult)
            
            if (sendResult.success) {
              setGenerateEmailStatus({ success: true, message: `邮件已成功生成并发送，请检查邮箱：${formData.email}` })
            } else {
              setGenerateEmailStatus({ success: false, message: `邮件发送失败: ${sendResult.message || '未知错误'}` })
            }
            
            setGenerateEmailProgress(null)
            setGenerateEmailTaskId(null)
            
            // 5秒后清除状态
            setTimeout(() => {
              setGenerateEmailStatus(null)
            }, 5000)
          } else if (normalizedTaskStatus === 'failed') {
            throw new Error(`任务执行失败: ${taskMessage}`)
          } else {
            // 继续轮询
            setTimeout(checkTaskStatus, 2000)
          }
        } catch (error) {
          console.error('检查任务状态失败:', error)
          setGenerateEmailStatus({ success: false, message: `邮件生成失败: ${error instanceof Error ? error.message : '未知错误'}` })
          setGenerateEmailProgress(null)
          setGenerateEmailTaskId(null)
          
          // 5秒后清除状态
          setTimeout(() => {
            setGenerateEmailStatus(null)
          }, 5000)
        }
      }

      // 开始轮询
      setTimeout(checkTaskStatus, 1000)
    } catch (error) {
      console.error('生成邮件失败:', error)
      setGenerateEmailStatus({ success: false, message: `邮件生成失败: ${error instanceof Error ? error.message : '网络错误或后端服务未启动'}` })
      setGenerateEmailProgress(null)
      setGenerateEmailTaskId(null)
      
      // 5秒后清除状态
      setTimeout(() => {
        setGenerateEmailStatus(null)
      }, 5000)
    }
  }

  return (
    <div className="space-y-6">
      {/* 页面标题 */}
      <div className="flex justify-between items-center">
        <h1 className="text-2xl font-bold">系统设置</h1>
        <button
          onClick={handleSave}
          className="flex items-center space-x-2 px-6 py-2 bg-primary-500 hover:bg-primary-600 text-white rounded-lg transition-colors"
        >
          <CheckIcon className="w-4 h-4" />
          <span>保存设置</span>
        </button>
      </div>

      {/* 保存成功提示 */}
      {saved && (
        <div className="bg-green-100 dark:bg-green-900/30 text-green-800 dark:text-green-200 p-4 rounded-lg flex items-center space-x-2">
          <ExclamationCircleIcon className="w-5 h-5" />
          <span>设置已成功保存</span>
        </div>
      )}
      {saveStatus && !saved && (
        <div className={`${saveStatus.success ? 'bg-green-100 dark:bg-green-900/30 text-green-800 dark:text-green-200' : 'bg-red-100 dark:bg-red-900/30 text-red-800 dark:text-red-200'} p-4 rounded-lg flex items-center space-x-2`}>
          <ExclamationCircleIcon className="w-5 h-5" />
          <span>{saveStatus.message}</span>
        </div>
      )}

      {/* 标签页 */}
      <div className="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-200 dark:border-gray-700">
        <div className="border-b border-gray-200 dark:border-gray-700">
          <nav className="flex">
            <TabButton
              active={activeTab === 'general'}
              onClick={() => setActiveTab('general')}
              label="通用设置"
            />
          </nav>
        </div>

        {/* 标签内容 */}
        <div className="p-6">
          {activeTab === 'general' && (
            <div className="space-y-6">
              <SettingSection title="基本信息">
                <FormGroup label="邮箱地址">
                  <input
                    type="email"
                    name="email"
                    value={formData.email}
                    onChange={handleInputChange}
                    className="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent dark:bg-gray-700"
                    placeholder="输入您的邮箱"
                  />
                </FormGroup>
                
                <FormGroup label="发送时间">
                  <input
                    type="time"
                    name="deliveryTime"
                    value={formData.deliveryTime}
                    onChange={handleInputChange}
                    className="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent dark:bg-gray-700"
                  />
                  <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">
                    系统会在每小时的第15分钟检查发送时间<br/>
                    例如：设置16:06，实际发送时间约为16:15-16:18<br/>
                    设置16:20，实际发送时间约为17:15-17:18
                  </p>
                </FormGroup>
                
                <FormGroup label="发送模式">
                  <select
                    name="deliveryMode"
                    value={formData.deliveryMode}
                    onChange={handleInputChange}
                    className="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent dark:bg-gray-700"
                  >
                    <option value="BALANCED">平衡模式</option>
                    <option value="QUIET">安静模式</option>
                    <option value="HUNTER">主动模式</option>
                  </select>
                  <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">
                    平衡模式：平衡内容质量和数量<br/>
                    安静模式：只发送最重要的内容<br/>
                    主动模式：积极寻找和发送更多内容
                  </p>
                </FormGroup>
                
                <FormGroup label="发送频率">
                  <select
                    name="deliveryFrequency"
                    value={formData.deliveryFrequency}
                    onChange={handleInputChange}
                    className="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent dark:bg-gray-700"
                  >
                    <option value="daily">每天</option>
                    <option value="weekly">每周</option>
                    <option value="biweekly">每两周</option>
                    <option value="monthly">每月</option>
                  </select>
                </FormGroup>
              </SettingSection>
              
              <SettingSection title="邮件测试">
                <EmailTestSection
                  email={formData.email}
                  onTestEmail={handleTestEmail}
                  testEmailStatus={testEmailStatus}
                />
              </SettingSection>
              
              <SettingSection title="生成并发送邮件">
                <GenerateEmailSection
                  email={formData.email}
                  onGenerateEmail={() => handleGenerateEmail(false)}
                  onForceGenerateEmail={() => handleGenerateEmail(true)}
                  generateEmailStatus={generateEmailStatus}
                  generateEmailProgress={generateEmailProgress}
                  generateEmailTaskId={generateEmailTaskId}
                  lastBuildMeta={lastBuildMeta}
                />
              </SettingSection>
            </div>
          )}





          {/* 高级选项 */}
          <div className="mt-8">
            <button
              onClick={() => setShowAdvanced(!showAdvanced)}
              className="flex items-center space-x-2 text-primary-600 dark:text-primary-400 font-medium hover:underline"
            >
              {showAdvanced ? (
                <>
                  <ChevronUpIcon className="w-4 h-4" />
                  <span>隐藏高级选项</span>
                </>
              ) : (
                <>
                  <ChevronDownIcon className="w-4 h-4" />
                  <span>显示高级选项</span>
                </>
              )}
            </button>

            {showAdvanced && (
              <div className="mt-4 p-4 bg-gray-50 dark:bg-gray-700 rounded-lg space-y-4">
                <FormGroup label="数据源URL">
                  <input
                    type="text"
                    name="sourceUrl"
                    value={formData.sourceUrl}
                    onChange={handleInputChange}
                    className="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent dark:bg-gray-700"
                    placeholder="输入数据源URL"
                  />
                </FormGroup>
                
                <FormGroup label="缓存时间 (分钟)">
                  <input
                    type="number"
                    name="cacheMinutes"
                    value={formData.cacheMinutes}
                    onChange={handleInputChange}
                    className="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent dark:bg-gray-700"
                    placeholder="缓存时间"
                  />
                </FormGroup>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}

interface TabButtonProps {
  active: boolean
  onClick: () => void
  label: string
}

const TabButton: React.FC<TabButtonProps> = ({ active, onClick, label }) => {
  return (
    <button
      onClick={onClick}
      className={`px-6 py-4 text-sm font-medium transition-colors ${active
        ? 'border-b-2 border-primary-500 text-primary-600 dark:text-primary-400'
        : 'text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300'
        }`}
    >
      {label}
    </button>
  )
}

interface SettingSectionProps {
  title: string
  children: React.ReactNode
}

const SettingSection: React.FC<SettingSectionProps> = ({ title, children }) => {
  return (
    <div className="space-y-4">
      <h3 className="text-lg font-semibold text-gray-700 dark:text-gray-300">{title}</h3>
      <div className="space-y-4">{children}</div>
    </div>
  )
}

interface FormGroupProps {
  label: string
  children: React.ReactNode
}

const FormGroup: React.FC<FormGroupProps> = ({ label, children }) => {
  return (
    <div>
      <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">{label}</label>
      {children}
    </div>
  )
}

export default Settings
