import { Outlet, Link, useLocation } from 'react-router-dom'
import { HomeIcon, UserIcon, CogIcon } from '@heroicons/react/24/outline'

const Layout: React.FC = () => {
  const location = useLocation()
  
  const getPageTitle = () => {
    switch (location.pathname) {
      case '/':
        return '首页'
      case '/profile':
        return '用户画像'
      case '/settings':
        return '设置'
      case '/config':
        return '系统配置'
      default:
        return '首页'
    }
  }

  return (
    <div className="flex h-screen bg-gray-50 text-gray-900 dark:bg-gray-900 dark:text-gray-100">
      {/* 侧边栏 */}
      <div className="w-64 bg-white dark:bg-gray-800 shadow-lg flex flex-col">
        {/* 品牌标识 */}
        <div className="p-4 border-b border-gray-200 dark:border-gray-700">
          <h1 className="text-xl font-bold text-primary-600 dark:text-primary-400">AI 送报员</h1>
          <p className="text-xs text-gray-500 dark:text-gray-400">智能信息管理系统</p>
        </div>
        
        {/* 导航菜单 */}
        <nav className="flex-1 p-4 space-y-2">
          <NavItem href="/" icon={<HomeIcon className="w-5 h-5" />} label="首页" currentPath={location.pathname} />
          <NavItem href="/profile" icon={<UserIcon className="w-5 h-5" />} label="用户画像" currentPath={location.pathname} />
          <NavItem href="/settings" icon={<CogIcon className="w-5 h-5" />} label="设置" currentPath={location.pathname} />
          <NavItem href="/config" icon={<CogIcon className="w-5 h-5" />} label="系统配置" currentPath={location.pathname} />
        </nav>
      </div>
      
      {/* 主内容区 */}
      <div className="flex-1 flex flex-col overflow-hidden">
        {/* 顶部导航栏 */}
        <header className="bg-white dark:bg-gray-800 shadow-sm z-10">
          <div className="flex items-center justify-between p-4">
            <h2 className="text-lg font-semibold">{getPageTitle()}</h2>
            <div className="flex items-center space-x-4">
              <button className="p-2 rounded-full hover:bg-gray-100 dark:hover:bg-gray-700">
                <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                </svg>
              </button>
              <div className="w-8 h-8 rounded-full bg-primary-500 flex items-center justify-center text-white font-medium">
                AD
              </div>
            </div>
          </div>
        </header>
        
        {/* 内容区域 */}
        <main className="flex-1 overflow-y-auto p-6">
          <Outlet />
        </main>
      </div>
    </div>
  )
}

interface NavItemProps {
  href: string
  icon: React.ReactNode
  label: string
  currentPath: string
}

const NavItem: React.FC<NavItemProps> = ({ href, icon, label, currentPath }) => {
  const isActive = currentPath === href
  
  return (
    <Link
      to={href}
      className={`flex items-center space-x-3 px-4 py-2 rounded-lg transition-colors ${isActive
        ? 'bg-primary-100 dark:bg-primary-900/30 text-primary-600 dark:text-primary-400'
        : 'hover:bg-gray-100 dark:hover:bg-gray-700 text-gray-700 dark:text-gray-300 hover:text-primary-600 dark:hover:text-primary-400'
        }`}
    >
      {icon}
      <span>{label}</span>
    </Link>
  )
}

export default Layout
