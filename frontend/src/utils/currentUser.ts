export const CURRENT_USER_ID_KEY = 'currentUserId'

export const getCurrentUserId = (): number => {
  if (typeof window === 'undefined') {
    return 1
  }
  const raw = window.localStorage.getItem(CURRENT_USER_ID_KEY)
  const parsed = raw ? Number(raw) : NaN
  return Number.isInteger(parsed) && parsed > 0 ? parsed : 1
}

export const setCurrentUserId = (userId: number): void => {
  if (typeof window === 'undefined') {
    return
  }
  if (!Number.isInteger(userId) || userId <= 0) {
    return
  }
  window.localStorage.setItem(CURRENT_USER_ID_KEY, String(userId))
}
