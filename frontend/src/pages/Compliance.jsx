/**
 * Compliance page — redirects to ReturnDraft since compliance briefs
 * are generated and managed there via the /api/v1/returns endpoints.
 */
import React, { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

const Compliance = () => {
  const navigate = useNavigate();
  useEffect(() => { navigate('/returns', { replace: true }); }, [navigate]);
  return null;
};

export default Compliance;
