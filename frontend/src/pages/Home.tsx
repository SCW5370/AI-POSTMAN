import React from 'react'
import { ArrowRightIcon, CheckCircleIcon, ClockIcon, EnvelopeIcon } from '@heroicons/react/24/outline'

const Home: React.FC = () => {
  return (
    <div className="space-y-8">
      {/* 欢迎信息 */}
      <div className="bg-gradient-to-r from-primary-500 to-primary-600 text-white rounded-2xl p-8 shadow-lg">
        <h1 className="text-3xl font-bold mb-4">欢迎使用 AI 送报员</h1>
        <p className="text-lg opacity-90 mb-6">智能信息管理系统，为您提供个性化的信息推送服务</p>
        <div className="flex space-x-4">
          <a 
            href="/profile" 
            className="flex items-center space-x-2 px-6 py-3 bg-white text-primary-600 rounded-lg font-medium hover:bg-gray-100 transition-colors"
          >
            <span>完善用户画像</span>
            <ArrowRightIcon className="w-4 h-4" />
          </a>
          <a 
            href="/settings" 
            className="flex items-center space-x-2 px-6 py-3 bg-white/20 text-white rounded-lg font-medium hover:bg-white/30 transition-colors"
          >
            <span>系统设置</span>
            <ArrowRightIcon className="w-4 h-4" />
          </a>
        </div>
      </div>

      {/* 功能卡片 */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <FeatureCard
          title="用户画像"
          description="完善您的兴趣爱好和偏好，获得个性化的信息推送"
          icon={<CheckCircleIcon className="w-6 h-6 text-green-500" />}
          link="/profile"
          linkText="开始设置"
        />
        <FeatureCard
          title="邮件设置"
          description="配置邮件发送频率和时间，确保及时收到重要信息"
          icon={<EnvelopeIcon className="w-6 h-6 text-blue-500" />}
          link="/settings"
          linkText="配置邮件"
        />
        <FeatureCard
          title="系统配置"
          description="管理API密钥、数据源等系统配置，优化系统性能"
          icon={<ClockIcon className="w-6 h-6 text-purple-500" />}
          link="/settings"
          linkText="系统设置"
        />
      </div>

      {/* 系统状态 */}
      <div className="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-200 dark:border-gray-700 p-6">
        <h2 className="text-lg font-semibold mb-4">系统状态</h2>
        <div className="space-y-3">
          <StatusItem label="服务状态" status="正常" isSuccess={true} />
          <StatusItem label="邮件服务" status="已连接" isSuccess={true} />
          <StatusItem label="数据同步" status="最新" isSuccess={true} />
        </div>
      </div>
    </div>
  )
}

interface FeatureCardProps {
  title: string
  description: string
  icon: React.ReactNode
  link: string
  linkText: string
}

const FeatureCard: React.FC<FeatureCardProps> = ({ title, description, icon, link, linkText }) => {
  return (
    <div className="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-200 dark:border-gray-700 p-6 hover:shadow-md transition-shadow">
      <div className="mb-4">{icon}</div>
      <h3 className="text-xl font-semibold mb-2">{title}</h3>
      <p className="text-gray-600 dark:text-gray-400 mb-4">{description}</p>
      <a 
        href={link} 
        className="inline-flex items-center space-x-2 text-primary-600 dark:text-primary-400 font-medium hover:underline"
      >
        <span>{linkText}</span>
        <ArrowRightIcon className="w-4 h-4" />
      </a>
    </div>
  )
}

interface StatusItemProps {
  label: string
  status: string
  isSuccess: boolean
}

const StatusItem: React.FC<StatusItemProps> = ({ label, status, isSuccess }) => {
  return (
    <div className="flex items-center justify-between">
      <span className="text-gray-700 dark:text-gray-300">{label}</span>
      <span className={`flex items-center space-x-2 ${isSuccess ? 'text-green-600 dark:text-green-400' : 'text-red-600 dark:text-red-400'}`}>
        {isSuccess ? (
          <CheckCircleIcon className="w-4 h-4" />
        ) : (
          <ClockIcon className="w-4 h-4" />
        )}
        <span>{status}</span>
      </span>
    </div>
  )
}

export default Home