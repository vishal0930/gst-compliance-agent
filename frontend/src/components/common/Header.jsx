import React from "react";
import { useLocation, useNavigate } from "react-router-dom";
import {
  Menu,
  Bell,
  Search,
  ChevronDown,
  LogOut,
} from "lucide-react";
import useAuthStore from "../../store/authStore";
import { getCurrentPeriod } from "../../utils/formatters";

const Header = ({ collapsed, setCollapsed }) => {
  const location = useLocation();
  const navigate = useNavigate();
  const { user, logout } = useAuthStore();
  const period = getCurrentPeriod();

  const pageTitles = {
    "/dashboard": "Dashboard",
    "/invoices": "Invoices",
    "/gstr2b": "GSTR-2B",
    "/reconciliation": "Reconciliation",
    "/returns": "Return Draft",
    "/deadlines": "Deadlines",
    "/analytics": "Analytics",
    "/insights": "AI Insights",
    "/settings": "Settings",
    "/profile": "Profile",
  };

  const title = pageTitles[location.pathname] || "GST Compliance";

  const handleLogout = async () => {
    await logout();
    navigate("/login");
  };

  return (
    <header className="h-16 bg-slate-950 border-b border-slate-800 flex items-center justify-between px-6">
      <div className="flex items-center gap-5">
        <button
          onClick={() => setCollapsed(!collapsed)}
          className="p-2 rounded-lg hover:bg-slate-800 transition"
        >
          <Menu className="text-slate-300" size={20} />
        </button>

        <div>
          <h1 className="text-white font-semibold text-lg">{title}</h1>
          <p className="text-xs text-slate-400">
            GST Period • {period.month} {period.year}
          </p>
        </div>
      </div>

      <div className="hidden lg:flex items-center w-96">
        <div className="relative w-full">
          <Search
            size={16}
            className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-500"
          />
          <input
            type="text"
            placeholder="Search invoices, GSTIN, vendors..."
            className="w-full bg-slate-900 border border-slate-700 rounded-xl pl-10 pr-4 py-2 text-sm text-white placeholder:text-slate-500 focus:outline-none focus:border-amber-500"
          />
        </div>
      </div>

      <div className="flex items-center gap-4">
        <button className="relative p-2 rounded-lg hover:bg-slate-800 transition">
          <Bell size={20} className="text-slate-300" />
          <span className="absolute top-1 right-1 h-2 w-2 rounded-full bg-amber-400"></span>
        </button>

        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-full bg-amber-500 flex items-center justify-center text-slate-950 font-bold">
            {user?.name?.charAt(0).toUpperCase() || 'U'}
          </div>

          <div className="hidden md:block">
            <p className="text-sm text-white font-medium">{user?.name || 'User'}</p>
            <p className="text-xs text-slate-400">{user?.email || 'user@example.com'}</p>
          </div>

          <ChevronDown size={18} className="text-slate-500" />
        </div>

        <button
          onClick={handleLogout}
          className="p-2 rounded-lg hover:bg-red-500/10 hover:text-red-400 transition"
        >
          <LogOut size={18} />
        </button>
      </div>
    </header>
  );
};

export default Header;