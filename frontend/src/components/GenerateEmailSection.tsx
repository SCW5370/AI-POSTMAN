import React from 'react'
import { EnvelopeIcon, CheckIcon, ExclamationCircleIcon } from '@heroicons/react/24/outline'

interface GenerateEmailSectionProps {
  email: string
  onGenerateEmail: () => void
  onForceGenerateEmail: () => void
  generateEmailStatus: { success: boolean; message: string } | null
  generateEmailProgress: { status: string; message: string } | null
  generateEmailTaskId: string | null
  lastBuildMeta: { llmUsed: boolean; strategy: string } | null
}

const GenerateEmailSection: React.FC<GenerateEmailSectionProps> = ({
  onGenerateEmail,
  onForceGenerateEmail,
  generateEmailStatus,
  generateEmailProgress,
  generateEmailTaskId,
  lastBuildMeta
}) => {
  return (
    <div className="space-y-4">
      <div className="space-y-4">
        <button
          onClick={onGenerateEmail}
          className="flex items-center space-x-2 px-6 py-2 bg-green-500 hover:bg-green-600 text-white rounded-lg transition-colors"
        >
          <EnvelopeIcon className="w-4 h-4" />
          <span>生成并发送邮件</span>
        </button>
        <button
          onClick={onForceGenerateEmail}
          className="flex items-center space-x-2 px-6 py-2 bg-amber-500 hover:bg-amber-600 text-white rounded-lg transition-colors"
        >
          <EnvelopeIcon className="w-4 h-4" />
          <span>强制重建并发送（跳过复用）</span>
        </button>
      </div>
      {generateEmailProgress && (
        <div className="mt-4 p-4 rounded-lg bg-blue-100 dark:bg-blue-900/30 text-blue-800 dark:text-blue-200">
          <div className="flex items-center space-x-2">
            <div className="animate-spin rounded-full h-5 w-5 border-t-2 border-b-2 border-blue-500"></div>
            <div>
              <div className="font-medium">处理中...</div>
              <div className="text-sm">{generateEmailProgress.message}</div>
              {generateEmailTaskId && (
                <div className="text-xs text-blue-600 dark:text-blue-300 mt-1">任务ID: {generateEmailTaskId}</div>
              )}
            </div>
          </div>
        </div>
      )}
      {generateEmailStatus && (
        <div className={`mt-4 p-4 rounded-lg ${generateEmailStatus.success ? 'bg-green-100 dark:bg-green-900/30 text-green-800 dark:text-green-200' : 'bg-red-100 dark:bg-red-900/30 text-red-800 dark:text-red-200'}`}>
          <div className="flex items-center space-x-2">
            {generateEmailStatus.success ? (
              <CheckIcon className="w-5 h-5" />
            ) : (
              <ExclamationCircleIcon className="w-5 h-5" />
            )}
            <span>{generateEmailStatus.message}</span>
          </div>
          {lastBuildMeta && (
            <div className="mt-2 text-sm">
              <span className="mr-3">LLM参与: {lastBuildMeta.llmUsed ? '是' : '否(降级)'}</span>
              <span>策略: {lastBuildMeta.strategy}</span>
            </div>
          )}
        </div>
      )}
    </div>
  )
}

export default GenerateEmailSection
