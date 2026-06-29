import axios from "axios";
// Import your central store directly
import useAuthStore from "../store/authStore"; 

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "/api/v1";

const client = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000, 
  headers: {
    "Content-Type": "application/json",
  },
});

/**
 * Request Interceptor
 * Pulls directly from the store state to avoid local storage fragmentation.
 */
client.interceptors.request.use(
  (config) => {
    const token = useAuthStore.getState().token;
    const localStorageToken = localStorage.getItem('accessToken');

    console.log("=== Request Interceptor Debug ===");
    console.log("URL:", config.url);
    console.log("Store token:", token);
    console.log("LocalStorage token:", localStorageToken);
    console.log("Headers before:", config.headers);

    // Use store token first, fall back to localStorage
    const effectiveToken = token || localStorageToken;

    if (effectiveToken && config.headers) {
      config.headers.Authorization = `Bearer ${effectiveToken}`;
      console.log("Headers after:", config.headers);
    } else {
      console.log("No token available - Authorization header not set");
    }

    return config;
  },
  (error) => Promise.reject(normalizeError(error))
);

/**
 * Response Interceptor
 * Safe, defensive unwrapping that gracefully accommodates mixed backend payload signatures.
 */
client.interceptors.response.use(
  (response) => {
    // 1. Let raw binary payloads pass through untouched
    if (response.config.responseType === "blob" || response.config.responseType === "arraybuffer") {
      return response;
    }

    // 2. Defensive Unwrapping: Only pluck .data if it's explicitly an enveloped response layout
    if (
      response.data &&
      typeof response.data === "object" &&
      "success" in response.data
    ) {
      return response.data.data !== undefined ? response.data.data : response.data;
    }

    // 3. Fallback for non-enveloped objects, auth payloads, or direct arrays
    return response.data;
  },
  (error) => {
    if (error.response?.status === 401) {
      // Execute global logout action to flush store state and handle routing cleanly
      useAuthStore.getState().logout?.();
    }
    return Promise.reject(normalizeError(error));
  }
);

/**
 * Global Error Normalizer
 */
function normalizeError(error) {
  const standardError = {
    status: error.response?.status || 500,
    message: "An unexpected error occurred. Please try again.",
    errors: null,
    code: error.code || "UNKNOWN_ERROR",
  };

  if (error.response?.data) {
    const apiError = error.response.data;
    standardError.message = apiError.message || standardError.message;
    standardError.errors = apiError.errors || null; 
  } else if (error.request) {
    standardError.message = "Network connectivity timeout. Please check your connection.";
    standardError.code = "NETWORK_TIMEOUT";
  }

  return standardError;
}

/**
 * Multipart File Upload Helper
 */
export const uploadMultipart = (url, formData, onProgress) => {
  return client.post(url, formData, {
    headers: {
      "Content-Type": "multipart/form-data",
    },
    onUploadProgress: (progressEvent) => {
      if (onProgress && progressEvent.total) {
        const percentCompleted = Math.round((progressEvent.loaded * 100) / progressEvent.total);
        onProgress(percentCompleted);
      }
    },
  });
};

/**
 * File Download Utility
 */
export const downloadFile = async (url, params = {}, customFilename = "export.xlsx") => {
  const response = await client.get(url, { params, responseType: "blob" });
  const disposition = response.headers["content-disposition"];
  let filename = customFilename;

  if (disposition && disposition.includes("filename=")) {
    filename = disposition.split("filename=")[1].replace(/["']/g, "");
  }

  const blob = new Blob([response.data], { type: response.headers["content-type"] });
  const downloadUrl = window.URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = downloadUrl;
  link.setAttribute("download", filename);
  document.body.appendChild(link);
  link.click();
  
  link.parentNode.removeChild(link);
  window.URL.revokeObjectURL(downloadUrl);
};

export default client;