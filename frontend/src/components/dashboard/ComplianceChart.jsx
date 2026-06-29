import React from 'react';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
  LineChart,
  Line,
} from 'recharts';
import { Card } from 'antd';

const COLORS = ['#1677ff', '#52c41a', '#ff4d4f', '#faad14', '#722ed1'];

export const BarChartComponent = ({ data, title, xKey, yKey, colors = COLORS }) => {
  return (
    <Card title={title} className="h-80">
      <ResponsiveContainer width="100%" height="100%">
        <BarChart data={data}>
          <CartesianGrid strokeDasharray="3 3" />
          <XAxis dataKey={xKey} />
          <YAxis />
          <Tooltip />
          <Legend />
          <Bar dataKey={yKey} fill={colors[0]} radius={[4, 4, 0, 0]} />
        </BarChart>
      </ResponsiveContainer>
    </Card>
  );
};

export const PieChartComponent = ({ data, title, dataKey, nameKey }) => {
  return (
    <Card title={title} className="h-80">
      <ResponsiveContainer width="100%" height="100%">
        <PieChart>
          <Pie
            data={data}
            cx="50%"
            cy="50%"
            labelLine={false}
            label={({ name, percent }) => `${name}: ${(percent * 100).toFixed(0)}%`}
            outerRadius={80}
            fill="#8884d8"
            dataKey={dataKey}
            nameKey={nameKey}
          >
            {data.map((_, index) => (
              <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
            ))}
          </Pie>
          <Tooltip />
          <Legend />
        </PieChart>
      </ResponsiveContainer>
    </Card>
  );
};

export const LineChartComponent = ({ data, title, xKey, lines }) => {
  return (
    <Card title={title} className="h-80">
      <ResponsiveContainer width="100%" height="100%">
        <LineChart data={data}>
          <CartesianGrid strokeDasharray="3 3" />
          <XAxis dataKey={xKey} />
          <YAxis />
          <Tooltip />
          <Legend />
          {lines.map((line, index) => (
            <Line
              key={line.key}
              type="monotone"
              dataKey={line.key}
              stroke={COLORS[index % COLORS.length]}
              strokeWidth={2}
              dot={{ r: 4 }}
              activeDot={{ r: 6 }}
            />
          ))}
        </LineChart>
      </ResponsiveContainer>
    </Card>
  );
};

export const ComplianceChart = ({ data, type = 'bar', title }) => {
  const chartProps = {
    data: data || [],
    title: title || 'Compliance Overview',
  };

  switch (type) {
    case 'pie':
      return <PieChartComponent {...chartProps} dataKey="value" nameKey="name" />;
    case 'line':
      return <LineChartComponent {...chartProps} xKey="date" lines={[{ key: 'value', name: 'Value' }]} />;
    case 'bar':
    default:
      return <BarChartComponent {...chartProps} xKey="name" yKey="value" />;
  }
};

export default ComplianceChart;