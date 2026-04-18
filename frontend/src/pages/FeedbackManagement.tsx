import React, { useState } from 'react'
import { MagnifyingGlassIcon, FunnelIcon, CheckCircleIcon, XCircleIcon, ExclamationCircleIcon } from '@heroicons/react/24/outline'

const FeedbackManagement: React.FC = () => {
  const [feedbacks] = useState([
    { id: 1, userId: 1, digestId: 123, digestItemId: 456, feedbackType: 'USEFUL', content: '内容非常有价值，学到了很多', createdAt: '2026-04-14 10:30' },
    { id: 2, userId: 1, digestId: 123, digestItemId: 457, feedbackType: 'NORMAL', content: '内容一般，希望能有更多深度分析', createdAt: '2026-04-14 09:15' },
    { id: 3, userId: 2, digestId: 124, digestItemId: 458, feedbackType: 'FOLLOW', content: '对这个话题很感兴趣，希望能看到更多相关内容', createdAt: '2026-04-13 16:45' },
    { id: 4, userId: 3, digestId: 125, digestItemId: 459, feedbackType: 'USEFUL', content: '推荐的工具非常实用，已经开始使用了', createdAt: '2026-04-13 14:20' },
  ])
  const [searchTerm, setSearchTerm] = useState('')
  const [selectedUserId, setSelectedUserId] = useState('')

  // 模拟反馈数据
  const filteredFeedbacks = feedbacks.filter(feedback => 
    (selectedUserId ? feedback.userId.toString() === selectedUserId : true) &&
    (feedback.content.toLowerCase().includes(searchTerm.toLowerCase()) ||
     feedback.feedbackType.toLowerCase().includes(searchTerm.toLowerCase()))
  )

  const getFeedbackTypeColor = (type: string) => {
    switch (type) {
      case 'USEFUL':
        return 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200'
      case 'NORMAL':
        return 'bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-200'
      case 'FOLLOW':
        return 'bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-200'
      default:
        return 'bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-200'
    }
  }

  const getFeedbackTypeIcon = (type: string) => {
    switch (type) {
      case 'USEFUL':
        return <CheckCircleIcon className="w-4 h-4" />
      case 'NORMAL':
        return <ExclamationCircleIcon className="w-4 h-4" />
      case 'FOLLOW':
        return <XCircleIcon className="w-4 h-4" />
      default:
        return null
    }
  }

  return (
    <div className="space-y-6">
      {/* 页面标题 */}
      <div className="flex justify-between items-center">
        <h1 className="text-2xl font-bold">反馈管理</h1>
      </div>

      {/* 搜索和筛选 */}
      <div className="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-200 dark:border-gray-700 p-4">
        <div className="flex flex-col sm:flex-row gap-4">
          <div className="flex-1 relative">
            <MagnifyingGlassIcon className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-gray-400" />
            <input
              type="text"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-10 pr-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent dark:bg-gray-700"
              placeholder="搜索反馈..."
            />
          </div>
          <div className="flex items-center space-x-2">
            <input
              type="text"
              value={selectedUserId}
              onChange={(e) => setSelectedUserId(e.target.value)}
              className="px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent dark:bg-gray-700"
              placeholder="用户ID"
            />
            <button className="flex items-center space-x-2 px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors">
              <FunnelIcon className="w-4 h-4" />
            <span>筛选</span>
            </button>
          </div>
        </div>
      </div>

      {/* 反馈列表 */}
      <div className="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-200 dark:border-gray-700 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-gray-50 dark:bg-gray-700">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">ID</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">用户ID</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">日报ID</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">反馈类型</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">内容</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">创建时间</th>
              </tr>
            </thead>
            <tbody className="bg-white dark:bg-gray-800 divide-y divide-gray-200 dark:divide-gray-700">
              {filteredFeedbacks.map((feedback) => (
                <tr key={feedback.id} className="hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors">
                  <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900 dark:text-gray-100">{feedback.id}</td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500 dark:text-gray-400">{feedback.userId}</td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500 dark:text-gray-400">{feedback.digestId}</td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <span className={`px-2 inline-flex items-center space-x-1 text-xs leading-5 font-semibold rounded-full ${getFeedbackTypeColor(feedback.feedbackType)}`}>
                      {getFeedbackTypeIcon(feedback.feedbackType)}
                      <span>{feedback.feedbackType}</span>
                    </span>
                  </td>
                  <td className="px-6 py-4 text-sm text-gray-500 dark:text-gray-400">{feedback.content}</td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500 dark:text-gray-400">{feedback.createdAt}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* 反馈统计 */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-200 dark:border-gray-700 p-6">
          <h3 className="text-sm font-medium text-gray-600 dark:text-gray-400 mb-2">总反馈数</h3>
          <p className="text-2xl font-bold">{feedbacks.length}</p>
        </div>
        <div className="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-200 dark:border-gray-700 p-6">
          <h3 className="text-sm font-medium text-gray-600 dark:text-gray-400 mb-2">有用反馈</h3>
          <p className="text-2xl font-bold text-green-600 dark:text-green-400">{feedbacks.filter(f => f.feedbackType === 'USEFUL').length}</p>
        </div>
        <div className="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-200 dark:border-gray-700 p-6">
          <h3 className="text-sm font-medium text-gray-600 dark:text-gray-400 mb-2">关注反馈</h3>
          <p className="text-2xl font-bold text-blue-600 dark:text-blue-400">{feedbacks.filter(f => f.feedbackType === 'FOLLOW').length}</p>
        </div>
      </div>
    </div>
  )
}

export default FeedbackManagement
