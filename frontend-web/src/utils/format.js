export function formatDateTime(value) {
  if (!value) {
    return "暂无时间";
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat("zh-CN", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).format(date);
}

export function formatDate(value) {
  if (!value) {
    return "未标注日期";
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat("zh-CN", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).format(date);
}

export function mealTypeLabel(value) {
  const labelMap = {
    BREAKFAST: "早餐",
    LUNCH: "午餐",
    DINNER: "晚餐",
    SNACK: "加餐",
  };

  return labelMap[value] || value || "未标注";
}

export function riskTone(level) {
  if (level === "GREEN") {
    return "green";
  }
  if (level === "YELLOW") {
    return "yellow";
  }
  if (level === "RED") {
    return "red";
  }
  return "neutral";
}

export function riskLabel(level) {
  const labelMap = {
    GREEN: "低风险",
    YELLOW: "中风险",
    RED: "高风险",
  };

  return labelMap[level] || "未知";
}
