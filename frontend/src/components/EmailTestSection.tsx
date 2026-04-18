import React from 'react'
import { EnvelopeIcon, CheckIcon, ExclamationCircleIcon } from '@heroicons/react/24/outline'

interface EmailTestSectionProps {
  email: string
  onTestEmail: () => void
  testEmailStatus: { success: boolean; message: string } | null
}

const EmailTestSection: React.FC<EmailTestSectionProps> = ({ onTestEmail, testEmailStatus }) => {
  return (
    <div className="space-y-4">
      <p className="text-sm text-gray-500 dark:text-gray-400 mb-4">
        测试邮件发送功能，确保您的邮箱配置正确
      </p>
      <div className="space-y-4">
        <button
          onClick={onTestEmail}
          className="flex items-center space-x-2 px-6 py-2 bg-blue-500 hover:bg-blue-600 text-white rounded-lg transition-colors"
        >
          <EnvelopeIcon className="w-4 h-4" />
          <span>发送测试邮件</span>
        </button>
      </div>
      {testEmailStatus && (
        <div className={`mt-4 p-4 rounded-lg ${testEmailStatus.success ? 'bg-green-100 dark:bg-green-900/30 text-green-800 dark:text-green-200' : 'bg-red-100 dark:bg-red-900/30 text-red-800 dark:text-red-200'}`}>
          <div className="flex items-center space-x-2">
            {testEmailStatus.success ? (
              <CheckIcon className="w-5 h-5" />
            ) : (
              <ExclamationCircleIcon className="w-5 h-5" />
            )}
            <span>{testEmailStatus.message}</span>
          </div>
        </div>
      )}
    </div>
  )
}

export default EmailTestSection