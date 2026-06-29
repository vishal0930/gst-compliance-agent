import React from "react";
import { NavLink } from "react-router-dom";
import {
  LayoutDashboard,
  FileText,
  FileCheck,
  RefreshCw,
  ClipboardList,
  CalendarClock,
  BarChart3,
  BrainCircuit,
  User,
  Settings,
  ShieldCheck,
} from "lucide-react";
import useAuthStore from "../../store/authStore";

const Sidebar = ({ collapsed, setCollapsed }) => {
  const { user } = useAuthStore();

  const sections = [
    {
      title: "WORKSPACE",
      items: [
        { to: "/dashboard", icon: LayoutDashboard, label: "Dashboard" },
        { to: "/invoices", icon: FileText, label: "Invoices" },
        { to: "/gstr2b", icon: FileCheck, label: "GSTR-2B" },
        { to: "/reconciliation", icon: RefreshCw, label: "Reconciliation" },
      ],
    },
    {
      title: "OPERATIONS",
      items: [
        { to: "/returns", icon: ClipboardList, label: "Return Draft" },
        { to: "/deadlines", icon: CalendarClock, label: "Deadlines" },
        { to: "/analytics", icon: BarChart3, label: "Analytics" },
        { to: "/insights", icon: BrainCircuit, label: "AI Insights" },
      ],
    },
    {
      title: "SYSTEM",
      items: [
        { to: "/profile", icon: User, label: "Profile" },
        { to: "/settings", icon: Settings, label: "Settings" },
      ],
    },
  ];

  return (
    <aside
      className={`${
        collapsed ? "w-20" : "w-64"
      } bg-slate-950 border-r border-slate-800 transition-all duration-300 flex flex-col h-screen`}
    >
      {/* Logo */}
      <div className="h-16 border-b border-slate-800 flex items-center px-5">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-amber-500 flex items-center justify-center text-slate-950 font-bold">
            <ShieldCheck size={20} />
          </div>

          {!collapsed && (
            <div>
              <h1 className="text-white font-bold text-base">
                GST Compliance
              </h1>
              <p className="text-slate-400 text-xs">
                AI Platform
              </p>
            </div>
          )}
        </div>
      </div>

      {/* Navigation */}
      <div className="flex-1 overflow-y-auto py-5">
        {sections.map((section) => (
          <div key={section.title} className="mb-6">
            {!collapsed && (
              <p className="px-5 mb-2 text-[11px] tracking-widest font-semibold text-slate-500 uppercase">
                {section.title}
              </p>
            )}

            <div className="space-y-1 px-3">
              {section.items.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  className={({ isActive }) =>
                    `
                    flex items-center
                    ${
                      collapsed ? "justify-center px-0" : "px-3"
                    }
                    py-3 rounded-xl transition-all duration-200
                    ${
                      isActive
                        ? "bg-amber-500/15 border-l-4 border-amber-400 text-amber-300"
                        : "text-slate-400 hover:bg-slate-800 hover:text-white"
                    }
                  `
                  }
                >
                  <item.icon
                    size={20}
                    className={collapsed ? "" : "mr-3"}
                  />

                  {!collapsed && (
                    <span className="text-sm font-medium">
                      {item.label}
                    </span>
                  )}
                </NavLink>
              ))}
            </div>
          </div>
        ))}
      </div>

      {/* User Card */}
      <div className="border-t border-slate-800 p-4">
        <div
          className={`flex items-center ${
            collapsed ? "justify-center" : "gap-3"
          }`}
        >
          <div className="w-10 h-10 rounded-full bg-amber-500 text-slate-900 flex items-center justify-center font-bold">
            {user?.name?.charAt(0).toUpperCase() || 'U'}
          </div>

          {!collapsed && (
            <div>
              <p className="text-sm text-white font-semibold">
                {user?.name || 'User'}
              </p>
              <p className="text-xs text-slate-400">
                {user?.email || 'user@example.com'}
              </p>
            </div>
          )}
        </div>
      </div>
    </aside>
  );
};

export default Sidebar;