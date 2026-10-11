import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  ArrowDown, ArrowUp, ArrowUpDown, BriefcaseBusiness, Building2, Check,
  ChevronDown, ChevronLeft, ChevronRight, CircleHelp, Command, LayoutDashboard,
  LogOut, Menu, MoreHorizontal, Plus, Search, Settings2, ShieldCheck, Users,
  ArrowLeft, CalendarDays, ClipboardCheck, Clock3, Moon, PencilLine, Sun, UserRound, X,
} from 'lucide-react'

const TOKEN_KEY = 'peopleos_token'
const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '')
const initials = (first = '', last = '') => `${first[0] || ''}${last[0] || ''}`.toUpperCase()
const palette = ['lavender', 'mint', 'peach', 'blue', 'rose', 'yellow']
const currency = (value) => value == null ? '—' : new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 0 }).format(value)
const localDate = () => { const date = new Date(); return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}` }

async function api(path, options = {}) {
  const token = localStorage.getItem(TOKEN_KEY)
  const response = await fetch(`${API_BASE_URL}/api${path}`, {
    ...options,
    headers: {
      ...(options.body ? { 'Content-Type': 'application/json' } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers,
    },
  })
  if (response.status === 401) throw new Error('Your session expired. Please sign in again.')
  if (response.status === 403) throw new Error('You do not have permission to do that.')
  if (!response.ok) {
    let message = `Request failed (${response.status})`
    try {
      const data = await response.json()
      message = data.message || data.error || message
    } catch { /* use status fallback */ }
    throw new Error(message)
  }
  if (response.status === 204) return null
  const text = await response.text()
  if (!text) return null
  try { return JSON.parse(text) } catch { return text }
}

function App() {
  const [token, setToken] = useState(() => localStorage.getItem(TOKEN_KEY))
  const [theme, setTheme] = useState(() => localStorage.getItem('peopleos_theme') || 'light')
  const [employees, setEmployees] = useState([])
  const [departments, setDepartments] = useState([])
  const [currentUser, setCurrentUser] = useState(null)
  const [employeeUsers, setEmployeeUsers] = useState([])
  const [accountsIssue, setAccountsIssue] = useState('')
  const [selectedEmployee, setSelectedEmployee] = useState(null)
  const [employeeInfo, setEmployeeInfo] = useState(null)
  const [leaveRequests, setLeaveRequests] = useState([])
  const [leaveTypes, setLeaveTypes] = useState([])
  const [attendanceRows, setAttendanceRows] = useState([])
  const [attendanceDate, setAttendanceDate] = useState(localDate)
  const [activeView, setActiveView] = useState('people')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [query, setQuery] = useState('')
  const [departmentFilter, setDepartmentFilter] = useState('all')
  const [sort, setSort] = useState({ key: 'firstName', direction: 'asc' })
  const [page, setPage] = useState(1)
  const [modal, setModal] = useState(null)
  const [menuId, setMenuId] = useState(null)
  const [mobileNav, setMobileNav] = useState(false)
  const [notice, setNotice] = useState('')

  useEffect(() => {
    document.documentElement.dataset.theme = theme
    localStorage.setItem('peopleos_theme', theme)
  }, [theme])

  const loadData = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const profile = await api('/auth/me')
      setCurrentUser(profile)
      const approver = profile.role === 'ROLE_LEAVE_APPROVER'
      const [people, teams] = approver
        ? await Promise.all([api('/employees'), api('/departments')])
        : [await api('/employees/mine').catch((e) => {
          if (e.message.includes('No employee profile')) return null
          throw e
        }), []]
      const employeeRows = Array.isArray(people) ? people : people ? [people] : []
      setEmployees(approver ? employeeRows : employeeRows.filter((person) => person.id === profile.employeeId))
      setDepartments(Array.isArray(teams) ? teams : [])
      if (approver) {
        setActiveView('people')
        setAccountsIssue('')
        try {
          const accounts = await api('/auth/users')
          setEmployeeUsers(Array.isArray(accounts) ? accounts : [])
        } catch (e) {
          setEmployeeUsers([])
          setAccountsIssue(`Could not load user accounts: ${e.message}`)
        }
      } else {
        setEmployeeUsers([])
        setAccountsIssue('')
        const ownEmployee = employeeRows.find((person) => person.id === profile.employeeId)
        if (ownEmployee) {
          setSelectedEmployee({ ...ownEmployee, departmentName: ownEmployee.departmentName || 'Unassigned' })
          setActiveView('my-profile')
        } else {
          setSelectedEmployee(null)
          setActiveView('leaves')
        }
      }
    } catch (e) {
      if (e.message.includes('session expired')) {
        localStorage.removeItem(TOKEN_KEY)
        setToken(null)
      } else setError(`${e.message}. Make sure the Spring Boot backend is running on port 8080.`)
    } finally { setLoading(false) }
  }, [])

  useEffect(() => { if (token) loadData() }, [token, loadData])

  useEffect(() => {
    if (!token || !['employee', 'my-profile'].includes(activeView) || !selectedEmployee) return
    Promise.all([api(`/employees/${selectedEmployee.id}/info`), api(`/leaves?employeeId=${selectedEmployee.id}`)])
      .then(([info, requests]) => { setEmployeeInfo(info); setLeaveRequests(requests) })
      .catch((e) => setError(e.message))
  }, [token, activeView, selectedEmployee])

  useEffect(() => {
    if (!token || activeView !== 'leaves') return
    Promise.all([currentUser?.role === 'ROLE_LEAVE_APPROVER' || currentUser?.employeeId ? api('/leaves') : Promise.resolve([]), api('/leaves/types')])
      .then(([requests, types]) => { setLeaveRequests(requests); setLeaveTypes(types) })
      .catch((e) => setError(e.message))
  }, [token, activeView, currentUser])

  useEffect(() => {
    if (!token || activeView !== 'attendance' || !currentUser) return
    const path = currentUser.role === 'ROLE_LEAVE_APPROVER'
      ? `/attendance/date/${attendanceDate}`
      : currentUser.employeeId ? `/attendance/employee/${currentUser.employeeId}` : null
    if (!path) { setAttendanceRows([]); return }
    api(path).then((rows) => setAttendanceRows(Array.isArray(rows) ? rows : [])).catch((e) => setError(e.message))
  }, [token, activeView, currentUser, attendanceDate])

  const filtered = useMemo(() => {
    const needle = query.trim().toLowerCase()
    const list = employees.filter((person) => {
      const department = departments.find((d) => d.id === person.departmentId)?.name || ''
      const matchesQuery = !needle || `${person.firstName} ${person.lastName} ${person.email} ${department}`.toLowerCase().includes(needle)
      return matchesQuery && (departmentFilter === 'all' || String(person.departmentId || '') === departmentFilter)
    })
    return list.sort((a, b) => {
      const av = sort.key === 'department' ? departments.find((d) => d.id === a.departmentId)?.name || '' : a[sort.key] || ''
      const bv = sort.key === 'department' ? departments.find((d) => d.id === b.departmentId)?.name || '' : b[sort.key] || ''
      return String(av).localeCompare(String(bv), undefined, { numeric: true }) * (sort.direction === 'asc' ? 1 : -1)
    })
  }, [employees, departments, query, departmentFilter, sort])
  const pageSize = 8
  const pageCount = Math.max(1, Math.ceil(filtered.length / pageSize))
  const visible = filtered.slice((page - 1) * pageSize, page * pageSize)
  const departmentName = (id) => departments.find((item) => item.id === id)?.name || 'Unassigned'
  const notify = (text) => { setNotice(text); window.setTimeout(() => setNotice(''), 2600) }

  function signOut() {
    localStorage.removeItem(TOKEN_KEY)
    setToken(null)
    setEmployees([])
    setCurrentUser(null)
    setActiveView('people')
  }

  async function saveEmployee(form) {
    const payload = {
      firstName: form.firstName.trim(), lastName: form.lastName.trim(), email: form.email.trim(),
      salary: form.salary ? Number(form.salary) : null,
      departmentId: form.departmentId ? Number(form.departmentId) : null,
      userId: form.userId ? Number(form.userId) : null,
    }
    try {
      if (modal?.employee) await api(`/employees/${modal.employee.id}`, { method: 'PUT', body: JSON.stringify(payload) })
      else await api('/employees', { method: 'POST', body: JSON.stringify(payload) })
      setModal(null)
      await loadData()
      notify(modal?.employee ? 'Employee details updated' : 'Employee added to your team')
    } catch (e) { throw e }
  }

  async function saveDepartment(name) {
    await api('/departments', { method: 'POST', body: JSON.stringify({ name: name.trim() }) })
    setModal(null)
    await loadData()
    notify(`${name.trim()} department created`)
  }

  function openEmployee(person) {
    setSelectedEmployee({ ...person, departmentName: departmentName(person.departmentId) })
    setEmployeeInfo(null)
    setLeaveRequests([])
    setActiveView('employee')
  }

  function openMyProfile() {
    const ownEmployee = employees.find((person) => Number(person.id) === Number(currentUser?.employeeId))
    setSelectedEmployee(ownEmployee ? { ...ownEmployee, departmentName: departmentName(ownEmployee.departmentId) } : null)
    setEmployeeInfo(null)
    setLeaveRequests([])
    setActiveView('my-profile')
    setMobileNav(false)
  }

  async function saveEmployeeInfo(info) {
    const saved = await api(`/employees/${selectedEmployee.id}/info`, { method: 'PUT', body: JSON.stringify(info) })
    setEmployeeInfo(saved)
    notify('Employee profile updated')
  }

  async function submitLeave(request) {
    await api('/leaves', { method: 'POST', body: JSON.stringify(request) })
    const requests = await api('/leaves')
    setLeaveRequests(requests)
    notify('Leave request submitted for approval')
  }

  async function decideLeave(id, status, note = '') {
    const updated = await api(`/leaves/${id}/decision`, { method: 'PATCH', body: JSON.stringify({ status, note }) })
    setLeaveRequests((current) => current.map((request) => request.id === id ? updated : request))
    notify(`Leave request ${status.toLowerCase()}`)
  }

  function viewDepartmentPeople(departmentId) {
    setDepartmentFilter(String(departmentId))
    setQuery('')
    setPage(1)
    setActiveView('people')
  }

  async function deleteEmployee(person) {
    setMenuId(null)
    if (!window.confirm(`Remove ${person.firstName} ${person.lastName} from the team?`)) return
    try {
      await api(`/employees/${person.id}`, { method: 'DELETE' })
      await loadData()
      notify('Employee removed')
    } catch (e) { setError(e.message) }
  }

  if (!token) return <Login theme={theme} setTheme={setTheme} onLogin={(newToken) => { localStorage.setItem(TOKEN_KEY, newToken); setToken(newToken) }} />

  const departmentsWithCounts = departments.map((department) => ({ ...department, count: employees.filter((person) => person.departmentId === department.id).length }))

  return (
    <div className="app-shell">
      <aside className={`sidebar ${mobileNav ? 'sidebar-open' : ''}`}>
        <div className="brand"><div className="brand-mark"><Command size={19} strokeWidth={2.6} /></div><span>people<span className="brand-light">OS</span></span><button className="mobile-close icon-button" onClick={() => setMobileNav(false)}><X size={18} /></button></div>
        <div className="workspace-label">WORKSPACE</div>
        <button className="workspace-switch"><span className="workspace-avatar">A</span><span className="workspace-copy"><b>Acme Studio</b><small>Free plan</small></span><ChevronDown size={16} className="muted-icon" /></button>
        <div className="nav-section-label">MENU</div>
        <nav className="navigation">
          {currentUser?.role === 'ROLE_LEAVE_APPROVER' ? <>
            <button className={`nav-item ${activeView === 'people' ? 'active' : ''}`} onClick={() => { setActiveView('people'); setDepartmentFilter('all'); setMobileNav(false) }}><LayoutDashboard size={18} /><span>Overview</span></button>
            <button className={`nav-item ${activeView === 'people' || activeView === 'employee' ? 'active' : ''}`} onClick={() => { setActiveView('people'); setDepartmentFilter('all'); setMobileNav(false) }}><Users size={18} /><span>People</span><span className="nav-count">{employees.length}</span></button>
            <button className={`nav-item ${activeView === 'departments' ? 'active' : ''}`} onClick={() => { setActiveView('departments'); setMobileNav(false) }}><BriefcaseBusiness size={18} /><span>Departments</span><span className="nav-count">{departments.length}</span></button>
          </> : null}
          {currentUser?.role !== 'ROLE_LEAVE_APPROVER' && <button className={`nav-item ${activeView === 'my-profile' ? 'active' : ''}`} onClick={openMyProfile}><UserRound size={18} /><span>My profile</span></button>}
          <button className={`nav-item ${activeView === 'leaves' ? 'active' : ''}`} onClick={() => { setActiveView('leaves'); setMobileNav(false) }}><CalendarDays size={18} /><span>{currentUser?.role === 'ROLE_LEAVE_APPROVER' ? 'Leave approvals' : 'My leave'}</span></button>
          <button className={`nav-item ${activeView === 'attendance' ? 'active' : ''}`} onClick={() => { setActiveView('attendance'); setMobileNav(false) }}><Clock3 size={18} /><span>{currentUser?.role === 'ROLE_LEAVE_APPROVER' ? 'Attendance' : 'My attendance'}</span></button>
        </nav>
        {currentUser?.role === 'ROLE_LEAVE_APPROVER' && <><div className="sidebar-divider" /><div className="nav-section-label">WORKSPACE</div><nav className="navigation"><button className="nav-item"><Settings2 size={18} /><span>Settings</span></button><button className="nav-item"><CircleHelp size={18} /><span>Help & support</span></button></nav></>}
        <div className="sidebar-bottom">{currentUser?.role === 'ROLE_LEAVE_APPROVER' && <div className="plan-card"><div className="plan-icon"><ShieldCheck size={16} /></div><div><b>Your workspace</b><p>People are your best asset.<br />Keep your team in sync.</p></div><button aria-label="Close" className="plan-close"><X size={14} /></button><button className="upgrade-link">Explore PeopleOS <span>↗</span></button></div>}<button onClick={signOut} className="profile-row"><div className="profile-avatar">{initials(currentUser?.username?.[0] || 'M', '')}</div><span className="profile-copy"><b>{currentUser?.username || 'My account'}</b><small>{currentUser?.role === 'ROLE_LEAVE_APPROVER' ? 'Leave approver' : 'Employee account'}</small></span><LogOut size={17} className="logout-icon" /></button></div>
      </aside>
      {mobileNav && <button aria-label="Close menu" className="scrim" onClick={() => setMobileNav(false)} />}
      <main className="main-area">
        <header className="topbar"><button onClick={() => setMobileNav(true)} className="mobile-menu icon-button"><Menu size={20} /></button><div className="breadcrumbs"><span>Workspace</span><ChevronRight size={14} /><b>{activeView === 'people' ? 'People' : activeView === 'departments' ? 'Departments' : activeView === 'employee' ? selectedEmployee?.firstName : activeView === 'my-profile' ? 'My profile' : activeView === 'attendance' ? 'Attendance' : 'Leave'}</b></div><div className="topbar-right"><span className="topbar-date">{new Intl.DateTimeFormat('en', { weekday: 'long', month: 'long', day: 'numeric' }).format(new Date())}</span><ThemeToggle theme={theme} setTheme={setTheme} /><div className="topbar-avatar">{initials(currentUser?.username?.[0] || 'M', '')}</div></div></header>
        <div className="page-content">
          {activeView !== 'employee' && activeView !== 'my-profile' && activeView !== 'leaves' && activeView !== 'attendance' && <div className="page-heading"><div><div className="eyebrow"><span className="eyebrow-dot" /> YOUR TEAM</div><h1>{activeView === 'people' ? 'People' : 'Departments'} <span className="heading-count">{activeView === 'people' ? employees.length : departments.length}</span></h1><p className="page-subtitle">{activeView === 'people' ? (departmentFilter === 'all' ? 'Manage your people, all in one place.' : `Employees in ${departmentName(Number(departmentFilter))}.`) : 'Organize your people into teams.'}</p></div>{currentUser?.role === 'ROLE_LEAVE_APPROVER' && <button className="primary-button" onClick={() => setModal(activeView === 'people' ? { employee: null } : { department: true })}><Plus size={17} strokeWidth={2.5} /> {activeView === 'people' ? 'Add employee' : 'Add department'}</button>}</div>}
          {error && <div className="error-banner"><span>{error}</span><button onClick={() => setError('')}><X size={16} /></button></div>}
          {activeView === 'people' ? <>
          <section className="summary-grid">
            <SummaryCard icon={<Users size={17} />} label="Total people" value={employees.length} foot="Across your organization" tone="violet" />
            <SummaryCard icon={<Building2 size={17} />} label="Departments" value={departments.length} foot="Teams working together" tone="amber" />
            <SummaryCard icon={<BriefcaseBusiness size={17} />} label="Avg. salary" value={employees.length ? currency(employees.reduce((n, p) => n + (Number(p.salary) || 0), 0) / employees.length) : '$0'} foot="Across all employees" tone="green" />
          </section>
          <section className="team-section"><div className="section-heading"><div><div className="section-title-row"><h2>Your people</h2><span className="subtle-count">{filtered.length} members</span></div><p>Everyone who makes your team great.</p></div><button className="filter-button"><span className="status-dot" /> All members <ChevronDown size={15} /></button></div>
            <div className="department-strip"><div className="department-label">TEAMS</div><button onClick={() => { setDepartmentFilter('all'); setPage(1) }} className={`department-chip ${departmentFilter === 'all' ? 'selected' : ''}`}><span className="dept-icon all-icon"><Users size={14} /></span><span>All people</span><span className="chip-count">{employees.length}</span></button>{departmentsWithCounts.map((dept, i) => <button key={dept.id} onClick={() => { setDepartmentFilter(String(dept.id)); setPage(1) }} className={`department-chip ${departmentFilter === String(dept.id) ? 'selected' : ''}`}><span className={`dept-icon dept-${i % 4}`}><Building2 size={14} /></span><span>{dept.name}</span><span className="chip-count">{dept.count}</span></button>)}{currentUser?.role === 'ROLE_LEAVE_APPROVER' && <button className="add-team" onClick={() => setModal({ department: true })}><Plus size={14} /> Add team</button>}</div>
            <div className="table-toolbar"><div className="search-field"><Search size={16} /><input value={query} placeholder="Search people..." onChange={(e) => { setQuery(e.target.value); setPage(1) }} /><kbd>⌘ K</kbd></div><button className="toolbar-filter" onClick={() => { setDepartmentFilter('all'); setQuery('') }}><Settings2 size={15} /> Filters{(query || departmentFilter !== 'all') && <span className="filter-active" />}</button><button className="toolbar-more" aria-label="More options"><MoreHorizontal size={18} /></button></div>
            <div className="table-wrap"><table><thead><tr><th><input type="checkbox" aria-label="Select all people" /></th><SortHead label="NAME" sortKey="firstName" sort={sort} setSort={setSort} /><SortHead label="DEPARTMENT" sortKey="department" sort={sort} setSort={setSort} /><SortHead label="EMAIL ADDRESS" sortKey="email" sort={sort} setSort={setSort} /><SortHead label="SALARY" sortKey="salary" sort={sort} setSort={setSort} /><th className="actions-head" /></tr></thead><tbody>
              {loading ? <tr><td colSpan="6" className="empty-state"><span className="spinner" /> Loading your people...</td></tr> : visible.map((person, index) => <tr key={person.id}><td><input type="checkbox" aria-label={`Select ${person.firstName}`} /></td><td><div className="person-cell"><div className={`person-avatar ${palette[(index + (page - 1) * pageSize) % palette.length]}`}>{initials(person.firstName, person.lastName)}</div><div className="person-info"><button className="person-name-link" onClick={() => openEmployee(person)}><b>{person.firstName} {person.lastName}</b></button><small>Employee · #{String(person.id).padStart(4, '0')}</small></div></div></td><td><span className="department-pill"><span className="small-dept-dot" />{departmentName(person.departmentId)}</span></td><td className="email-cell">{person.email}</td><td className="salary-cell">{currency(person.salary)}</td><td className="row-actions">{currentUser?.role === 'ROLE_LEAVE_APPROVER' && <><button className="row-more" aria-label="Employee options" onClick={() => setMenuId(menuId === person.id ? null : person.id)}><MoreHorizontal size={19} /></button>{menuId === person.id && <div className="row-menu"><button onClick={() => { setModal({ employee: person }); setMenuId(null) }}>Edit details</button><button className="danger-action" onClick={() => deleteEmployee(person)}>Remove employee</button></div>}</>}</td></tr>)}
              {!loading && visible.length === 0 && <tr><td colSpan="6" className="empty-state"><div className="empty-icon"><Users size={20} /></div><b>{employees.length ? 'No people found' : 'Your team starts here'}</b><span>{employees.length ? 'Try a different name or team.' : 'Add your first employee to get started.'}</span>{!employees.length && <button className="primary-button compact-button" onClick={() => setModal({ employee: null })}><Plus size={15} /> Add employee</button>}</td></tr>}
            </tbody></table></div>
            <div className="table-footer"><span>Showing <b>{filtered.length ? (page - 1) * pageSize + 1 : 0}–{Math.min(page * pageSize, filtered.length)}</b> of <b>{filtered.length}</b> people</span><div className="pagination"><button disabled={page <= 1} onClick={() => setPage(page - 1)} aria-label="Previous page"><ChevronLeft size={16} /></button><span>Page <b>{page}</b> of <b>{pageCount}</b></span><button disabled={page >= pageCount} onClick={() => setPage(page + 1)} aria-label="Next page"><ChevronRight size={16} /></button></div></div>
          </section>
          </> : activeView === 'departments' ? <section className="department-view"><div className="section-heading"><div><div className="section-title-row"><h2>All departments</h2><span className="subtle-count">{departments.length} teams</span></div><p>Teams and the people in each one.</p></div></div>{loading ? <div className="department-loading"><span className="spinner" /> Loading departments…</div> : departments.length ? <div className="department-cards">{departmentsWithCounts.map((dept, i) => { const members = employees.filter((person) => person.departmentId === dept.id); return <article className="department-card" key={dept.id}><div className="department-card-top"><span className={`dept-icon dept-${i % 4}`}><Building2 size={17} /></span><span className="dept-id">DEPARTMENT {String(dept.id).padStart(2, '0')}</span></div><h3>{dept.name}</h3><p>{dept.count} {dept.count === 1 ? 'team member' : 'team members'}</p><div className="department-members">{members.length ? members.slice(0, 5).map((person, j) => <span title={`${person.firstName} ${person.lastName}`} className={`person-avatar ${palette[j % palette.length]}`} key={person.id}>{initials(person.firstName, person.lastName)}</span>) : <span className="no-members">No members assigned yet</span>}{members.length > 5 && <span className="more-members">+{members.length - 5}</span>}</div><button className="department-people-link" onClick={() => viewDepartmentPeople(dept.id)}>{members.length ? 'View employees' : 'View department'} <ChevronRight size={14} /></button></article> })}</div> : <div className="department-empty"><div className="empty-icon"><Building2 size={20} /></div><b>No departments yet</b><span>Create a department to organize your employees into teams.</span>{currentUser?.role === 'ROLE_LEAVE_APPROVER' && <button className="primary-button compact-button" onClick={() => setModal({ department: true })}><Plus size={15} /> Add department</button>}</div>}</section> : activeView === 'my-profile' ? selectedEmployee ? <EmployeeDetail employee={selectedEmployee} info={employeeInfo} leaves={leaveRequests} approver={currentUser?.role === 'ROLE_LEAVE_APPROVER'} myProfile onBack={() => setActiveView(currentUser?.role === 'ROLE_LEAVE_APPROVER' ? 'people' : 'leaves')} onSave={saveEmployeeInfo} onApprove={decideLeave} /> : <UnlinkedProfile username={currentUser?.username} role={currentUser?.role} /> : activeView === 'employee' ? <EmployeeDetail employee={selectedEmployee} info={employeeInfo} leaves={leaveRequests} approver={currentUser?.role === 'ROLE_LEAVE_APPROVER'} onBack={() => setActiveView('people')} onSave={saveEmployeeInfo} onApprove={decideLeave} /> : activeView === 'attendance' ? <AttendanceView rows={attendanceRows} setRows={setAttendanceRows} employees={employees} date={attendanceDate} setDate={setAttendanceDate} user={currentUser} notify={notify} /> : <LeavesView requests={leaveRequests} types={leaveTypes} approver={currentUser?.role === 'ROLE_LEAVE_APPROVER'} employeeLinked={Boolean(currentUser?.employeeId)} onSubmit={submitLeave} onDecide={decideLeave} />}
          <footer className="page-footer"><span>Made for people who make it happen <span className="footer-heart">♥</span></span><span>© 2024 PeopleOS <span className="footer-sep">·</span> Privacy <span className="footer-sep">·</span> Terms</span></footer>
        </div>
      </main>
      {modal?.department ? <DepartmentModal onClose={() => setModal(null)} onSave={saveDepartment} /> : modal && <EmployeeModal employee={modal.employee} departments={departments} users={employeeUsers} accountsIssue={accountsIssue} onClose={() => setModal(null)} onSave={saveEmployee} />}
      {notice && <div className="toast"><span><Check size={15} /></span>{notice}</div>}
    </div>
  )
}

function SummaryCard({ icon, label, value, foot, tone }) {
  return <div className="summary-card"><div className="summary-top"><span className={`summary-icon ${tone}`}>{icon}</span><span className="summary-trend"><ArrowUp size={13} /> 12%</span></div><div className="summary-value">{value}</div><div className="summary-label">{label}</div><div className="summary-foot"><span className="foot-dot" />{foot}</div></div>
}

function SortHead({ label, sortKey, sort, setSort }) {
  const selected = sort.key === sortKey
  return <th><button className={`sort-button ${selected ? 'sorted' : ''}`} onClick={() => setSort({ key: sortKey, direction: selected && sort.direction === 'asc' ? 'desc' : 'asc' })}>{label}{selected ? sort.direction === 'asc' ? <ArrowUp size={12} /> : <ArrowDown size={12} /> : <ArrowUpDown size={12} />}</button></th>
}

function UnlinkedProfile({ username, role }) {
  return <div className="employee-detail-page">
    <section className="employee-hero"><div className="person-avatar lavender employee-hero-avatar"><UserRound size={23} /></div><div className="employee-hero-info"><div className="eyebrow"><span className="eyebrow-dot" /> MY PROFILE</div><h1>{username || 'My account'}</h1><p>{role === 'ROLE_LEAVE_APPROVER' ? 'Leave approver account' : 'Employee account'}</p></div><span className="department-pill">Not linked</span></section>
    <section className="detail-panel unlinked-profile-panel"><div className="detail-panel-heading"><div><h2>No employee profile linked</h2><p>Your login details are available, but there is no employee record connected to this account yet.</p></div></div><div className="unlinked-notice">Ask a leave approver to link this login to an employee from the People page. Once linked, your employee details will appear here.</div></section>
  </div>
}

function AttendanceView({ rows, setRows, employees, date, setDate, user, notify }) {
  const approver = user?.role === 'ROLE_LEAVE_APPROVER'
  const [employeeId, setEmployeeId] = useState('')
  const [busy, setBusy] = useState(false)
  const ownId = user?.employeeId
  const selectedId = approver ? (employeeId || employees[0]?.id) : ownId
  const today = localDate()
  const todayRecord = rows.find((row) => row.date === today && (!selectedId || row.employeeId === Number(selectedId)))

  useEffect(() => {
    if (approver && !employeeId && employees.length) setEmployeeId(String(employees[0].id))
  }, [approver, employeeId, employees])

  async function mark(action) {
    if (!selectedId) return
    setBusy(true)
    try {
      const row = await api(`/attendance/${action}/${selectedId}`, { method: action === 'check-in' ? 'POST' : 'PUT' })
      if (!approver) setRows((current) => [row, ...current.filter((entry) => entry.id !== row.id)])
      else {
        const daily = await api(`/attendance/date/${date}`)
        setRows(Array.isArray(daily) ? daily : [])
      }
      notify(action === 'check-in' ? 'Check-in recorded' : 'Check-out recorded')
    } catch (error) { notify(error.message) } finally { setBusy(false) }
  }

  return <div className="leave-page attendance-page">
    <div className="leave-page-heading"><div><div className="eyebrow"><span className="eyebrow-dot" /> TIME & PRESENCE</div><h1>{approver ? 'Attendance' : 'My attendance'}</h1><p>{approver ? 'Review daily attendance and record a team member’s check-in or check-out.' : 'Record your workday and review your attendance history.'}</p></div><span className="leave-total"><Clock3 size={16} /> {rows.length} records</span></div>
    {!approver && !ownId && <div className="unlinked-notice">Your login is not linked to an employee profile yet, so attendance cannot be recorded.</div>}
    <section className="detail-panel attendance-action-panel"><div className="detail-panel-heading"><div><h2>{approver ? 'Daily attendance' : 'Today'}</h2><p>{approver ? 'Select a date and employee to review or record attendance.' : `Attendance for ${new Date().toLocaleDateString()}`}</p></div><Clock3 size={19} className="leave-heading-icon" /></div>
      <div className="attendance-controls">{approver && <label className="field-label">Employee<select value={selectedId || ''} onChange={(event) => setEmployeeId(event.target.value)}><option value="">Choose an employee</option>{employees.map((employee) => <option key={employee.id} value={employee.id}>{employee.firstName} {employee.lastName}</option>)}</select></label>}{approver && <label className="field-label">Date<input type="date" value={date} onChange={(event) => setDate(event.target.value)} /></label>}
        {!approver && <><div className="attendance-status"><span className={`leave-status status-${todayRecord?.status?.toLowerCase() || 'pending'}`}>{todayRecord?.status || 'NOT CHECKED IN'}</span><span>{todayRecord?.checkInTime ? `In ${todayRecord.checkInTime}` : 'No check-in recorded'}</span><span>{todayRecord?.checkOutTime ? `Out ${todayRecord.checkOutTime}` : ''}</span></div><button className="primary-button" disabled={busy || !ownId || Boolean(todayRecord)} onClick={() => mark('check-in')}>{busy ? 'Saving…' : 'Check in'}</button><button className="secondary-button" disabled={busy || !ownId || !todayRecord || Boolean(todayRecord.checkOutTime)} onClick={() => mark('check-out')}>{busy ? 'Saving…' : 'Check out'}</button></>}
        {approver && <><button className="primary-button" disabled={busy || !selectedId || date !== today || rows.some((row) => row.employeeId === Number(selectedId) && row.date === today)} onClick={() => mark('check-in')}>Check in</button><button className="secondary-button" disabled={busy || !selectedId || date !== today || !rows.some((row) => row.employeeId === Number(selectedId) && row.date === today && !row.checkOutTime)} onClick={() => mark('check-out')}>Check out</button></>}
      </div>
    </section>
    <section className="detail-panel attendance-history-panel"><div className="detail-panel-heading"><div><h2>{approver ? `Attendance on ${date}` : 'Attendance history'}</h2><p>{approver ? 'All records for the selected day.' : 'Your recent recorded workdays.'}</p></div></div>{rows.length ? <div className="table-wrap"><table className="attendance-table"><thead><tr>{approver && <th>EMPLOYEE</th>}<th>DATE</th><th>CHECK IN</th><th>CHECK OUT</th><th>STATUS</th></tr></thead><tbody>{rows.map((row) => <tr key={row.id}>{approver && <td>{row.employeeName}</td>}<td>{row.date}</td><td>{row.checkInTime || '—'}</td><td>{row.checkOutTime || '—'}</td><td><span className={`leave-status status-${row.status?.toLowerCase()}`}>{row.status?.replace('_', ' ')}</span></td></tr>)}</tbody></table></div> : <div className="leave-empty"><Clock3 size={20} /><b>No attendance records</b><span>{approver ? 'There are no attendance records for this date.' : 'Check in to start your attendance history.'}</span></div>}</section>
  </div>
}

function EmployeeDetail({ employee, info, leaves, approver, myProfile = false, onBack, onSave, onApprove }) {
  const [editing, setEditing] = useState(false)
  const [form, setForm] = useState({})
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  useEffect(() => { if (info) setForm(info) }, [info])
  if (!employee) return <section className="department-view"><p className="empty-state">Choose an employee to view their details.</p></section>
  async function save(e) {
    e.preventDefault(); setSaving(true); setError('')
    try { await onSave(form); setEditing(false) } catch (err) { setError(err.message) } finally { setSaving(false) }
  }
  return <div className="employee-detail-page">{!myProfile && <button className="back-link" onClick={onBack}><ArrowLeft size={15} /> Back to people</button>}<section className="employee-hero"><div className="person-avatar lavender employee-hero-avatar">{initials(employee.firstName, employee.lastName)}</div><div className="employee-hero-info"><div className="eyebrow"><span className="eyebrow-dot" /> {myProfile ? 'MY PROFILE' : 'EMPLOYEE PROFILE'}</div><h1>{employee.firstName} {employee.lastName}</h1><p>{info?.jobTitle || 'Team member'} <span>·</span> {employee.email}</p></div><div className="employee-hero-actions">{approver && <button className="secondary-button" onClick={() => setEditing(!editing)}><PencilLine size={14} /> {editing ? 'Cancel edit' : 'Edit profile'}</button>}<span className="department-pill"><span className="small-dept-dot" />{employee.departmentName || 'Employee'}</span></div></section>
    {error && <div className="error-banner"><span>{error}</span><button onClick={() => setError('')}><X size={16} /></button></div>}
    <div className="employee-detail-grid"><section className="detail-panel"><div className="detail-panel-heading"><div><h2>Employee information</h2><p>Personal and work details</p></div></div>{editing && approver ? <form className="info-form" onSubmit={save}><label className="field-label">Job title<input value={form.jobTitle || ''} onChange={(e) => setForm({ ...form, jobTitle: e.target.value })} /></label><label className="field-label">Phone<input value={form.phone || ''} onChange={(e) => setForm({ ...form, phone: e.target.value })} /></label><label className="field-label">Hire date<input type="date" value={form.hireDate || ''} onChange={(e) => setForm({ ...form, hireDate: e.target.value })} /></label><label className="field-label">Date of birth<input type="date" value={form.dateOfBirth || ''} onChange={(e) => setForm({ ...form, dateOfBirth: e.target.value })} /></label><label className="field-label full-field">Address<input value={form.address || ''} onChange={(e) => setForm({ ...form, address: e.target.value })} /></label><label className="field-label full-field">Emergency contact<input value={form.emergencyContact || ''} onChange={(e) => setForm({ ...form, emergencyContact: e.target.value })} /></label><div className="full-field form-save-row"><button disabled={saving} className="primary-button">{saving ? 'Saving…' : 'Save profile'}</button></div></form> : <div className="detail-fields"><DetailField label="Work email" value={employee.email} /><DetailField label="Job title" value={info?.jobTitle} /><DetailField label="Department" value={employee.departmentName} /><DetailField label="Salary" value={currency(employee.salary)} /><DetailField label="Phone" value={info?.phone} /><DetailField label="Hire date" value={info?.hireDate} /><DetailField label="Date of birth" value={info?.dateOfBirth} /><DetailField label="Address" value={info?.address} wide /><DetailField label="Emergency contact" value={info?.emergencyContact} wide /></div>}</section><section className="detail-panel detail-leaves"><div className="detail-panel-heading"><div><h2>Leave history</h2><p>Leave requests for this employee</p></div><span className="subtle-count">{leaves.length} requests</span></div><LeaveRequestList requests={leaves} approver={approver} onDecide={onApprove} /></section></div>
  </div>
}

function DetailField({ label, value, wide }) { return <div className={`detail-field ${wide ? 'wide-field' : ''}`}><span>{label}</span><b>{value || 'Not provided'}</b></div> }

function LeavesView({ requests, types, approver, employeeLinked, onSubmit, onDecide }) {
  const [form, setForm] = useState({ leaveTypeId: '', startDate: '', endDate: '', reason: '' })
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const today = new Date().toISOString().slice(0, 10)
  async function submit(e) {
    e.preventDefault()
    if (!form.leaveTypeId || !form.startDate || !form.endDate || !form.reason.trim()) { setError('Complete each field before submitting.'); return }
    if (form.endDate < form.startDate) { setError('End date must be on or after the start date.'); return }
    setBusy(true); setError('')
    try { await onSubmit({ ...form, leaveTypeId: Number(form.leaveTypeId) }); setForm({ leaveTypeId: '', startDate: '', endDate: '', reason: '' }) } catch (err) { setError(err.message) } finally { setBusy(false) }
  }
  return <div className="leave-page"><div className="leave-page-heading"><div><div className="eyebrow"><span className="eyebrow-dot" /> TIME AWAY</div><h1>{approver ? 'Leave approvals' : 'My leave'}</h1><p>{approver ? 'Review and decide on employee leave requests.' : 'Request time off and keep track of its status.'}</p></div><span className="leave-total"><CalendarDays size={16} /> {requests.length} {approver ? 'requests' : 'requests'}</span></div>{error && <div className="error-banner"><span>{error}</span><button onClick={() => setError('')}><X size={16} /></button></div>}{!approver && !employeeLinked && <div className="unlinked-notice">Your login is not linked to an employee profile yet. Ask the leave approver to link your account before requesting time off.</div>}{!approver && employeeLinked && <form className="detail-panel leave-request-form" onSubmit={submit}><div className="detail-panel-heading"><div><h2>Request time off</h2><p>Your approver will review your request.</p></div><ClipboardCheck size={19} className="leave-heading-icon" /></div><div className="leave-form-grid"><label className="field-label">Leave type<select value={form.leaveTypeId} onChange={(e) => setForm({ ...form, leaveTypeId: e.target.value })}><option value="">Choose a type</option>{types.map((type) => <option value={type.id} key={type.id}>{type.name}</option>)}</select></label><label className="field-label">Start date<input required type="date" min={today} value={form.startDate} onChange={(e) => setForm({ ...form, startDate: e.target.value })} /></label><label className="field-label">End date<input required type="date" min={form.startDate || today} value={form.endDate} onChange={(e) => setForm({ ...form, endDate: e.target.value })} /></label><label className="field-label leave-reason">Reason<textarea required maxLength="1000" value={form.reason} onChange={(e) => setForm({ ...form, reason: e.target.value })} placeholder="Add a short note for your approver" /></label></div><div className="leave-submit-row"><span>Requests are submitted as pending.</span><button disabled={busy} className="primary-button">{busy ? 'Submitting…' : 'Submit request'}</button></div></form>}<section className="detail-panel leave-list-panel"><div className="detail-panel-heading"><div><h2>{approver ? 'All requests' : 'Request history'}</h2><p>{requests.filter((request) => request.status === 'PENDING').length} pending review</p></div></div><LeaveRequestList requests={requests} approver={approver} onDecide={onDecide} /></section></div>
}

function LeaveRequestList({ requests, approver, onDecide }) {
  const [deciding, setDeciding] = useState(null)
  return requests.length ? <div className="leave-request-list">{requests.map((request) => <article className="leave-request-card" key={request.id}><div className="leave-request-icon"><CalendarDays size={17} /></div><div className="leave-request-main"><div className="leave-request-title"><b>{request.leaveTypeName}</b><span className={`leave-status status-${request.status?.toLowerCase()}`}>{request.status}</span></div><div className="leave-dates">{request.employeeName && <span>{request.employeeName}</span>}<span>{request.startDate} – {request.endDate}</span></div><p>{request.reason}</p>{request.decisionNote && <div className="decision-note">Approver note: {request.decisionNote}</div>}</div>{approver && request.status === 'PENDING' && <div className="leave-decision-actions"><button disabled={deciding === request.id} className="approve-button" onClick={async () => { setDeciding(request.id); try { await onDecide(request.id, 'APPROVED') } finally { setDeciding(null) } }}>Approve</button><button disabled={deciding === request.id} className="reject-button" onClick={async () => { setDeciding(request.id); try { await onDecide(request.id, 'REJECTED', window.prompt('Optional note for the employee:') || '') } finally { setDeciding(null) } }}>Reject</button></div>}</article>)}</div> : <div className="leave-empty"><Clock3 size={20} /><b>No leave requests yet</b><span>{approver ? 'New requests will appear here.' : 'Your requests will appear here after you submit them.'}</span></div>
}

function Login({ onLogin, theme, setTheme }) {
  const [register, setRegister] = useState(false)
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  async function submit(e) {
    e.preventDefault(); setBusy(true); setError('')
    try {
      if (register) await api('/auth/register', { method: 'POST', body: JSON.stringify({ username, password }) })
      const result = await api('/auth/login', { method: 'POST', body: JSON.stringify({ username, password }) })
      onLogin(result.token)
    } catch (err) { setError(err.message) } finally { setBusy(false) }
  }
  return <div className="login-screen"><div className="login-decoration decoration-one" /><div className="login-decoration decoration-two" /><div className="login-panel"><div className="login-brand"><div className="brand-mark"><Command size={19} strokeWidth={2.6} /></div><span>people<span className="brand-light">OS</span></span></div><div className="login-art"><div className="art-orbit orbit-one" /><div className="art-orbit orbit-two" /><div className="art-card art-card-a"><span className="art-avatar art-lavender">AM</span><span><b>Alex Morgan</b><small>Product design</small></span><span className="art-pill" /></div><div className="art-card art-card-b"><span className="art-avatar art-mint">JL</span><span><b>Jamie Lee</b><small>Engineering</small></span><span className="art-pill" /></div><div className="art-center"><Users size={29} /></div><div className="art-spark spark-a">✳</div><div className="art-spark spark-b">✦</div></div><div className="login-copy"><div className="eyebrow"><span className="eyebrow-dot" /> A BETTER WAY TO WORK</div><h1>Good work<br />starts with <span>good people.</span></h1><p>Your team, thoughtfully organized.<br />Everything you need to help people thrive.</p><div className="login-features"><span><Check size={14} /> One clear view of your team</span><span><Check size={14} /> A little less busywork</span></div></div><div className="login-legal">© 2024 PeopleOS <span>·</span> Built for better work</div></div><div className="login-form-side"><ThemeToggle theme={theme} setTheme={setTheme} className="login-theme-toggle" /><form className="login-form" onSubmit={submit}><div className="form-overline">WELCOME TO PEOPLEOS</div><h2>{register ? 'Create your account' : 'Welcome back'}</h2><p className="login-subtitle">{register ? 'Get started with your team workspace.' : 'Sign in to pick up where you left off.'}</p>{error && <div className="form-error">{error}</div>}<label className="field-label">Username<input required autoComplete="username" value={username} onChange={(e) => setUsername(e.target.value)} placeholder="Enter your username" /></label><label className="field-label">Password<input required type="password" autoComplete={register ? 'new-password' : 'current-password'} value={password} onChange={(e) => setPassword(e.target.value)} placeholder="Enter your password" /></label>{!register && <div className="forgot-link">Protected with secure sign-in <ShieldCheck size={14} /></div>}<button disabled={busy} className="primary-button login-submit">{busy ? 'Please wait…' : register ? 'Create account' : 'Sign in'}<ChevronRight size={17} /></button><div className="register-prompt">{register ? 'Already have an account?' : 'New to PeopleOS?'} <button type="button" onClick={() => { setRegister(!register); setError('') }}>{register ? 'Sign in' : 'Create an account'}</button></div><div className="login-security"><span><ShieldCheck size={15} /></span> Your information is protected and secure</div></form><div className="form-corner">PEOPLE FIRST, ALWAYS <span>✳</span></div></div></div>
}

function ThemeToggle({ theme, setTheme, className = '' }) {
  const dark = theme === 'dark'
  return <button type="button" className={`theme-toggle ${className}`} onClick={() => setTheme(dark ? 'light' : 'dark')} aria-label={`Switch to ${dark ? 'light' : 'dark'} mode`} title={`Switch to ${dark ? 'light' : 'dark'} mode`}>{dark ? <Sun size={16} /> : <Moon size={16} />}<span>{dark ? 'Light' : 'Dark'}</span></button>
}

function DepartmentModal({ onClose, onSave }) {
  const [name, setName] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  async function submit(e) {
    e.preventDefault()
    if (!name.trim()) { setError('Enter a department name.'); return }
    setBusy(true); setError('')
    try { await onSave(name) } catch (err) { setError(err.message) } finally { setBusy(false) }
  }
  return <div className="modal-backdrop" onMouseDown={(e) => { if (e.target === e.currentTarget) onClose() }}><form className="employee-modal department-modal" onSubmit={submit}><div className="modal-top"><div className="modal-icon"><Building2 size={19} /></div><button type="button" className="icon-button" onClick={onClose}><X size={18} /></button></div><h2>Add a department</h2><p className="modal-intro">Create a team to group employees together.</p>{error && <div className="form-error">{error}</div>}<label className="field-label">Department name<input autoFocus maxLength="100" value={name} onChange={(e) => setName(e.target.value)} placeholder="e.g. Engineering" /></label><div className="modal-actions"><button type="button" className="secondary-button" onClick={onClose}>Cancel</button><button disabled={busy} className="primary-button">{busy ? 'Creating…' : <><Plus size={16} /> Create department</>}</button></div></form></div>
}

function EmployeeModal({ employee, departments, users = [], accountsIssue, onClose, onSave }) {
  const selectableUsers = users.filter((user) => !user.employeeId || user.employeeId === employee?.id)
  const [form, setForm] = useState({ firstName: employee?.firstName || '', lastName: employee?.lastName || '', email: employee?.email || '', salary: employee?.salary ?? '', departmentId: employee?.departmentId ?? '', userId: employee?.userId ?? '' })
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  async function submit(e) {
    e.preventDefault()
    if (form.salary && Number(form.salary) <= 0) { setError('Salary must be greater than zero.'); return }
    if (!form.firstName.trim() || !form.lastName.trim() || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) { setError('Enter a first name, last name, and valid email address.'); return }
    setBusy(true); setError('')
    await onSave(form).catch((err) => setError(err.message))
    setBusy(false)
  }
  return <div className="modal-backdrop" onMouseDown={(e) => { if (e.target === e.currentTarget) onClose() }}><form className="employee-modal" onSubmit={submit}><div className="modal-top"><div className="modal-icon"><Users size={19} /></div><button type="button" className="icon-button" onClick={onClose}><X size={18} /></button></div><h2>{employee ? 'Edit employee' : 'Add a team member'}</h2><p className="modal-intro">{employee ? 'Update this person’s information.' : 'Add someone new to your organization.'}</p>{error && <div className="form-error">{error}</div>}<div className="form-grid"><label className="field-label">First name<input autoFocus value={form.firstName} onChange={(e) => setForm({ ...form, firstName: e.target.value })} placeholder="e.g. Alex" /></label><label className="field-label">Last name<input value={form.lastName} onChange={(e) => setForm({ ...form, lastName: e.target.value })} placeholder="e.g. Morgan" /></label><label className="field-label full-field">Work email<input type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} placeholder="alex@company.com" /></label><label className="field-label">Department<select value={form.departmentId} onChange={(e) => setForm({ ...form, departmentId: e.target.value })}><option value="">Unassigned</option>{departments.map((dept) => <option value={dept.id} key={dept.id}>{dept.name}</option>)}</select></label><label className="field-label">Annual salary<input type="number" min="1" value={form.salary} onChange={(e) => setForm({ ...form, salary: e.target.value })} placeholder="e.g. 72000" /></label><label className="field-label full-field">Employee login<select disabled={selectableUsers.length === 0} value={form.userId} onChange={(e) => setForm({ ...form, userId: e.target.value })}><option value="">{selectableUsers.length ? "Don't link a login yet" : "No eligible login accounts"}</option>{selectableUsers.map((user) => <option value={user.id} key={user.id}>{user.username} ({user.role === 'ROLE_LEAVE_APPROVER' ? 'Leave approver' : 'Employee'})</option>)}</select><small className="field-help">{accountsIssue || (selectableUsers.length ? "Choose the login for this employee. Both employee and leave approver accounts can have an employee profile." : users.length ? "All login accounts are already linked to employees. Edit their existing employee record to change its login." : "No login accounts are available. Register an account first, then reopen this form.")}</small></label></div><div className="modal-actions"><button type="button" className="secondary-button" onClick={onClose}>Cancel</button><button disabled={busy} className="primary-button">{busy ? 'Saving…' : employee ? 'Save changes' : <><Plus size={16} /> Add employee</>}</button></div></form></div>
}

export default App
