
import { useNavigate } from 'react-router-dom';
import { Button, Result } from 'antd';

const NotFound = () => {
  const navigate = useNavigate();

  return (
    <div style={{ 
      display: 'flex', 
      justifyContent: 'center', 
      alignItems: 'center', 
      height: '80vh', 
      background: 'transparent' // Inherits parent layout's Slate 900 base canvas
    }}>
      <Result
        status="404"
        title={<span style={{ color: '#FACC15', fontSize: '36px', fontWeight: 700 }}>404</span>}
        subTitle={
          <span style={{ color: '#94A3B8', fontSize: '16px' }}>
            The compliance module or route you are searching for does not exist or has moved.
          </span>
        }
        extra={
          <Button 
            type="primary" 
            onClick={() => navigate('/dashboard')}
            style={{ fontWeight: 600 }}
          >
            Return to Dashboard
          </Button>
        }
      />
    </div>
  );
};

export default NotFound;