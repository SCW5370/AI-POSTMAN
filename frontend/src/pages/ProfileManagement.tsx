import React, { useState } from 'react'
import { PencilIcon, CheckIcon, TrashIcon, MagnifyingGlassIcon, ChevronDownIcon, ChevronUpIcon } from '@heroicons/react/24/outline'
import { getCurrentUserId, setCurrentUserId } from '../utils/currentUser'
import { apiFetch } from '../utils/api'

const toList = (value: Record<string, unknown> | null | undefined): string[] => {
  if (!value) {
    return []
  }
  return Object.keys(value)
}

const toMap = (values: string[]): Record<string, string> => {
  return values.reduce<Record<string, string>>((acc, item) => {
    const key = item.trim()
    if (key) {
      acc[key] = key
    }
    return acc
  }, {})
}

const ProfileManagement: React.FC = () => {
  const [userId, setUserId] = useState(String(getCurrentUserId()))
  const [profile, setProfile] = useState({
    userId: getCurrentUserId(),
    occupation: '软件工程师',
    interests: ['人工智能', '后端开发', '开源项目'],
    preferredTopics: ['AI Agent', 'Java', 'Spring Boot', 'GitHub'],
    recentActivities: ['学习AI Agent', '参加技术会议', '贡献开源项目'],
    readingHabits: ['技术博客', '学术论文', '开源文档'],
    contentPreferences: ['深度分析', '实用教程', '行业动态'],
    technicalSkills: ['Java', 'Python', 'JavaScript', 'Spring Boot', 'React', 'Docker', 'Kubernetes', 'AWS'],
    experienceLevel: '中级',
    learningGoals: ['深入学习AI Agent', '掌握微服务架构', '提升DevOps技能'],
    industryFocus: ['互联网', '金融科技', '人工智能'],
    codingHabits: ['TDD', '代码审查', '持续集成'],
    toolPreferences: ['IntelliJ IDEA', 'VS Code', 'Git', 'Postman'],
    contentFormats: ['文章', '视频', '代码示例', '在线课程'],
    timeAvailability: ['工作日晚上', '周末', '随时']
  })
  const [isEditing, setIsEditing] = useState(false)
  const [newInterest, setNewInterest] = useState('')
  const [newTopic, setNewTopic] = useState('')
  const [newActivity, setNewActivity] = useState('')
  const [expandedSections, setExpandedSections] = useState({
    basicInfo: true,
    technicalSkills: true,
    interests: true,
    preferredTopics: true,
    recentActivities: true,
    readingHabits: true,
    contentPreferences: true,
    learningGoals: true,
    industryFocus: true,
    codingHabits: true,
    toolPreferences: true,
    contentFormats: true,
    timeAvailability: true
  })

  const toggleSection = (section: ProfileSection) => {
    setExpandedSections(prev => ({
      ...prev,
      [section]: !prev[section]
    }))
  }

  const technicalSkillOptions = [
    'Java', 'Python', 'JavaScript', 'TypeScript', 'C++', 'C#', 'Go', 'Rust',
    'Spring Boot', 'React', 'Angular', 'Vue', 'Node.js', 'Express',
    'Docker', 'Kubernetes', 'AWS', 'Azure', 'GCP',
    'PostgreSQL', 'MySQL', 'MongoDB', 'Redis',
    'Git', 'Jenkins', 'CI/CD', 'DevOps',
    'AI/ML', 'Data Science', 'Big Data',
    'Frontend', 'Backend', 'Full Stack', 'Mobile', 'Web', 'Embedded'
  ]

  const experienceLevelOptions = ['初级', '中级', '高级', '专家']

  const industryOptions = [
    '互联网', '金融科技', '人工智能', '医疗健康', '教育', '电子商务',
    '制造业', '能源', '交通', '零售', '媒体', '游戏', '政府'
  ]

  const contentFormatOptions = ['文章', '视频', '代码示例', '在线课程', '播客', '研讨会', '文档']

  const timeAvailabilityOptions = ['工作日晚上', '周末', '随时', '早上', '下午']

  const readingHabitOptions = ['技术博客', '学术论文', '开源文档', '行业报告', '技术书籍', '在线教程', '会议记录', '技术周刊']

  const contentPreferenceOptions = ['深度分析', '实用教程', '行业动态', '技术前沿', '案例研究', '最佳实践', '趋势预测', '工具评测']

  const handleAddTechnicalSkill = (skill: string) => {
    if (!profile.technicalSkills.includes(skill)) {
      setProfile(prev => ({
        ...prev,
        technicalSkills: [...prev.technicalSkills, skill]
      }))
    }
  }

  const handleRemoveTechnicalSkill = (skill: string) => {
    setProfile(prev => ({
      ...prev,
      technicalSkills: prev.technicalSkills.filter(s => s !== skill)
    }))
  }

  const handleAddLearningGoal = (goal: string) => {
    setProfile(prev => ({
      ...prev,
      learningGoals: [...prev.learningGoals, goal]
    }))
  }

  const handleRemoveLearningGoal = (goal: string) => {
    setProfile(prev => ({
      ...prev,
      learningGoals: prev.learningGoals.filter(g => g !== goal)
    }))
  }

  const handleAddIndustry = (industry: string) => {
    if (!profile.industryFocus.includes(industry)) {
      setProfile(prev => ({
        ...prev,
        industryFocus: [...prev.industryFocus, industry]
      }))
    }
  }

  const handleRemoveIndustry = (industry: string) => {
    setProfile(prev => ({
      ...prev,
      industryFocus: prev.industryFocus.filter(i => i !== industry)
    }))
  }

  const handleAddCodingHabit = (habit: string) => {
    if (!profile.codingHabits.includes(habit)) {
      setProfile(prev => ({
        ...prev,
        codingHabits: [...prev.codingHabits, habit]
      }))
    }
  }

  const handleAddTool = (tool: string) => {
    if (!profile.toolPreferences.includes(tool)) {
      setProfile(prev => ({
        ...prev,
        toolPreferences: [...prev.toolPreferences, tool]
      }))
    }
  }

  const handleRemoveTool = (tool: string) => {
    setProfile(prev => ({
      ...prev,
      toolPreferences: prev.toolPreferences.filter(t => t !== tool)
    }))
  }

  const handleAddContentFormat = (format: string) => {
    if (!profile.contentFormats.includes(format)) {
      setProfile(prev => ({
        ...prev,
        contentFormats: [...prev.contentFormats, format]
      }))
    }
  }

  const handleRemoveContentFormat = (format: string) => {
    setProfile(prev => ({
      ...prev,
      contentFormats: prev.contentFormats.filter(f => f !== format)
    }))
  }

  const handleAddTimeAvailability = (time: string) => {
    if (!profile.timeAvailability.includes(time)) {
      setProfile(prev => ({
        ...prev,
        timeAvailability: [...prev.timeAvailability, time]
      }))
    }
  }

  const handleRemoveTimeAvailability = (time: string) => {
    setProfile(prev => ({
      ...prev,
      timeAvailability: prev.timeAvailability.filter(t => t !== time)
    }))
  }

  const handleAddActivity = () => {
    if (newActivity.trim()) {
      setProfile(prev => ({
        ...prev,
        recentActivities: [...prev.recentActivities, newActivity.trim()]
      }))
      setNewActivity('')
    }
  }

  const handleRemoveActivity = (activity: string) => {
    setProfile(prev => ({
      ...prev,
      recentActivities: prev.recentActivities.filter(a => a !== activity)
    }))
  }

  const handleAddReadingHabit = (habit: string) => {
    if (!profile.readingHabits.includes(habit)) {
      setProfile(prev => ({
        ...prev,
        readingHabits: [...prev.readingHabits, habit]
      }))
    }
  }

  const handleRemoveReadingHabit = (habit: string) => {
    setProfile(prev => ({
      ...prev,
      readingHabits: prev.readingHabits.filter(h => h !== habit)
    }))
  }

  const handleAddContentPreference = (preference: string) => {
    if (!profile.contentPreferences.includes(preference)) {
      setProfile(prev => ({
        ...prev,
        contentPreferences: [...prev.contentPreferences, preference]
      }))
    }
  }

  const handleRemoveContentPreference = (preference: string) => {
    setProfile(prev => ({
      ...prev,
      contentPreferences: prev.contentPreferences.filter(p => p !== preference)
    }))
  }

  const handleLoadProfile = async () => {
    console.log('加载用户画像:', userId)
    try {
      const resolvedUserId = Number(userId)
      if (!Number.isInteger(resolvedUserId) || resolvedUserId <= 0) {
        throw new Error('用户ID无效')
      }
      const response = await apiFetch(`/api/profile/me?userId=${resolvedUserId}`)
      
      if (!response.ok) {
        throw new Error('Failed to load profile')
      }
      
      const result = await response.json()
      const data = result.data || {}
      console.log('用户画像加载成功:', data)
      setCurrentUserId(resolvedUserId)
      
      setProfile(prev => ({
        ...prev,
        userId: resolvedUserId,
        occupation: data.occupation || prev.occupation,
        interests: toList(data.interests) || prev.interests,
        preferredTopics: toList(data.preferredTopics) || prev.preferredTopics,
        recentActivities: toList(data.recentActivities) || prev.recentActivities,
        readingHabits: toList(data.readingHabits).length > 0 ? toList(data.readingHabits) : prev.readingHabits,
        contentPreferences: toList(data.contentPreferences).length > 0 ? toList(data.contentPreferences) : prev.contentPreferences,
        technicalSkills: toList(data.technicalSkills).length > 0 ? toList(data.technicalSkills) : prev.technicalSkills,
        experienceLevel: data.experienceLevel || prev.experienceLevel,
        learningGoals: toList(data.learningGoals).length > 0 ? toList(data.learningGoals) : prev.learningGoals,
        industryFocus: toList(data.industryFocus).length > 0 ? toList(data.industryFocus) : prev.industryFocus,
        codingHabits: toList(data.codingHabits).length > 0 ? toList(data.codingHabits) : prev.codingHabits,
        toolPreferences: toList(data.toolPreferences).length > 0 ? toList(data.toolPreferences) : prev.toolPreferences,
        contentFormats: toList(data.contentFormats).length > 0 ? toList(data.contentFormats) : prev.contentFormats,
        timeAvailability: toList(data.timeAvailability).length > 0 ? toList(data.timeAvailability) : prev.timeAvailability
      }))
    } catch (error) {
      console.error('加载用户画像失败:', error)
      alert('加载用户画像失败，请稍后重试')
    }
  }

  const handleSaveProfile = async () => {
    try {
      const resolvedUserId = Number(userId)
      if (!Number.isInteger(resolvedUserId) || resolvedUserId <= 0) {
        throw new Error('用户ID无效')
      }
      
      const requestData = {
        occupation: profile.occupation,
        interests: toMap(profile.interests),
        preferredTopics: toMap(profile.preferredTopics),
        recentActivities: toMap(profile.recentActivities),
        readingHabits: toMap(profile.readingHabits),
        contentPreferences: toMap(profile.contentPreferences),
        technicalSkills: toMap(profile.technicalSkills),
        experienceLevel: profile.experienceLevel,
        learningGoals: toMap(profile.learningGoals),
        industryFocus: toMap(profile.industryFocus),
        codingHabits: toMap(profile.codingHabits),
        toolPreferences: toMap(profile.toolPreferences),
        contentFormats: toMap(profile.contentFormats),
        timeAvailability: toMap(profile.timeAvailability)
      }
      
      console.log('保存用户画像请求数据:', requestData)
      
      const response = await apiFetch(`/api/profile/update/survey?userId=${resolvedUserId}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(requestData)
      })
      
      console.log('保存用户画像响应状态:', response.status)
      
      if (!response.ok) {
        const errorText = await response.text()
        console.error('保存用户画像失败响应:', errorText)
        throw new Error(`保存用户画像失败: ${errorText}`)
      }
      
      const responseData = await response.json()
      console.log('保存用户画像成功响应:', responseData)
      
      setCurrentUserId(resolvedUserId)
      setIsEditing(false)
      alert('用户画像已保存')
    } catch (error) {
      console.error('保存用户画像失败:', error)
      const errorMessage = error instanceof Error ? error.message : '未知错误'
      alert(`保存用户画像失败: ${errorMessage}`)
    }
  }

  const handleAddInterest = () => {
    if (newInterest.trim()) {
      setProfile(prev => ({
        ...prev,
        interests: [...prev.interests, newInterest.trim()]
      }))
      setNewInterest('')
    }
  }

  const handleAddTopic = () => {
    if (newTopic.trim()) {
      setProfile(prev => ({
        ...prev,
        preferredTopics: [...prev.preferredTopics, newTopic.trim()]
      }))
      setNewTopic('')
    }
  }

  const handleRemoveInterest = (interest: string) => {
    setProfile(prev => ({
      ...prev,
      interests: prev.interests.filter(i => i !== interest)
    }))
  }

  const handleRemoveTopic = (topic: string) => {
    setProfile(prev => ({
      ...prev,
      preferredTopics: prev.preferredTopics.filter(t => t !== topic)
    }))
  }

  return (
    <div className="space-y-6">
      {/* 页面标题 */}
      <div className="flex justify-between items-center">
        <h1 className="text-2xl font-bold">用户画像管理</h1>
      </div>

      {/* 用户选择 */}
      <div className="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-200 dark:border-gray-700 p-4">
        <div className="flex flex-col sm:flex-row gap-4 items-center">
          <div className="flex-1 relative">
            <MagnifyingGlassIcon className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-gray-400" />
            <input
              type="text"
              value={userId}
              onChange={(e) => setUserId(e.target.value)}
              className="w-full pl-10 pr-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent dark:bg-gray-700"
              placeholder="输入用户ID"
            />
          </div>
          <button
            onClick={handleLoadProfile}
            className="px-4 py-2 bg-primary-500 hover:bg-primary-600 text-white rounded-lg transition-colors"
          >
            加载画像
          </button>
        </div>
      </div>

      {/* 用户画像详情 */}
      <div className="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-200 dark:border-gray-700 p-6">
        <div className="flex justify-between items-center mb-6">
          <h2 className="text-lg font-semibold">用户画像详情</h2>
          {!isEditing ? (
            <button
              onClick={() => setIsEditing(true)}
              className="flex items-center space-x-2 px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
            >
              <PencilIcon className="w-4 h-4" />
              <span>编辑</span>
            </button>
          ) : (
            <button
              onClick={handleSaveProfile}
              className="flex items-center space-x-2 px-4 py-2 bg-primary-500 hover:bg-primary-600 text-white rounded-lg transition-colors"
            >
              <CheckIcon className="w-4 h-4" />
              <span>保存</span>
            </button>
          )}
        </div>

        <div className="space-y-6">
          {/* 基本信息 */}
          <div>
            <div 
              className="flex justify-between items-center cursor-pointer" 
              onClick={() => toggleSection('basicInfo')}
            >
              <h3 className="text-md font-semibold text-gray-700 dark:text-gray-300">基本信息</h3>
              {expandedSections.basicInfo ? (
                <ChevronUpIcon className="w-5 h-5 text-gray-500" />
              ) : (
                <ChevronDownIcon className="w-5 h-5 text-gray-500" />
              )}
            </div>
            {expandedSections.basicInfo && (
              <div className="mt-4 space-y-4">
                {/* 职业信息 */}
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">职业</label>
                  <input
                    type="text"
                    value={profile.occupation}
                    onChange={(e) => setProfile(prev => ({ ...prev, occupation: e.target.value }))}
                    disabled={!isEditing}
                    className={`w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent dark:bg-gray-700 ${!isEditing ? 'bg-gray-50 dark:bg-gray-600' : ''}`}
                  />
                  <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">
                    职业信息会影响AI推荐的内容领域和专业深度
                  </p>
                </div>

                {/* 经验水平 */}
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">经验水平</label>
                  <select
                    value={profile.experienceLevel}
                    onChange={(e) => setProfile(prev => ({ ...prev, experienceLevel: e.target.value }))}
                    disabled={!isEditing}
                    className={`w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent dark:bg-gray-700 ${!isEditing ? 'bg-gray-50 dark:bg-gray-600' : ''}`}
                  >
                    {experienceLevelOptions.map(level => (
                      <option key={level} value={level}>{level}</option>
                    ))}
                  </select>
                  <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">
                    经验水平会影响AI内容的技术深度和复杂度
                  </p>
                </div>
              </div>
            )}
          </div>

          {/* 技术技能 */}
          <div>
            <div 
              className="flex justify-between items-center cursor-pointer" 
              onClick={() => toggleSection('technicalSkills')}
            >
              <h3 className="text-md font-semibold text-gray-700 dark:text-gray-300">技术技能</h3>
              {expandedSections.technicalSkills ? (
                <ChevronUpIcon className="w-5 h-5 text-gray-500" />
              ) : (
                <ChevronDownIcon className="w-5 h-5 text-gray-500" />
              )}
            </div>
            {expandedSections.technicalSkills && (
              <div className="mt-4 space-y-4">
                <div className="flex flex-wrap gap-2">
                  {profile.technicalSkills.map((skill, index) => (
                    <div key={index} className="flex items-center space-x-1 bg-indigo-100 dark:bg-indigo-900/30 text-indigo-800 dark:text-indigo-200 rounded-full px-3 py-1">
                      <span className="text-sm">{skill}</span>
                      {isEditing && (
                        <button
                          onClick={() => handleRemoveTechnicalSkill(skill)}
                          className="text-red-500 hover:text-red-700"
                        >
                          <TrashIcon className="w-3 h-3" />
                        </button>
                      )}
                    </div>
                  ))}
                </div>
                {isEditing && (
                  <div className="mt-2">
                    <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">选择技术技能</label>
                    <div className="flex flex-wrap gap-2">
                      {technicalSkillOptions.map(skill => (
                        !profile.technicalSkills.includes(skill) && (
                          <button
                            key={skill}
                            onClick={() => handleAddTechnicalSkill(skill)}
                            className="px-3 py-1 border border-gray-300 dark:border-gray-600 rounded-full hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors text-sm"
                          >
                            {skill}
                          </button>
                        )
                      ))}
                    </div>
                  </div>
                )}
                {!isEditing && (
                  <p className="text-xs text-gray-500 dark:text-gray-400">
                    技术技能会影响AI推荐的技术内容和工具相关信息
                  </p>
                )}
              </div>
            )}
          </div>

          {/* 兴趣爱好 */}
          <div>
            <div 
              className="flex justify-between items-center cursor-pointer" 
              onClick={() => toggleSection('interests')}
            >
              <h3 className="text-md font-semibold text-gray-700 dark:text-gray-300">兴趣爱好</h3>
              {expandedSections.interests ? (
                <ChevronUpIcon className="w-5 h-5 text-gray-500" />
              ) : (
                <ChevronDownIcon className="w-5 h-5 text-gray-500" />
              )}
            </div>
            {expandedSections.interests && (
              <div className="mt-4 space-y-2">
                <div className="flex flex-wrap gap-2">
                  {profile.interests.map((interest, index) => (
                    <div key={index} className="flex items-center space-x-1 bg-gray-100 dark:bg-gray-700 rounded-full px-3 py-1">
                      <span className="text-sm">{interest}</span>
                      {isEditing && (
                        <button
                          onClick={() => handleRemoveInterest(interest)}
                          className="text-red-500 hover:text-red-700"
                        >
                          <TrashIcon className="w-3 h-3" />
                        </button>
                      )}
                    </div>
                  ))}
                </div>
                {isEditing && (
                  <div className="flex gap-2">
                    <input
                      type="text"
                      value={newInterest}
                      onChange={(e) => setNewInterest(e.target.value)}
                      className="flex-1 px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent dark:bg-gray-700"
                      placeholder="添加兴趣爱好"
                    />
                    <button
                      onClick={handleAddInterest}
                      className="px-4 py-2 bg-primary-500 hover:bg-primary-600 text-white rounded-lg transition-colors"
                    >
                      添加
                    </button>
                  </div>
                )}
                {!isEditing && (
                  <p className="text-xs text-gray-500 dark:text-gray-400">
                    兴趣爱好会影响AI推荐的内容主题和相关度
                  </p>
                )}
              </div>
            )}
          </div>

          {/* 偏好话题 */}
          <div>
            <div 
              className="flex justify-between items-center cursor-pointer" 
              onClick={() => toggleSection('preferredTopics')}
            >
              <h3 className="text-md font-semibold text-gray-700 dark:text-gray-300">偏好话题</h3>
              {expandedSections.preferredTopics ? (
                <ChevronUpIcon className="w-5 h-5 text-gray-500" />
              ) : (
                <ChevronDownIcon className="w-5 h-5 text-gray-500" />
              )}
            </div>
            {expandedSections.preferredTopics && (
              <div className="mt-4 space-y-2">
                <div className="flex flex-wrap gap-2">
                  {profile.preferredTopics.map((topic, index) => (
                    <div key={index} className="flex items-center space-x-1 bg-blue-100 dark:bg-blue-900/30 text-blue-800 dark:text-blue-200 rounded-full px-3 py-1">
                      <span className="text-sm">{topic}</span>
                      {isEditing && (
                        <button
                          onClick={() => handleRemoveTopic(topic)}
                          className="text-red-500 hover:text-red-700"
                        >
                          <TrashIcon className="w-3 h-3" />
                        </button>
                      )}
                    </div>
                  ))}
                </div>
                {isEditing && (
                  <div className="flex gap-2">
                    <input
                      type="text"
                      value={newTopic}
                      onChange={(e) => setNewTopic(e.target.value)}
                      className="flex-1 px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent dark:bg-gray-700"
                      placeholder="添加偏好话题"
                    />
                    <button
                      onClick={handleAddTopic}
                      className="px-4 py-2 bg-primary-500 hover:bg-primary-600 text-white rounded-lg transition-colors"
                    >
                      添加
                    </button>
                  </div>
                )}
                {!isEditing && (
                  <p className="text-xs text-gray-500 dark:text-gray-400">
                    偏好话题会影响AI内容的主题方向和重点
                  </p>
                )}
              </div>
            )}
          </div>

          {/* 学习目标 */}
          <div>
            <div 
              className="flex justify-between items-center cursor-pointer" 
              onClick={() => toggleSection('learningGoals')}
            >
              <h3 className="text-md font-semibold text-gray-700 dark:text-gray-300">学习目标</h3>
              {expandedSections.learningGoals ? (
                <ChevronUpIcon className="w-5 h-5 text-gray-500" />
              ) : (
                <ChevronDownIcon className="w-5 h-5 text-gray-500" />
              )}
            </div>
            {expandedSections.learningGoals && (
              <div className="mt-4 space-y-2">
                <div className="flex flex-wrap gap-2">
                  {profile.learningGoals.map((goal, index) => (
                    <div key={index} className="flex items-center space-x-1 bg-amber-100 dark:bg-amber-900/30 text-amber-800 dark:text-amber-200 rounded-full px-3 py-1">
                      <span className="text-sm">{goal}</span>
                      {isEditing && (
                        <button
                          onClick={() => handleRemoveLearningGoal(goal)}
                          className="text-red-500 hover:text-red-700"
                        >
                          <TrashIcon className="w-3 h-3" />
                        </button>
                      )}
                    </div>
                  ))}
                </div>
                {isEditing && (
                  <div className="flex gap-2">
                    <input
                      type="text"
                      placeholder="添加学习目标"
                      onKeyPress={(e) => {
                        if (e.key === 'Enter' && (e.target as HTMLInputElement).value.trim()) {
                          handleAddLearningGoal((e.target as HTMLInputElement).value.trim());
                          (e.target as HTMLInputElement).value = '';
                        }
                      }}
                      className="flex-1 px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent dark:bg-gray-700"
                    />
                    <button
                      onClick={() => {
                        const input = document.querySelector('input[placeholder="添加学习目标"]') as HTMLInputElement;
                        if (input && input.value.trim()) {
                          handleAddLearningGoal(input.value.trim());
                          input.value = '';
                        }
                      }}
                      className="px-4 py-2 bg-primary-500 hover:bg-primary-600 text-white rounded-lg transition-colors"
                    >
                      添加
                    </button>
                  </div>
                )}
                {!isEditing && (
                  <p className="text-xs text-gray-500 dark:text-gray-400">
                    学习目标会影响AI推荐的内容深度和方向，帮助您实现学习计划
                  </p>
                )}
              </div>
            )}
          </div>

          {/* 行业焦点 */}
          <div>
            <div 
              className="flex justify-between items-center cursor-pointer" 
              onClick={() => toggleSection('industryFocus')}
            >
              <h3 className="text-md font-semibold text-gray-700 dark:text-gray-300">行业焦点</h3>
              {expandedSections.industryFocus ? (
                <ChevronUpIcon className="w-5 h-5 text-gray-500" />
              ) : (
                <ChevronDownIcon className="w-5 h-5 text-gray-500" />
              )}
            </div>
            {expandedSections.industryFocus && (
              <div className="mt-4 space-y-4">
                <div className="flex flex-wrap gap-2">
                  {profile.industryFocus.map((industry, index) => (
                    <div key={index} className="flex items-center space-x-1 bg-teal-100 dark:bg-teal-900/30 text-teal-800 dark:text-teal-200 rounded-full px-3 py-1">
                      <span className="text-sm">{industry}</span>
                      {isEditing && (
                        <button
                          onClick={() => handleRemoveIndustry(industry)}
                          className="text-red-500 hover:text-red-700"
                        >
                          <TrashIcon className="w-3 h-3" />
                        </button>
                      )}
                    </div>
                  ))}
                </div>
                {isEditing && (
                  <div className="mt-2">
                    <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">选择行业</label>
                    <div className="flex flex-wrap gap-2">
                      {industryOptions.map(industry => (
                        !profile.industryFocus.includes(industry) && (
                          <button
                            key={industry}
                            onClick={() => handleAddIndustry(industry)}
                            className="px-3 py-1 border border-gray-300 dark:border-gray-600 rounded-full hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors text-sm"
                          >
                            {industry}
                          </button>
                        )
                      ))}
                    </div>
                  </div>
                )}
                {!isEditing && (
                  <p className="text-xs text-gray-500 dark:text-gray-400">
                    行业焦点会影响AI推荐的行业相关内容和趋势分析
                  </p>
                )}
              </div>
            )}
          </div>

          {/* 编码习惯 */}
          <div>
            <div 
              className="flex justify-between items-center cursor-pointer" 
              onClick={() => toggleSection('codingHabits')}
            >
              <h3 className="text-md font-semibold text-gray-700 dark:text-gray-300">编码习惯</h3>
              {expandedSections.codingHabits ? (
                <ChevronUpIcon className="w-5 h-5 text-gray-500" />
              ) : (
                <ChevronDownIcon className="w-5 h-5 text-gray-500" />
              )}
            </div>
            {expandedSections.codingHabits && (
              <div className="mt-4 space-y-2">
                <div className="flex flex-wrap gap-2">
                  {profile.codingHabits.map((habit, index) => (
                    <span key={index} className="bg-emerald-100 dark:bg-emerald-900/30 text-emerald-800 dark:text-emerald-200 rounded-full px-3 py-1 text-sm">
                      {habit}
                    </span>
                  ))}
                </div>
                {isEditing && (
                  <div className="flex gap-2">
                    <input
                      type="text"
                      placeholder="添加编码习惯"
                      onKeyPress={(e) => {
                        if (e.key === 'Enter' && (e.target as HTMLInputElement).value.trim()) {
                          handleAddCodingHabit((e.target as HTMLInputElement).value.trim());
                          (e.target as HTMLInputElement).value = '';
                        }
                      }}
                      className="flex-1 px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent dark:bg-gray-700"
                    />
                    <button
                      onClick={() => {
                        const input = document.querySelector('input[placeholder="添加编码习惯"]') as HTMLInputElement;
                        if (input && input.value.trim()) {
                          handleAddCodingHabit(input.value.trim());
                          input.value = '';
                        }
                      }}
                      className="px-4 py-2 bg-primary-500 hover:bg-primary-600 text-white rounded-lg transition-colors"
                    >
                      添加
                    </button>
                  </div>
                )}
              </div>
            )}
          </div>

          {/* 工具偏好 */}
          <div>
            <div 
              className="flex justify-between items-center cursor-pointer" 
              onClick={() => toggleSection('toolPreferences')}
            >
              <h3 className="text-md font-semibold text-gray-700 dark:text-gray-300">工具偏好</h3>
              {expandedSections.toolPreferences ? (
                <ChevronUpIcon className="w-5 h-5 text-gray-500" />
              ) : (
                <ChevronDownIcon className="w-5 h-5 text-gray-500" />
              )}
            </div>
            {expandedSections.toolPreferences && (
              <div className="mt-4 space-y-2">
                <div className="flex flex-wrap gap-2">
                  {profile.toolPreferences.map((tool, index) => (
                    <div key={index} className="flex items-center space-x-1 bg-purple-100 dark:bg-purple-900/30 text-purple-800 dark:text-purple-200 rounded-full px-3 py-1">
                      <span className="text-sm">{tool}</span>
                      {isEditing && (
                        <button
                          onClick={() => handleRemoveTool(tool)}
                          className="text-red-500 hover:text-red-700"
                        >
                          <TrashIcon className="w-3 h-3" />
                        </button>
                      )}
                    </div>
                  ))}
                </div>
                {isEditing && (
                  <div className="flex gap-2">
                    <input
                      type="text"
                      placeholder="添加工具偏好"
                      onKeyPress={(e) => {
                        if (e.key === 'Enter' && (e.target as HTMLInputElement).value.trim()) {
                          handleAddTool((e.target as HTMLInputElement).value.trim());
                          (e.target as HTMLInputElement).value = '';
                        }
                      }}
                      className="flex-1 px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent dark:bg-gray-700"
                    />
                    <button
                      onClick={() => {
                        const input = document.querySelector('input[placeholder="添加工具偏好"]') as HTMLInputElement;
                        if (input && input.value.trim()) {
                          handleAddTool(input.value.trim());
                          input.value = '';
                        }
                      }}
                      className="px-4 py-2 bg-primary-500 hover:bg-primary-600 text-white rounded-lg transition-colors"
                    >
                      添加
                    </button>
                  </div>
                )}
              </div>
            )}
          </div>

          {/* 内容格式偏好 */}
          <div>
            <div 
              className="flex justify-between items-center cursor-pointer" 
              onClick={() => toggleSection('contentFormats')}
            >
              <h3 className="text-md font-semibold text-gray-700 dark:text-gray-300">内容格式偏好</h3>
              {expandedSections.contentFormats ? (
                <ChevronUpIcon className="w-5 h-5 text-gray-500" />
              ) : (
                <ChevronDownIcon className="w-5 h-5 text-gray-500" />
              )}
            </div>
            {expandedSections.contentFormats && (
              <div className="mt-4 space-y-4">
                <div className="flex flex-wrap gap-2">
                  {profile.contentFormats.map((format, index) => (
                    <div key={index} className="flex items-center space-x-1 bg-yellow-100 dark:bg-yellow-900/30 text-yellow-800 dark:text-yellow-200 rounded-full px-3 py-1">
                      <span className="text-sm">{format}</span>
                      {isEditing && (
                        <button
                          onClick={() => handleRemoveContentFormat(format)}
                          className="text-red-500 hover:text-red-700"
                        >
                          <TrashIcon className="w-3 h-3" />
                        </button>
                      )}
                    </div>
                  ))}
                </div>
                {isEditing && (
                  <div className="mt-2">
                    <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">选择内容格式</label>
                    <div className="flex flex-wrap gap-2">
                      {contentFormatOptions.map(format => (
                        !profile.contentFormats.includes(format) && (
                          <button
                            key={format}
                            onClick={() => handleAddContentFormat(format)}
                            className="px-3 py-1 border border-gray-300 dark:border-gray-600 rounded-full hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors text-sm"
                          >
                            {format}
                          </button>
                        )
                      ))}
                    </div>
                  </div>
                )}
                {!isEditing && (
                  <p className="text-xs text-gray-500 dark:text-gray-400">
                    内容格式偏好会影响AI推荐的内容类型和呈现方式
                  </p>
                )}
              </div>
            )}
          </div>

          {/* 时间可用性 */}
          <div>
            <div 
              className="flex justify-between items-center cursor-pointer" 
              onClick={() => toggleSection('timeAvailability')}
            >
              <h3 className="text-md font-semibold text-gray-700 dark:text-gray-300">时间可用性</h3>
              {expandedSections.timeAvailability ? (
                <ChevronUpIcon className="w-5 h-5 text-gray-500" />
              ) : (
                <ChevronDownIcon className="w-5 h-5 text-gray-500" />
              )}
            </div>
            {expandedSections.timeAvailability && (
              <div className="mt-4 space-y-4">
                <div className="flex flex-wrap gap-2">
                  {profile.timeAvailability.map((time, index) => (
                    <div key={index} className="flex items-center space-x-1 bg-pink-100 dark:bg-pink-900/30 text-pink-800 dark:text-pink-200 rounded-full px-3 py-1">
                      <span className="text-sm">{time}</span>
                      {isEditing && (
                        <button
                          onClick={() => handleRemoveTimeAvailability(time)}
                          className="text-red-500 hover:text-red-700"
                        >
                          <TrashIcon className="w-3 h-3" />
                        </button>
                      )}
                    </div>
                  ))}
                </div>
                {isEditing && (
                  <div className="mt-2">
                    <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">选择时间可用性</label>
                    <div className="flex flex-wrap gap-2">
                      {timeAvailabilityOptions.map(time => (
                        !profile.timeAvailability.includes(time) && (
                          <button
                            key={time}
                            onClick={() => handleAddTimeAvailability(time)}
                            className="px-3 py-1 border border-gray-300 dark:border-gray-600 rounded-full hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors text-sm"
                          >
                            {time}
                          </button>
                        )
                      ))}
                    </div>
                  </div>
                )}
                {!isEditing && (
                  <p className="text-xs text-gray-500 dark:text-gray-400">
                    时间可用性会影响AI推荐的内容长度和复杂度，匹配您的可用时间
                  </p>
                )}
              </div>
            )}
          </div>

          {/* 最近活动 */}
          <div>
            <div 
              className="flex justify-between items-center cursor-pointer" 
              onClick={() => toggleSection('recentActivities')}
            >
              <h3 className="text-md font-semibold text-gray-700 dark:text-gray-300">最近活动</h3>
              {expandedSections.recentActivities ? (
                <ChevronUpIcon className="w-5 h-5 text-gray-500" />
              ) : (
                <ChevronDownIcon className="w-5 h-5 text-gray-500" />
              )}
            </div>
            {expandedSections.recentActivities && (
              <div className="mt-4 space-y-4">
                <div className="flex flex-wrap gap-2">
                  {profile.recentActivities.map((activity, index) => (
                    <div key={index} className="flex items-center space-x-1 bg-green-100 dark:bg-green-900/30 text-green-800 dark:text-green-200 rounded-full px-3 py-1">
                      <span className="text-sm">{activity}</span>
                      {isEditing && (
                        <button
                          onClick={() => handleRemoveActivity(activity)}
                          className="text-red-500 hover:text-red-700"
                        >
                          <TrashIcon className="w-3 h-3" />
                        </button>
                      )}
                    </div>
                  ))}
                </div>
                {isEditing && (
                  <div className="flex gap-2">
                    <input
                      type="text"
                      value={newActivity}
                      onChange={(e) => setNewActivity(e.target.value)}
                      className="flex-1 px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent dark:bg-gray-700"
                      placeholder="添加最近活动（例如：学习AI Agent、参加技术会议）"
                    />
                    <button
                      onClick={handleAddActivity}
                      className="px-4 py-2 bg-primary-500 hover:bg-primary-600 text-white rounded-lg transition-colors"
                    >
                      添加
                    </button>
                  </div>
                )}
                {!isEditing && (
                  <p className="text-xs text-gray-500 dark:text-gray-400">
                    最近活动会影响AI对您兴趣的判断，建议填写真实的活动
                  </p>
                )}
              </div>
            )}
          </div>

          {/* 阅读习惯 */}
          <div>
            <div 
              className="flex justify-between items-center cursor-pointer" 
              onClick={() => toggleSection('readingHabits')}
            >
              <h3 className="text-md font-semibold text-gray-700 dark:text-gray-300">阅读习惯</h3>
              {expandedSections.readingHabits ? (
                <ChevronUpIcon className="w-5 h-5 text-gray-500" />
              ) : (
                <ChevronDownIcon className="w-5 h-5 text-gray-500" />
              )}
            </div>
            {expandedSections.readingHabits && (
              <div className="mt-4 space-y-4">
                <div className="flex flex-wrap gap-2">
                  {profile.readingHabits.map((habit, index) => (
                    <div key={index} className="flex items-center space-x-1 bg-purple-100 dark:bg-purple-900/30 text-purple-800 dark:text-purple-200 rounded-full px-3 py-1">
                      <span className="text-sm">{habit}</span>
                      {isEditing && (
                        <button
                          onClick={() => handleRemoveReadingHabit(habit)}
                          className="text-red-500 hover:text-red-700"
                        >
                          <TrashIcon className="w-3 h-3" />
                        </button>
                      )}
                    </div>
                  ))}
                </div>
                {isEditing && (
                  <div className="mt-2">
                    <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">选择阅读习惯</label>
                    <div className="flex flex-wrap gap-2">
                      {readingHabitOptions.map(habit => (
                        !profile.readingHabits.includes(habit) && (
                          <button
                            key={habit}
                            onClick={() => handleAddReadingHabit(habit)}
                            className="px-3 py-1 border border-gray-300 dark:border-gray-600 rounded-full hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors text-sm"
                          >
                            {habit}
                          </button>
                        )
                      ))}
                    </div>
                  </div>
                )}
                {!isEditing && (
                  <p className="text-xs text-gray-500 dark:text-gray-400">
                    阅读习惯会影响AI推荐的内容格式和深度
                  </p>
                )}
              </div>
            )}
          </div>

          {/* 内容偏好 */}
          <div>
            <div 
              className="flex justify-between items-center cursor-pointer" 
              onClick={() => toggleSection('contentPreferences')}
            >
              <h3 className="text-md font-semibold text-gray-700 dark:text-gray-300">内容偏好</h3>
              {expandedSections.contentPreferences ? (
                <ChevronUpIcon className="w-5 h-5 text-gray-500" />
              ) : (
                <ChevronDownIcon className="w-5 h-5 text-gray-500" />
              )}
            </div>
            {expandedSections.contentPreferences && (
              <div className="mt-4 space-y-4">
                <div className="flex flex-wrap gap-2">
                  {profile.contentPreferences.map((preference, index) => (
                    <div key={index} className="flex items-center space-x-1 bg-yellow-100 dark:bg-yellow-900/30 text-yellow-800 dark:text-yellow-200 rounded-full px-3 py-1">
                      <span className="text-sm">{preference}</span>
                      {isEditing && (
                        <button
                          onClick={() => handleRemoveContentPreference(preference)}
                          className="text-red-500 hover:text-red-700"
                        >
                          <TrashIcon className="w-3 h-3" />
                        </button>
                      )}
                    </div>
                  ))}
                </div>
                {isEditing && (
                  <div className="mt-2">
                    <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">选择内容偏好</label>
                    <div className="flex flex-wrap gap-2">
                      {contentPreferenceOptions.map(preference => (
                        !profile.contentPreferences.includes(preference) && (
                          <button
                            key={preference}
                            onClick={() => handleAddContentPreference(preference)}
                            className="px-3 py-1 border border-gray-300 dark:border-gray-600 rounded-full hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors text-sm"
                          >
                            {preference}
                          </button>
                        )
                      ))}
                    </div>
                  </div>
                )}
                {!isEditing && (
                  <p className="text-xs text-gray-500 dark:text-gray-400">
                    内容偏好会影响AI生成的内容风格和重点
                  </p>
                )}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}

export default ProfileManagement
type ProfileSection =
  | 'basicInfo'
  | 'technicalSkills'
  | 'interests'
  | 'preferredTopics'
  | 'recentActivities'
  | 'readingHabits'
  | 'contentPreferences'
  | 'learningGoals'
  | 'industryFocus'
  | 'codingHabits'
  | 'toolPreferences'
  | 'contentFormats'
  | 'timeAvailability'
