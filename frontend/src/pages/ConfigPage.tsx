import { useState, useEffect } from 'react'
import { apiFetch } from '../utils/api'

interface ConfigForm {
  // 大模型配置
  llmApiKey: string
  llmApiBase: string
  llmModel: string
  
  // SMTP配置
  smtpUsername: string
  smtpPassword: string
}

interface ValidationErrors {
  [key: string]: string
}

const ConfigPage = () => {
  const [formData, setFormData] = useState<ConfigForm>({
    llmApiKey: '',
    llmApiBase: '',
    llmModel: '',
    smtpUsername: '',
    smtpPassword: ''
  })
  
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [validationErrors, setValidationErrors] = useState<ValidationErrors>({})

  // 加载当前配置
  useEffect(() => {
    const loadConfig = async () => {
      try {
        setLoading(true)
        const response = await apiFetch('/api/config')
        const data = await response.json()
        if (data.success) {
          setFormData({
            llmApiKey: data.data.llmApiKey || '',
            llmApiBase: data.data.llmApiBase || '',
            llmModel: data.data.llmModel || '',
            smtpUsername: data.data.smtpUsername || '',
            smtpPassword: data.data.smtpPassword || ''
          })
        }
      } catch (err) {
        setError('加载配置失败，请刷新页面重试')
        console.error('加载配置失败:', err)
      } finally {
        setLoading(false)
      }
    }

    loadConfig()
  }, [])

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target
    setFormData(prev => ({
      ...prev,
      [name]: value
    }))
    
    // 清除该字段的验证错误
    if (validationErrors[name]) {
      setValidationErrors(prev => {
        const newErrors = { ...prev }
        delete newErrors[name]
        return newErrors
      })
    }
  }

  const validateForm = (): boolean => {
    const errors: ValidationErrors = {}
    
    // 验证大模型配置
    if (!formData.llmApiKey) {
      errors.llmApiKey = 'API Key不能为空'
    }
    if (!formData.llmApiBase) {
      errors.llmApiBase = 'API Base URL不能为空'
    } else if (!formData.llmApiBase.startsWith('http://') && !formData.llmApiBase.startsWith('https://')) {
      errors.llmApiBase = 'API Base URL必须以http://或https://开头'
    }
    if (!formData.llmModel) {
      errors.llmModel = '模型ID不能为空'
    }
    
    // 验证SMTP配置
    if (!formData.smtpUsername) {
      errors.smtpUsername = '发信邮箱不能为空'
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.smtpUsername)) {
      errors.smtpUsername = '请输入有效的邮箱地址'
    }
    if (!formData.smtpPassword) {
      errors.smtpPassword = '授权码不能为空'
    }
    
    setValidationErrors(errors)
    return Object.keys(errors).length === 0
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    
    // 验证表单
    if (!validateForm()) {
      setError('请检查并修正表单中的错误')
      return
    }
    
    try {
      setLoading(true)
      setMessage(null)
      setError(null)
      
      const response = await apiFetch('/api/config', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(formData)
      })
      
      const data = await response.json()
      if (data.success) {
        setMessage('配置保存成功！请注意：部分配置可能需要重启服务才能生效')
        setTimeout(() => setMessage(null), 5000)
      } else {
        setError(data.message || '保存失败，请重试')
      }
    } catch (err) {
      setError('保存配置失败，请检查网络连接或后端服务状态')
      console.error('保存配置失败:', err)
    } finally {
      setLoading(false)
    }
  }

  const getInputClass = (field: string) => {
    if (validationErrors[field]) {
      return 'w-full px-3 py-2 border border-red-500 rounded-md focus:outline-none focus:ring-2 focus:ring-red-500'
    }
    return 'w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500'
  }

  return (
    <div className="container mx-auto px-4 py-8">
      <h1 className="text-2xl font-bold mb-6">系统配置</h1>
      
      {message && (
        <div className="bg-green-100 border border-green-400 text-green-700 px-4 py-3 rounded mb-6">
          <div className="flex items-start">
            <svg className="flex-shrink-0 h-5 w-5 text-green-500 mt-0.5" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor">
              <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
            </svg>
            <div className="ml-3">
              <p className="text-sm font-medium">{message}</p>
            </div>
          </div>
        </div>
      )}
      
      {error && (
        <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-6">
          <div className="flex items-start">
            <svg className="flex-shrink-0 h-5 w-5 text-red-500 mt-0.5" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor">
              <path fillRule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
            </svg>
            <div className="ml-3">
              <p className="text-sm font-medium">{error}</p>
            </div>
          </div>
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-6">
        {/* 大模型配置 */}
        <div className="bg-white p-6 rounded-lg shadow">
          <h2 className="text-lg font-semibold mb-4">大模型配置</h2>
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">API Key</label>
              <input
                type="password"
                name="llmApiKey"
                value={formData.llmApiKey}
                onChange={handleChange}
                className={getInputClass('llmApiKey')}
                placeholder="输入大模型API Key"
              />
              {validationErrors.llmApiKey && (
                <p className="mt-1 text-sm text-red-600">{validationErrors.llmApiKey}</p>
              )}
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">API Base URL</label>
              <input
                type="text"
                name="llmApiBase"
                value={formData.llmApiBase}
                onChange={handleChange}
                className={getInputClass('llmApiBase')}
                placeholder="输入API Base URL"
              />
              {validationErrors.llmApiBase && (
                <p className="mt-1 text-sm text-red-600">{validationErrors.llmApiBase}</p>
              )}
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">模型ID</label>
              <input
                type="text"
                name="llmModel"
                value={formData.llmModel}
                onChange={handleChange}
                className={getInputClass('llmModel')}
                placeholder="输入模型ID"
              />
              {validationErrors.llmModel && (
                <p className="mt-1 text-sm text-red-600">{validationErrors.llmModel}</p>
              )}
            </div>
          </div>
        </div>

        {/* SMTP配置 */}
        <div className="bg-white p-6 rounded-lg shadow">
          <h2 className="text-lg font-semibold mb-4">SMTP邮件服务配置</h2>
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">发信邮箱</label>
              <input
                type="email"
                name="smtpUsername"
                value={formData.smtpUsername}
                onChange={handleChange}
                className={getInputClass('smtpUsername')}
                placeholder="输入发信邮箱"
              />
              {validationErrors.smtpUsername && (
                <p className="mt-1 text-sm text-red-600">{validationErrors.smtpUsername}</p>
              )}
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">授权码</label>
              <input
                type="password"
                name="smtpPassword"
                value={formData.smtpPassword}
                onChange={handleChange}
                className={getInputClass('smtpPassword')}
                placeholder="输入SMTP授权码"
              />
              {validationErrors.smtpPassword && (
                <p className="mt-1 text-sm text-red-600">{validationErrors.smtpPassword}</p>
              )}
            </div>
            <div className="bg-blue-50 p-4 rounded-md">
              <p className="text-sm text-blue-700">
                <strong>提示：</strong>SMTP服务器默认使用QQ邮箱的smtp.qq.com，端口默认587，无需手动配置。
              </p>
            </div>
          </div>
        </div>

        <div className="flex justify-end">
          <button
            type="submit"
            disabled={loading}
            className="bg-blue-600 text-white px-6 py-3 rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 disabled:bg-gray-400 disabled:cursor-not-allowed transition-all duration-200"
          >
            {loading ? (
              <div className="flex items-center">
                <svg className="animate-spin -ml-1 mr-2 h-4 w-4 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
                保存中...
              </div>
            ) : '保存配置'}
          </button>
        </div>
      </form>
    </div>
  )
}

export default ConfigPage
