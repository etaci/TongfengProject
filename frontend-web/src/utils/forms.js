export function emptyToNull(value) {
  if (value == null) {
    return null;
  }

  const normalized = String(value).trim();
  return normalized ? normalized : null;
}

export function toIsoString(value) {
  if (!value) {
    return null;
  }
  return new Date(value).toISOString();
}

export function splitCsv(value) {
  return String(value || "")
    .split(",")
    .map((item) => item.trim())
    .filter(Boolean);
}

export function parseMedicationText(value) {
  return String(value || "")
    .split("\n")
    .map((line) => line.trim())
    .filter(Boolean)
    .map((line) => {
      const [name, dosage, frequency, remark, remainingDays, refillThresholdDays] = line.split("|").map((item) => item.trim());

      if (!name || !dosage || !frequency) {
        throw new Error(`用药格式不完整：${line}`);
      }

      return {
        name,
        dosage,
        frequency,
        remark: remark || null,
        remainingDays: remainingDays ? Number(remainingDays) : null,
        refillThresholdDays: refillThresholdDays ? Number(refillThresholdDays) : null,
      };
    });
}

export function mapProfileToForm(profile, fallbackName = "") {
  return {
    name: profile?.name || fallbackName,
    gender: profile?.gender || "MALE",
    birthday: profile?.birthday || "",
    heightCm: profile?.heightCm ?? "",
    targetUricAcid: profile?.targetUricAcid ?? "",
    allergies: (profile?.allergies || []).join(", "),
    comorbidities: (profile?.comorbidities || []).join(", "),
    emergencyContact: profile?.emergencyContact || "",
  };
}

export function mapMedicationToText(plan) {
  return {
    lines: (plan?.currentMedications || [])
      .map((item) => {
        const parts = [item.name, item.dosage, item.frequency, item.remark || ""];
        if (item.remainingDays != null || item.refillThresholdDays != null) {
          parts.push(item.remainingDays ?? "", item.refillThresholdDays ?? "");
        }
        return parts.join(" | ");
      })
      .join("\n"),
    followUpNote: plan?.followUpNote || "",
  };
}

export function getMedicationPeriods(frequency) {
  const normalized = String(frequency || "").trim().toLowerCase();

  if (["twice-daily", "bid", "q12h", "每日两次", "一天两次"].includes(normalized)) {
    return ["MORNING", "EVENING"];
  }

  if (["three-times-daily", "tid", "q8h", "每日三次", "一天三次"].includes(normalized)) {
    return ["MORNING", "NOON", "EVENING"];
  }

  if (["bedtime", "睡前", "每晚睡前"].includes(normalized)) {
    return ["BEDTIME"];
  }

  return ["MORNING"];
}

export function getMedicationPeriodLabel(period) {
  switch (period) {
    case "MORNING":
      return "早晨";
    case "NOON":
      return "中午";
    case "EVENING":
      return "晚上";
    case "BEDTIME":
      return "睡前";
    default:
      return period || "未设置";
  }
}
